ALTER TABLE devices ADD COLUMN device_secret_hash VARCHAR(255) NULL;
CREATE INDEX idx_media_device_deleted_type ON media_metadata(device_id, deleted_on_device, media_type);
CREATE INDEX idx_ops_admin_requested ON media_operation_requests(admin_id, requested_at);
