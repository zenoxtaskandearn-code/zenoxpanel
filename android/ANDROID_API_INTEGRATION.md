# ANDROID_API_INTEGRATION.md

## Backend inspected (actual contracts)
Source: `../backend` Spring Boot 3.3.5 (Java 21), JWT auth, JPA/Flyway MySQL, STOMP WebSocket.

### Device auth & session
- `POST /api/device/register`
  - Headers: optional `X-Owner-Email` (backend may infer single admin in some cases; client sends when provided)
  - Body: `{ "deviceUuid": "...", "name": "...", "deviceType": "ANDROID", "appVersion": "1.0.0", "deviceSecret": "..." }`
  - Response: `{ accessToken, refreshToken, tokenType, expiresInSeconds, device{...} }`
- `POST /api/device/auth` (also `/api/device/login`)
  - Body: `{ "deviceUuid": "...", "deviceSecret": "..." }`
  - Response: same auth payload with device view
- `POST /api/device/heartbeat` (Bearer device token)
  - Body: `{ "appVersion": "..." }`
- `POST /api/device/status` (Bearer device token)
  - Body: `{ "status": "ONLINE|OFFLINE|AWAY" }`

### Device media metadata sync
- `POST /api/device/media/sync` (Bearer device token)
  - Body: `{ "items": [ { "externalId":"IMAGE:123", "mediaType":"IMAGE|VIDEO", "fileName":..., "mimeType":..., "sizeBytes":..., "width":..., "height":..., "durationMs":..., "capturedAt":"ISO-8601", "thumbnailAvailable":true, "streamAvailable":true, "thumbnailRef":"...", "streamRef":"...", "checksum":..., "deletedOnDevice":false } ] }`
  - Notes: client uses stable external IDs derived from MediaStore IDs/types and syncs incremental dirty/deleted changes; no raw media bytes uploaded.

### Remote operations (device side)
- `GET /api/device/operations/pending` → `[OperationView]`
- `POST /api/device/operations/{id}/ack` body `{ "message": "...", "status": "..."? }`
- `POST /api/device/operations/{id}/status` body `{ "status":"ACKNOWLEDGED|IN_PROGRESS|COMPLETED|FAILED", "message":"...", "failureReason":"..." }`
- Operation types observed: `PREVIEW`, `EDIT`, `DELETE` (admin-created; backend enforces consent/permissions/expiry).

### WebSocket (realtime)
- Endpoint: `/ws` (STOMP), SockJS also available server-side; client uses raw WebSocket + STOMP frames.
- Auth: JWT in STOMP `CONNECT` header `Authorization: Bearer <token>` (client also supports token-based handshake conventions where applicable).
- Topics consumed: `/topic/operations`, `/topic/gallery`, `/topic/devices/{deviceDbId}`, `/topic/devices/{deviceDbId}/operations`, `/topic/devices/{deviceDbId}/gallery`.
- Behavior: on MESSAGE events client refreshes pending operations and processes consent-gated work.

## Client adaptation decisions & contract gaps
1. **No pairing/registration-code issuance endpoint found** in backend device/admin APIs.
   - App UX includes registration/pairing code field (per requirements) but performs registration via available `POST /api/device/register` using owner email/header and generated device credentials. If deployment requires pre-issued codes, backend extension is needed; app documents/handles gracefully.
2. **No binary media relay/upload endpoint found** (admin media thumbnail/stream endpoints return contract responses, not proxied device bytes).
   - `PREVIEW` operations: app validates consent/accessibility, prepares on-device preview info (type/dimensions/duration where available), and reports `COMPLETED` with explicit message that backend media relay is unavailable in current API. No permanent cloud copy is created.
3. **Device token refresh flow not exposed as device endpoint** (admin refresh exists; device auth returns tokens).
   - App stores tokens/secret securely and can re-authenticate when needed; interceptor does not auto-rotate via missing device-refresh API.
4. **Remote operation payload schema not strictly defined** in backend contract.
   - App parses optional JSON payload hints (e.g. `contentUri`, `mediaStoreId`, `mediaType`, `action`, `newName`) and falls back to local index resolution/safe failure reporting.
5. **Consent source of truth**: backend tracks admin-device consent; app enforces local explicit opt-in and also reports status/blocks processing when local consent/remote-access is disabled.

## Security behaviors
- Bearer auth for protected device APIs and WebSocket CONNECT.
- Keystore-backed encrypted storage for tokens/device secret; DataStore for non-secret settings.
- Auth header redacted in HTTP logs; no sensitive media/token logging.
- Operations executed only when registered + consented + remote access enabled + permission-accessible.
