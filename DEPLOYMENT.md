# Deployment Guide (Render)

This project is designed for:
- **Backend**: Render Web Service (Docker) running Spring Boot on port `$PORT`
- **Frontend**: Render Static Site (Vite build) with SPA rewrite to `index.html`
- **Database**: External MySQL provider (Render managed databases are PostgreSQL; MySQL is used by requirement)

> Do **not** claim a deployment is successful unless it has actually been deployed and verified in the target environment.

## 1. Prerequisites
- A Git repository containing this project (push to GitHub/GitLab) or use Render Blueprint from the repo
- An external MySQL 8 database (e.g. Aiven, PlanetScale/MySQL-compatible provider, Railway MySQL, DigitalOcean, or any managed MySQL). Collect:
  - JDBC URL, username, password
  - Ensure the database allows connections from Render (networking/SSL as required by provider)
- Render account

## 2. Environment variables

### Backend (Web Service)
| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` |
| `PORT` | Provided by Render | Server port (app binds to `${PORT}`) |
| `SPRING_DATASOURCE_URL` | Yes | JDBC MySQL URL, e.g. `jdbc:mysql://host:3306/db?useSSL=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | Yes | DB username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | DB password |
| `JWT_SECRET` | Yes | Strong random secret (>= 32 bytes). Do not use default dev value. |
| `JWT_ACCESS_MINUTES` | No | Access token TTL (default 30) |
| `JWT_REFRESH_DAYS` | No | Refresh token TTL (default 14) |
| `JWT_DEVICE_ACCESS_MINUTES` | No | Device access token TTL (default 60) |
| `ADMIN_REGISTRATION_ENABLED` | Recommended | `false` in production after initial admin setup (or control carefully) |
| `ADMIN_REGISTRATION_INVITE_CODE` | Optional | Invite code when registration is enabled |
| `CORS_ALLOWED_ORIGINS` | Yes | Frontend origin(s), comma-separated (e.g. `https://your-frontend.onrender.com`) |
| `WS_ALLOWED_ORIGINS` | Yes | Frontend origin(s) allowed for WebSocket/STOMP |
| `RATE_LIMIT_RPM` | No | Requests per minute per client key (default 120) |
| `OPERATION_EXPIRY_MINUTES` | No | Default remote operation expiry (default 30) |

### Frontend (Static Site, build-time)
| Variable | Required | Description |
|---|---|---|
| `VITE_API_BASE_URL` | Yes | Backend base URL, e.g. `https://your-backend.onrender.com` (no trailing slash required) |

> Changing `VITE_API_BASE_URL` requires a frontend rebuild/redeploy because Vite inlines env vars at build time.

## 3. Deploy backend (Render Web Service, Docker)
1. In Render: **New +** → **Web Service** → connect repo.
2. **Root Directory:** `backend`
3. **Runtime:** Docker
4. **Dockerfile Path:** `./Dockerfile` (relative to backend root)
5. **Health Check Path:** `/actuator/health`
6. Set environment variables from the table above.
7. Deploy. On startup Flyway applies migrations automatically (`spring.flyway.enabled=true`, `ddl-auto: validate`).

**Notes:**
- Container listens on `PORT` (Render-provided) via entrypoint/profile config.
- Ensure CORS/WS origins exactly match the deployed frontend URL (scheme + host).
- Use HTTPS URLs in production.

## 4. Deploy frontend (Render Static Site)
1. In Render: **New +** → **Static Site** → connect same repo.
2. **Root Directory:** `frontend`
3. **Build Command:** `npm ci && npm run build` (or `npm install && npm run build`)
4. **Publish Directory:** `dist`
5. Set `VITE_API_BASE_URL` to the backend URL.
6. **SPA fallback:** add rewrite rule `/* -> /index.html` (Render dashboard “Redirects/Rewrites”, or via `render.yaml` routes). `frontend/public/_redirects` is included for compatible hosts.

## 5. Using `render.yaml` (Blueprint)
A sample blueprint is provided at repo root: [`render.yaml`](render.yaml).
- It defines backend (Docker) and frontend (static) services.
- Secrets/connection strings are marked `sync: false` (set in Render dashboard) except generated `JWT_SECRET`.
- Review and adjust names, plans, regions, and env values before applying.
- You still must provision **external MySQL** separately and supply datasource env vars.

## 6. Post-deploy verification checklist
- [ ] `GET https://<backend>/actuator/health` returns healthy status
- [ ] Admin registration/login works according to configured registration policy
- [ ] Frontend loads and can authenticate against backend (CORS/HTTPS correct)
- [ ] Device/admin APIs reachable with valid JWT; unauthorized requests rejected
- [ ] WebSocket `/ws` accepts authenticated STOMP CONNECT (when tested with valid token)
- [ ] No real secrets committed to repository

## 7. Operational considerations
- **Database backups/migrations:** Flyway manages schema versions; never reset/destroy existing user data. Back up DB before upgrades.
- **Registration control:** disable open admin registration in production after bootstrapping (`ADMIN_REGISTRATION_ENABLED=false`) or require invite code.
- **Rate limiting:** in-memory limiter is per-instance; for multi-instance scaling use a shared/distributed rate limiter.
- **Media delivery:** thumbnail/stream endpoints return contracts; actual media bytes depend on Android device integration and secure delivery mechanisms.

## 8. Local production-like run
Backend with Docker:
```bash
docker build -t gallery-backend ./backend
docker run -p 8080:8080 --env-file .env gallery-backend
```
Frontend static build served locally:
```bash
cd frontend && npm ci && npm run build && npm run preview
``` (or any static server pointed at `dist` with SPA fallback)
