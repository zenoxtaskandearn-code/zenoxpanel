-- Remote Gallery Management Platform initial schema (PostgreSQL)

CREATE TABLE admins (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admins_email UNIQUE (email)
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    device_uuid VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    device_type VARCHAR(50) NULL,
    owner_admin_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    last_seen_at TIMESTAMPTZ NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ NULL,
    last_known_ip VARCHAR(64) NULL,
    app_version VARCHAR(50) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_devices_device_uuid UNIQUE (device_uuid),
    CONSTRAINT fk_devices_owner_admin FOREIGN KEY (owner_admin_id) REFERENCES admins(id)
);
CREATE INDEX idx_devices_owner ON devices(owner_admin_id);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_revoked ON devices(revoked);

CREATE TABLE device_sessions (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    session_token_hash VARCHAR(255) NOT NULL,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMPTZ NULL,
    last_heartbeat_at TIMESTAMPTZ NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    disconnect_reason VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_sessions_device FOREIGN KEY (device_id) REFERENCES devices(id)
);
CREATE INDEX idx_device_sessions_device_active ON device_sessions(device_id, active);
CREATE INDEX idx_device_sessions_token ON device_sessions(session_token_hash);

CREATE TABLE device_permissions (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_stream BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_sync_metadata BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_device_permissions_device UNIQUE (device_id),
    CONSTRAINT fk_device_permissions_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE TABLE remote_access_consents (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT FALSE,
    scope VARCHAR(255) NULL,
    granted_at TIMESTAMPTZ NULL,
    revoked_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consents_device FOREIGN KEY (device_id) REFERENCES devices(id),
    CONSTRAINT fk_consents_admin FOREIGN KEY (admin_id) REFERENCES admins(id)
);
CREATE INDEX idx_consents_device_admin ON remote_access_consents(device_id, admin_id);
CREATE INDEX idx_consents_granted ON remote_access_consents(granted);

CREATE TABLE media_metadata (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    external_id VARCHAR(191) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NULL,
    mime_type VARCHAR(100) NULL,
    size_bytes BIGINT NULL,
    width INT NULL,
    height INT NULL,
    duration_ms BIGINT NULL,
    captured_at TIMESTAMPTZ NULL,
    thumbnail_available BOOLEAN NOT NULL DEFAULT FALSE,
    stream_available BOOLEAN NOT NULL DEFAULT FALSE,
    thumbnail_ref VARCHAR(512) NULL,
    stream_ref VARCHAR(512) NULL,
    checksum VARCHAR(128) NULL,
    deleted_on_device BOOLEAN NOT NULL DEFAULT FALSE,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_media_device_external UNIQUE (device_id, external_id),
    CONSTRAINT fk_media_device FOREIGN KEY (device_id) REFERENCES devices(id)
);
CREATE INDEX idx_media_device_type ON media_metadata(device_id, media_type);
CREATE INDEX idx_media_captured ON media_metadata(captured_at);
CREATE INDEX idx_media_name ON media_metadata(file_name);

CREATE TABLE media_operation_requests (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    media_id BIGINT NULL,
    operation_type VARCHAR(30) NOT NULL,
    payload TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500) NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NULL,
    acknowledged_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    device_ack_message VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ops_device FOREIGN KEY (device_id) REFERENCES devices(id),
    CONSTRAINT fk_ops_admin FOREIGN KEY (admin_id) REFERENCES admins(id),
    CONSTRAINT fk_ops_media FOREIGN KEY (media_id) REFERENCES media_metadata(id)
);
CREATE INDEX idx_ops_device_status ON media_operation_requests(device_id, status);
CREATE INDEX idx_ops_status_expires ON media_operation_requests(status, expires_at);
CREATE INDEX idx_ops_requested ON media_operation_requests(requested_at);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(100) NULL,
    device_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NULL,
    resource_id VARCHAR(100) NULL,
    details TEXT NULL,
    ip_address VARCHAR(64) NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_device FOREIGN KEY (device_id) REFERENCES devices(id)
);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_actor ON audit_logs(actor_type, actor_id);
CREATE INDEX idx_audit_action ON audit_logs(action);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    subject_type VARCHAR(20) NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ NULL,
    replaced_by_token_hash VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_subject ON refresh_tokens(subject_type, subject_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
