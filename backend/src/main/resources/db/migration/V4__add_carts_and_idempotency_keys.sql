-- ───────────────────────────────────────────────────────────────────
-- V4: Add carts and idempotency_keys tables
-- ───────────────────────────────────────────────────────────────────
-- P1 Batch 03: Cart management + Idempotency-Key support.

-- ── 1. carts ──────────────────────────────────────────────────────
CREATE TABLE carts (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL COMMENT '买家用户 ID',
    group_buy_id        BIGINT       NOT NULL COMMENT '团购活动 ID',
    group_buy_item_id   BIGINT       NOT NULL COMMENT '团购商品 ID',
    quantity            INT          NOT NULL DEFAULT 1 COMMENT '加购数量',
    share_token_id      BIGINT       NULL COMMENT '已关联的分享 token ID，用于 hidden 团购后续校验',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_carts_user_item UNIQUE (user_id, group_buy_item_id)
) COMMENT='购物车表';

CREATE INDEX idx_carts_user_id ON carts (user_id);
CREATE INDEX idx_carts_group_buy_id ON carts (group_buy_id);
CREATE INDEX idx_carts_group_buy_item_id ON carts (group_buy_item_id);

-- ── 2. idempotency_keys ───────────────────────────────────────────
CREATE TABLE idempotency_keys (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL COMMENT '请求用户 ID',
    method              VARCHAR(16)  NOT NULL COMMENT 'HTTP method, e.g. POST',
    path                VARCHAR(512) NOT NULL COMMENT '实际请求路径',
    idempotency_key     VARCHAR(256) NOT NULL COMMENT 'Idempotency-Key 值',
    request_hash        VARCHAR(128) NOT NULL COMMENT '请求体 hash（SHA-256），无 body 用 path',
    status              VARCHAR(16)  NOT NULL DEFAULT 'processing' COMMENT 'processing / succeeded / failed',
    response_body_json  TEXT         NULL COMMENT '成功时 response data 的 JSON 快照',
    error_code          VARCHAR(64)  NULL COMMENT '失败时的错误码（失败时使用）',
    error_message       TEXT         NULL COMMENT '失败时的错误消息',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_keys_user_method_path_key UNIQUE (user_id, method, path, idempotency_key)
) COMMENT='幂等键记录表';

CREATE INDEX idx_idempotency_keys_user_id ON idempotency_keys (user_id);
CREATE INDEX idx_idempotency_keys_key ON idempotency_keys (idempotency_key);
CREATE INDEX idx_idempotency_keys_status ON idempotency_keys (status);
