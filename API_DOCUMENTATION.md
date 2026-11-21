# API Documentation

Base URL (local): `http://localhost:8080`

All admin-protected endpoints require `Authorization: Bearer <accessToken>`.
Device-protected endpoints require a **device** JWT (`ROLE_DEVICE`).
WebSocket requires authenticated STOMP CONNECT with token.

Error responses follow a standard shape:
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/api/...",
  "details": { }
}
```

## 1. Admin authentication (`/api/auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public (controlled) | Create admin account. Body: `{ email, name, password, inviteCode? }`. Registration may be disabled or require invite code via server config. Returns tokens + profile. |
| POST | `/api/auth/login` | Public | Login. Body: `{ email, password }`. Returns `{ accessToken, refreshToken, tokenType, expiresInSeconds, admin }`. |
| POST | `/api/auth/refresh` | Public | Rotate tokens. Body: `{ refreshToken }`. |
| POST | `/api/auth/logout` | Admin JWT | Revoke provided refresh token (if any). Body: `{ refreshToken? }`. |
| GET | `/api/auth/me` | Admin JWT | Current admin profile. |
| PUT | `/api/auth/profile` | Admin JWT | Update profile. Body: `{ name }`. |
| POST | `/api/auth/change-password` | Admin JWT | Change password. Body: `{ currentPassword, newPassword }`. |

**Roles:** Admin tokens include `ROLE_ADMIN` (RBAC enforced).

## 2. Device APIs (device agent)

### 2.1 Device registration/auth (public endpoints)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/device/register` | Public | Register or re-register device. Body: `{ deviceUuid, name, deviceType?, appVersion?, deviceSecret }`. Owner resolution: `X-Owner-Email` header, or auto-assigned if exactly one admin exists. Returns device tokens + device view. |
| POST | `/api/device/auth` (alias `/api/device/login`) | Public | Authenticate device. Body: `{ deviceUuid, deviceSecret }`. Returns device access/refresh tokens. Revoked devices rejected. |

### 2.2 Device-authenticated operations (`ROLE_DEVICE`)
| Method | Path | Description |
|---|---|---|
| POST | `/api/device/heartbeat` | Update last-seen/online status. Body: `{ appVersion? }`. |
| POST | `/api/device/status` | Report status. Body: `{ status }` where status in `ONLINE|OFFLINE|AWAY`. |
| POST | `/api/device/media/sync` | Upsert media metadata from device. Body: `{ items: [ { externalId, mediaType(IMAGE|VIDEO), fileName?, mimeType?, sizeBytes?, width?, height?, durationMs?, capturedAt?, thumbnailAvailable?, streamAvailable?, thumbnailRef?, streamRef?, checksum?, deletedOnDevice? } ] }`. Requires device permission `canSyncMetadata`. |
| GET | `/api/device/operations/pending` | List pending/acknowledged/in-progress remote operation requests for this device. |
| POST | `/api/device/operations/{id}/ack` | Acknowledge operation. Body: `{ message?, status? }`. |
| POST | `/api/device/operations/{id}/status` | Report execution status. Body: `{ status: ACKNOWLEDGED|IN_PROGRESS|COMPLETED|FAILED, message?, failureReason? }`. **Actual media operations must be authorized and executed by the Android application.** |

## 3. Admin device management (`/api/admin/devices`, `ROLE_ADMIN`)
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/devices` | List devices owned by admin. Query: `search`, `status`, `revoked`. |
| POST | `/api/admin/devices` | Create device record. Body: `{ deviceUuid, name, deviceType? }`. |
| GET | `/api/admin/devices/{id}` | Device details (status, permissions, latest consent, metadata). |
| PUT | `/api/admin/devices/{id}` | Update device. Body: `{ name?, deviceType? }`. |
| POST | `/api/admin/devices/{id}/revoke?revoked=true|false` | Revoke or restore device access. |
| PUT | `/api/admin/devices/{id}/permissions` | Set permissions. Body: `{ canView, canStream, canEdit, canDelete, canSyncMetadata }`. |
| POST | `/api/admin/devices/{id}/consent` | Record remote access consent state. Body: `{ granted, scope?, expiresAt?, notes? }`. Server enforces consent for media/operations. |

## 4. Gallery / media metadata (`/api/admin/devices/{deviceId}/media`)
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/devices/{deviceId}/media` | Paginated list. Query: `mediaType` (`IMAGE|VIDEO`), `search` (file name), `from` (YYYY-MM-DD), `to` (YYYY-MM-DD), `sort` (`capturedAt,desc|asc`, `name,asc|desc`, `indexedAt,asc|desc`), `page`, `size`. Requires ownership and active consent. |
| GET | `/api/admin/devices/{deviceId}/media/{mediaId}` | Media metadata details. |
| GET | `/api/admin/devices/{deviceId}/media/{mediaId}/thumbnail` | **Thumbnail retrieval contract**. Returns availability/contract/message; actual thumbnail bytes are supplied by authorized Android integration when available. No fake media returned. |
| GET | `/api/admin/devices/{deviceId}/media/{mediaId}/stream` | **On-demand media streaming contract**. Enforces consent and `canStream` permission; returns availability/contract/message. Actual streaming is fulfilled by Android/device integration when available. |

