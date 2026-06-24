-- ───────────────────────────────────────────────────────────────────
-- V1: Create all MVP core tables
-- ───────────────────────────────────────────────────────────────────
-- Batch 0 creates the full MVP table set in one migration.
-- Subsequent batches only add migrations when truly necessary.

-- ── 1. users ──────────────────────────────────────────────────────
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(64)  NOT NULL,
    avatar_url  VARCHAR(512) NULL,
    phone       VARCHAR(20)  NULL,
    wechat_openid VARCHAR(128) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal / disabled',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='用户表';

-- ── 2. leaders ────────────────────────────────────────────────────
CREATE TABLE leaders (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    display_name    VARCHAR(64)  NOT NULL,
    avatar_url      VARCHAR(512) NULL,
    bio             TEXT         NULL,
    service_status  VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal / disabled',
    member_count    INT          NOT NULL DEFAULT 0,
    follower_count  INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_leaders_user_id UNIQUE (user_id)
) COMMENT='团长身份表';

CREATE INDEX idx_leaders_user_id ON leaders (user_id);

-- ── 3. stores ─────────────────────────────────────────────────────
CREATE TABLE stores (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    leader_id               BIGINT       NOT NULL,
    name                    VARCHAR(128) NOT NULL,
    logo_url                VARCHAR(512) NULL,
    description             TEXT         NULL,
    default_delivery_type   VARCHAR(20)  NOT NULL DEFAULT 'express' COMMENT 'express / pickup / local_delivery',
    distribution_enabled    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '帮卖开关，MVP 仅预留',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / disabled',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_stores_leader_id UNIQUE (leader_id)
) COMMENT='店铺表';

CREATE INDEX idx_stores_leader_id ON stores (leader_id);

