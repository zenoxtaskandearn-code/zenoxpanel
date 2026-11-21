# Remote Gallery Management Platform

A web platform for administering registered devices and their indexed gallery metadata with secure remote-operation workflows.

This repository contains:
- **Backend**: Spring Boot 3 (Java 21), Spring Security, JPA, Flyway, MySQL, JWT, WebSocket/STOMP, Maven
- **Frontend**: React (Vite, JavaScript), Tailwind CSS, Lucide React, Axios, React Router, TanStack Query
- **Database**: MySQL (external provider supported; Flyway migrations included)
- **Deployment targets**: Render Web Service (backend, Docker) + Render Static Site (frontend)

> **Important:** The Android APK/device agent is **not** included in this repository and will be built separately. Actual media access and remote media operations (preview/edit/delete execution on device content) must be authorized and executed by the Android application. This platform provides backend contracts, authentication, validation, request tracking, audit logging, and an admin dashboard.

## Features implemented
- Admin authentication (register/login/refresh/logout), JWT access + refresh tokens, BCrypt password hashing, RBAC-protected admin APIs, profile/password management
- Device management: registration/auth contracts, listing/details, online/offline status, heartbeat, revocation, ownership, permissions, remote access consent
- Gallery infrastructure: media metadata model and sync API, pagination/search/date filtering/sorting, thumbnail/stream **contracts** (no fabricated media)
- Remote operations: request creation/tracking, device acknowledgment/status updates, expiration handling, audit logs
- WebSocket/STOMP infrastructure: authenticated CONNECT, device/gallery/operation event topics, realtime publish hooks
- Admin dashboard UI: Login, Dashboard, Devices, Device Details, Gallery, Image/Video Viewers, Activity Logs, Settings, Profile (dark/light mode, responsive layout)
- Security baseline: JWT auth, CORS restrictions, input validation, rate limiting, device-specific authorization, consent enforcement, audit logging, secure endpoint design
- Deployment artifacts: backend Dockerfile, production profile, health endpoint, env config, `.env.example`, `render.yaml`, deployment/API docs

## Repository structure
```text
backend/    Spring Boot application (Maven, Flyway, Dockerfile)
frontend/   React + Vite admin dashboard
docs/       (reserved)
.env.example
render.yaml
README.md
DEPLOYMENT.md
API_DOCUMENTATION.md
PHASE_PROGRESS.md
```

## Prerequisites
- Java 21 (backend target) and Maven 3.9+ for local backend builds/tests
- Node.js 18+ and npm for frontend
- MySQL 8+ (local or external)

## Local development

### 1. Database
Create a MySQL database/user or use defaults from `backend/src/main/resources/application.yml`:
- URL: `jdbc:mysql://localhost:3306/gallery_platform...`
- Username: `root`
- Password: `password`

Override via environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

### 2. Backend
```bash
cd backend
mvn spring-boot:run
```
- Default port: `8080`
- Health: `GET /actuator/health`
- Flyway runs automatically on startup (`ddl-auto: validate`)

**Production profile:** `SPRING_PROFILES_ACTIVE=prod`

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
- Dev server: `http://localhost:5173`
- API base URL: `VITE_API_BASE_URL` (defaults to `http://localhost:8080`)

Build for production:
```bash
npm run build
npm run preview
```

## Configuration
See `.env.example` for backend and frontend environment variables. **Never commit real secrets.**

Key settings:
- `JWT_SECRET` (required in production; use a strong random value)
- `CORS_ALLOWED_ORIGINS` / `WS_ALLOWED_ORIGINS` (comma-separated frontend origins)
- `ADMIN_REGISTRATION_ENABLED` and `ADMIN_REGISTRATION_INVITE_CODE` (controlled admin registration)
- `VITE_API_BASE_URL` (frontend build-time API URL)

## Testing
### Backend
```bash
cd backend
mvn test
```
Includes integration tests using H2 in `test` profile (Flyway disabled, schema auto-created for tests).

### Frontend
```bash
cd frontend
npm run build
```

## Deployment
See [DEPLOYMENT.md](DEPLOYMENT.md) for Render deployment instructions and [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for API/WebSocket contracts.

## Android integration (pending)
To complete end-to-end functionality, the Android application must:
- Authenticate as a device (`/api/device/register`, `/api/device/auth`) and maintain heartbeats/status
- Sync gallery metadata (`POST /api/device/media/sync`)
- Connect to WebSocket `/ws` with device JWT and handle realtime events
- Poll/receive and execute remote operation requests (`/api/device/operations/pending`, `/ack`, `/status`) **only when authorized by consent/permissions**
- Provide thumbnail/stream delivery according to negotiated contracts/refs
- Obtain explicit device-owner consent for remote access and honor revocation

The backend enforces server-side authorization, consent checks, request expiration, and audit logging, but cannot perform on-device media operations without the Android agent.