## 5. Remote operations (`/api/admin/operations`)
| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/operations` | Create operation request. Body: `{ deviceId, mediaId?, operationType(PREVIEW|EDIT|DELETE), payload?, expiresInMinutes? }`. Enforces ownership, consent, permissions, and device state. DELETE requires `mediaId`. |
| GET | `/api/admin/operations` | List operations for admin. Query: `deviceId?`, `page`, `size`. |
| GET | `/api/admin/operations/{id}` | Operation details/status tracking. |

**Lifecycle/status:** `PENDING` → `ACKNOWLEDGED`/`IN_PROGRESS` → `COMPLETED`/`FAILED`, or `EXPIRED` (time-based). Expiration is enforced by scheduled processing and opportunistic checks.

## 6. Dashboard & audit
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/dashboard` | Admin | Aggregated stats (devices online/offline/total, indexed images/videos, recent activity, connection statistics) derived from real records. |
| GET | `/api/admin/audit-logs` | Admin | Paginated audit logs. Query: `deviceId?`, `page`, `size`. |

## 7. Health & actuator
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/actuator/health` | Public | Health/readiness probe for deployment. |
| GET | `/actuator/info` | Public | App info (if enabled). |

## 8. WebSocket / STOMP

- **Endpoint:** `/ws` (SockJS also enabled at `/ws`)
- **Allowed origins:** configured via `app.websocket.allowed-origins` / `WS_ALLOWED_ORIGINS`
- **Authentication:** STOMP `CONNECT` must include `Authorization: Bearer <token>` (or `token` header). Invalid/missing token rejects connection.
- **Broker destinations:** `/topic/**`, `/queue/**`
- **App prefix:** `/app`
- **User prefix:** `/user`

### Events published by server (topics)
| Topic | Event type | Purpose |
|---|---|---|
| `/topic/devices`, `/topic/devices/{deviceId}` | `DEVICE_STATUS` | Device online/offline/status changes |
| `/topic/gallery`, `/topic/devices/{deviceId}/gallery` | `GALLERY_UPDATED` | Media metadata sync/update notifications |
| `/topic/operations`, `/topic/devices/{deviceId}/operations` | `OPERATION_UPDATED` | Remote operation request/status changes |

**Payload (example):**
```json
{ "type": "OPERATION_UPDATED", "deviceId": 1, "operationId": 10, "status": "ACKNOWLEDGED", "message": "...", "at": "2026-01-01T00:00:00Z" }
```

**Reconnection/heartbeat:** clients should implement reconnect/backoff. Server issues realtime events best-effort; persisted state remains source of truth. Device HTTP heartbeat endpoint (`/api/device/heartbeat`) is provided for liveness tracking.

## 9. Security notes
- Passwords hashed with BCrypt; JWT for stateless auth; refresh tokens stored hashed and revocable.
- CORS restricted to configured origins; rate limiting applied (in-memory, per instance).
- Admin/device authorization is enforced (ownership checks, device-role separation, consent and permission checks).
- Remote access requires explicit consent and can expire/be revoked; audit logs record sensitive actions.
- No hidden access or permission bypass mechanisms are implemented.

## 10. Android integration contracts (pending)
The backend defines and validates contracts for device enrollment, metadata sync, consent-aware access, and remote operation lifecycle. The Android app must:
1. Register/authenticate and protect device secrets.
2. Sync accurate metadata and availability flags/refs.
3. Respect user consent and execute operations locally on the device when authorized.
4. Report acknowledgments/status/failures back to the server.
5. Provide secure thumbnail/stream delivery mechanisms aligned with platform policy. 
