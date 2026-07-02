-- ───────────────────────────────────────────────────────────────────
-- V5: Add coupons, user_coupons, member_level_rules tables + orders extension
-- ───────────────────────────────────────────────────────────────────
-- P1 Batch 04: Coupon / red packet and member growth.

-- ── 1. coupons ─────────────────────────────────────────────────────
-- Store-level coupons. coupon_type: amount / red_packet.
-- Both are treated as fixed-amount discounts in this batch.
CREATE TABLE coupons (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT       NOT NULL COMMENT '所属店铺 ID',
    name                VARCHAR(64)  NOT NULL COMMENT '优惠券名称',
    coupon_type         VARCHAR(16)  NOT NULL COMMENT '优惠券类型: amount / red_packet',
    amount              BIGINT       NOT NULL COMMENT '抵扣金额（分）',
    threshold_amount    BIGINT       NOT NULL DEFAULT 0 COMMENT '使用门槛（分），0 表示无门槛',
    total_quantity      INT          NOT NULL COMMENT '总库存',
    claimed_quantity    INT          NOT NULL DEFAULT 0 COMMENT '已领取数量',
    per_user_limit      INT          NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    start_time          DATETIME     NOT NULL COMMENT '有效期开始时间',
    end_time            DATETIME     NOT NULL COMMENT '有效期结束时间',
    status              VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态: active / disabled',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupons_store_name UNIQUE (store_id, name)
) COMMENT='优惠券表';

CREATE INDEX idx_coupons_store_id ON coupons (store_id);
CREATE INDEX idx_coupons_status ON coupons (status);
CREATE INDEX idx_coupons_end_time ON coupons (end_time);

-- ── 2. user_coupons ────────────────────────────────────────────────
-- User-owned coupons claimed from a store coupon.
-- Status: unused / locked / used / expired.
CREATE TABLE user_coupons (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL COMMENT '用户 ID',
    coupon_id           BIGINT       NOT NULL COMMENT '优惠券 ID',
    coupon_name         VARCHAR(64)  NOT NULL COMMENT '券名快照',
    coupon_type         VARCHAR(16)  NOT NULL COMMENT '券类型快照',
    amount              BIGINT       NOT NULL COMMENT '抵扣金额快照（分）',
    threshold_amount    BIGINT       NOT NULL DEFAULT 0 COMMENT '门槛快照（分）',
    status              VARCHAR(16)  NOT NULL DEFAULT 'unused' COMMENT '状态: unused / locked / used / expired',
    locked_order_id     BIGINT       NULL COMMENT '锁定的订单 ID',
    claimed_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    locked_at           DATETIME     NULL COMMENT '锁定时间',
    used_at             DATETIME     NULL COMMENT '使用时间',
    expired_at          DATETIME     NULL COMMENT '过期时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='用户优惠券表';

CREATE INDEX idx_user_coupons_user_id ON user_coupons (user_id);
CREATE INDEX idx_user_coupons_coupon_id ON user_coupons (coupon_id);
CREATE INDEX idx_user_coupons_status ON user_coupons (status);
CREATE INDEX idx_user_coupons_locked_order_id ON user_coupons (locked_order_id);

-- ── 3. member_level_rules ──────────────────────────────────────────
-- Store-level member tier rules. Ordered by min_growth_value ascending.
-- PUT replaces all rules for a store (delete + insert in a transaction).
CREATE TABLE member_level_rules (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT       NOT NULL COMMENT '所属店铺 ID',
    level_name          VARCHAR(32)  NOT NULL COMMENT '等级名称，如 V0/V1/V2',
    min_growth_value    INT          NOT NULL DEFAULT 0 COMMENT '最低成长值门槛',
    sort_order          INT          NOT NULL DEFAULT 0 COMMENT '排序（从小到大）',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_level_rules_store_sort UNIQUE (store_id, sort_order)
) COMMENT='会员等级规则表';

CREATE INDEX idx_level_rules_store_id ON member_level_rules (store_id);

-- ── 4. Extend orders with coupon snapshot fields ────────────────────
-- Note: H2 (test) does not support COMMENT in ALTER TABLE ADD COLUMN.
-- Comments are MySQL-only documentation; they don't affect H2 behavior.
ALTER TABLE orders
    ADD COLUMN user_coupon_id   BIGINT       NULL;
ALTER TABLE orders
    ADD COLUMN coupon_id        BIGINT       NULL;
ALTER TABLE orders
    ADD COLUMN coupon_name      VARCHAR(64)  NULL;
ALTER TABLE orders
    ADD COLUMN coupon_type      VARCHAR(16)  NULL;