-- ── 4. products ───────────────────────────────────────────────────
CREATE TABLE products (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT       NOT NULL,
    name                VARCHAR(128) NOT NULL,
    description         TEXT         NULL,
    cover_image_url     VARCHAR(512) NULL,
    category_id         BIGINT       NULL COMMENT 'P1 类目用',
    base_price_amount   BIGINT       NOT NULL DEFAULT 0 COMMENT '基础价格，单位分',
    stock               INT          NOT NULL DEFAULT 0 COMMENT '参考库存，不参与支付扣减',
    status              VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / inactive / deleted',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='商品表';

CREATE INDEX idx_products_store_id ON products (store_id);
CREATE INDEX idx_products_status ON products (status);

-- ── 5. group_buys ─────────────────────────────────────────────────
CREATE TABLE group_buys (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT       NOT NULL,
    leader_id           BIGINT       NOT NULL,
    title               VARCHAR(128) NOT NULL,
    introduction        TEXT         NULL,
    cover_image_url     VARCHAR(512) NULL,
    group_type          VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal / presale / coupon / signup',
    delivery_type       VARCHAR(20)  NOT NULL DEFAULT 'express' COMMENT 'express / pickup / local_delivery',
    shipping_time       DATETIME     NULL COMMENT '承诺发货时间',
    start_time          DATETIME     NULL,
    end_time            DATETIME     NULL,
    visibility          VARCHAR(20)  NOT NULL DEFAULT 'public' COMMENT 'public / hidden',
    status              VARCHAR(20)  NOT NULL DEFAULT 'published' COMMENT 'draft / published / ended / removed',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='团购活动表';

CREATE INDEX idx_group_buys_store_id ON group_buys (store_id);
CREATE INDEX idx_group_buys_leader_id ON group_buys (leader_id);
CREATE INDEX idx_group_buys_status ON group_buys (status);

-- ── 6. group_buy_items ────────────────────────────────────────────
CREATE TABLE group_buy_items (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_buy_id        BIGINT       NOT NULL,
    product_id          BIGINT       NOT NULL,
    sku_id              BIGINT       NULL COMMENT 'MVP 不使用',
    display_name        VARCHAR(128) NOT NULL COMMENT '团购中展示的商品名',
    group_price_amount  BIGINT       NOT NULL COMMENT '团购价，单位分，下单价格以此为准',
    group_stock         INT          NOT NULL DEFAULT 0 COMMENT '当前剩余可售库存，必须 >= 0',
    sold_count          INT          NOT NULL DEFAULT 0 COMMENT '已支付售出数量',
    sort_order          INT          NOT NULL DEFAULT 0,
    show_stock          BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'P1 使用',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='团购商品关系表';

CREATE INDEX idx_group_buy_items_group_buy_id ON group_buy_items (group_buy_id);
CREATE INDEX idx_group_buy_items_product_id ON group_buy_items (product_id);

-- ── 7. addresses ──────────────────────────────────────────────────
CREATE TABLE addresses (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    receiver_name   VARCHAR(64)  NOT NULL,
    receiver_phone  VARCHAR(20)  NOT NULL,
    province        VARCHAR(32)  NOT NULL,
    city            VARCHAR(32)  NOT NULL,
    district        VARCHAR(32)  NOT NULL,
    detail          VARCHAR(256) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='地址表';

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- ── 8. orders ─────────────────────────────────────────────────────
CREATE TABLE orders (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no          VARCHAR(64)  NOT NULL,
    user_id           BIGINT       NOT NULL,
    leader_id         BIGINT       NOT NULL,
    store_id          BIGINT       NOT NULL,
    group_buy_id      BIGINT       NOT NULL,
    address_id        BIGINT       NOT NULL,
    receiver_name     VARCHAR(64)  NOT NULL COMMENT '收货人快照',
    receiver_phone    VARCHAR(20)  NOT NULL COMMENT '收货手机号快照',
    province          VARCHAR(32)  NOT NULL COMMENT '省份快照',
    city              VARCHAR(32)  NOT NULL COMMENT '城市快照',
    district          VARCHAR(32)  NOT NULL COMMENT '区县快照',
    detail            VARCHAR(256) NOT NULL COMMENT '详细地址快照',
    full_address      VARCHAR(512) NOT NULL COMMENT '完整地址快照',
    total_amount      BIGINT       NOT NULL COMMENT '商品总额，单位分',
    discount_amount   BIGINT       NOT NULL DEFAULT 0 COMMENT '优惠金额，单位分，MVP 默认 0',
    pay_amount        BIGINT       NOT NULL COMMENT '应付金额，单位分',
    pay_status        VARCHAR(20)  NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid / paid / refunded',
    order_status      VARCHAR(20)  NOT NULL DEFAULT 'pending_pay' COMMENT 'pending_pay / paid / shipped / completed / canceled / after_sale',
    remark            VARCHAR(256) NULL,
    paid_at           DATETIME     NULL,
    shipped_at        DATETIME     NULL,
    completed_at      DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no)
) COMMENT='订单表';

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_store_id ON orders (store_id);
CREATE INDEX idx_orders_leader_id ON orders (leader_id);
CREATE INDEX idx_orders_group_buy_id ON orders (group_buy_id);
CREATE INDEX idx_orders_order_status ON orders (order_status);

-- ── 9. order_items ────────────────────────────────────────────────
CREATE TABLE order_items (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT       NOT NULL,
    product_id          BIGINT       NOT NULL,
    sku_id              BIGINT       NULL COMMENT 'MVP 不使用',
    group_buy_item_id   BIGINT       NOT NULL,
    product_name        VARCHAR(128) NOT NULL COMMENT '下单时商品名称快照',
    sku_name            VARCHAR(128) NOT NULL DEFAULT '' COMMENT '下单时规格快照',
    unit_price_amount   BIGINT       NOT NULL COMMENT '下单单价，单位分',
    quantity            INT          NOT NULL DEFAULT 1,
    total_amount        BIGINT       NOT NULL COMMENT '明细总额，单位分',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='订单明细表';

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- ── 10. shipments ─────────────────────────────────────────────────
CREATE TABLE shipments (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT       NOT NULL,
    delivery_type       VARCHAR(20)  NOT NULL DEFAULT 'express' COMMENT 'express / pickup / local_delivery',
    logistics_company   VARCHAR(64)  NULL,
    tracking_no         VARCHAR(128) NULL,
    shipped_by          BIGINT       NOT NULL COMMENT '操作人，通常为团长用户 ID',
    shipped_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='发货记录表';

CREATE INDEX idx_shipments_order_id ON shipments (order_id);

-- ── 11. subscriptions ─────────────────────────────────────────────
CREATE TABLE subscriptions (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    leader_id       BIGINT       NOT NULL,
    store_id        BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / canceled',
    source          VARCHAR(32)  NULL COMMENT 'homepage / product_detail / invitation',
    subscribed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at     DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_subscriptions_user_leader UNIQUE (user_id, leader_id)
) COMMENT='订阅关系表';

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_leader_id ON subscriptions (leader_id);

-- ── 12. member_relations ──────────────────────────────────────────
CREATE TABLE member_relations (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    leader_id           BIGINT       NOT NULL,
    store_id            BIGINT       NOT NULL,
    level_name          VARCHAR(32)  NOT NULL DEFAULT 'V0',
    growth_value        INT          NOT NULL DEFAULT 0,
    total_order_amount  BIGINT       NOT NULL DEFAULT 0 COMMENT '累计支付金额，单位分',
    total_orders        INT          NOT NULL DEFAULT 0,
    last_order_at       DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_relations_user_store UNIQUE (user_id, store_id)
) COMMENT='会员关系表';

CREATE INDEX idx_member_relations_user_id ON member_relations (user_id);
CREATE INDEX idx_member_relations_leader_id ON member_relations (leader_id);
CREATE INDEX idx_member_relations_store_id ON member_relations (store_id);
