-- ───────────────────────────────────────────────────────────────────
-- V3: Add group_buy_share_tokens table for share card functionality
-- ───────────────────────────────────────────────────────────────────
-- Share tokens allow access to hidden group buys and are long-lived.
-- Each group buy can have at most one active token at a time.

CREATE TABLE group_buy_share_tokens (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_buy_id    BIGINT       NOT NULL,
    token           VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / expired / revoked',
    expires_at      DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_share_tokens_token UNIQUE (token),
    CONSTRAINT uk_share_tokens_group_buy_active UNIQUE (group_buy_id, status)
) COMMENT='团购分享 token 表';

CREATE INDEX idx_share_tokens_group_buy_id ON group_buy_share_tokens (group_buy_id);
CREATE INDEX idx_share_tokens_token ON group_buy_share_tokens (token);
