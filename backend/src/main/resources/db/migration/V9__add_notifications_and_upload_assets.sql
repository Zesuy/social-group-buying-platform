-- P1 Batch 08:站内通知和上传资产治理

CREATE TABLE upload_assets (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    uploader_user_id    BIGINT       NOT NULL,
    object_key          VARCHAR(512) NOT NULL,
    url                 VARCHAR(512) NOT NULL,
    original_filename   VARCHAR(255) NULL,
    content_type        VARCHAR(64)  NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    checksum_sha256     VARCHAR(64)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'temporary' COMMENT 'temporary / in_use / deleted',
    reference_count     INT          NOT NULL DEFAULT 0,
    last_referenced_at  DATETIME     NULL,
    deleted_at          DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_upload_assets_object_key UNIQUE (object_key)
) COMMENT='上传资产表';

CREATE INDEX idx_upload_assets_uploader ON upload_assets (uploader_user_id, created_at);
CREATE INDEX idx_upload_assets_status_created ON upload_assets (status, created_at);
CREATE INDEX idx_upload_assets_cleanup ON upload_assets (status, reference_count, created_at);

CREATE TABLE upload_asset_references (
    id          BIGINT       NOT NULL PRIMARY KEY,
    asset_id    BIGINT       NOT NULL,
    object_key  VARCHAR(512) NOT NULL,
    ref_type    VARCHAR(64)  NOT NULL,
    ref_id      BIGINT       NOT NULL,
    field_name  VARCHAR(64)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_upload_asset_reference UNIQUE (asset_id, ref_type, ref_id, field_name)
) COMMENT='上传资产引用表';

CREATE INDEX idx_upload_asset_references_ref ON upload_asset_references (ref_type, ref_id);
CREATE INDEX idx_upload_asset_references_asset ON upload_asset_references (asset_id);

CREATE TABLE notifications (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    recipient_user_id   BIGINT       NOT NULL,
    sender_user_id      BIGINT       NULL,
    type                VARCHAR(32)  NOT NULL,
    title               VARCHAR(80)  NOT NULL,
    summary             VARCHAR(200) NOT NULL,
    body                TEXT         NULL,
    target_type         VARCHAR(32)  NOT NULL,
    target_id           BIGINT       NOT NULL,
    action_url          VARCHAR(255) NOT NULL,
    dedupe_key          VARCHAR(128) NOT NULL,
    read_status         VARCHAR(16)  NOT NULL DEFAULT 'unread' COMMENT 'unread / read',
    read_at             DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_notifications_dedupe_key UNIQUE (dedupe_key)
) COMMENT='站内通知表';

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_user_id, created_at);
CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_user_id, read_status, created_at);
CREATE INDEX idx_notifications_target ON notifications (target_type, target_id);
