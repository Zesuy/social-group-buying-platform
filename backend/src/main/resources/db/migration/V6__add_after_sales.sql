-- ───────────────────────────────────────────────────────────────────
-- V6: Add after_sales table for refund workflow
-- ───────────────────────────────────────────────────────────────────
-- P1 Batch 05: After-sale refund and regression freeze.
-- Status: pending -> approved -> completed (refund done)
--         pending -> rejected
-- On refund completion: order becomes refunded, stock restored,
-- member stats reversed, coupon stays used.

CREATE TABLE after_sales (
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id                BIGINT       NOT NULL COMMENT '订单 ID',
    user_id                 BIGINT       NOT NULL COMMENT '买家用户 ID',
    leader_id               BIGINT       NOT NULL COMMENT '团长 ID',
    store_id                BIGINT       NOT NULL COMMENT '店铺 ID',
    type                    VARCHAR(16)  NOT NULL DEFAULT 'refund' COMMENT '售后类型: refund',
    reason                  VARCHAR(500) NOT NULL COMMENT '售后原因',
    status                  VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending / approved / rejected / completed',
    amount                  BIGINT       NOT NULL COMMENT '退款金额（分）',
    original_order_status   VARCHAR(32)  NOT NULL COMMENT '申请时的订单状态快照（DB枚举）',
    reject_reason           VARCHAR(500) NULL COMMENT '拒绝原因',
    approved_at             DATETIME     NULL COMMENT '审核通过时间',
    rejected_at             DATETIME     NULL COMMENT '拒绝时间',
    completed_at            DATETIME     NULL COMMENT '退款完成时间',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_after_sales_order_id (order_id),
    INDEX idx_after_sales_user_id (user_id),
    INDEX idx_after_sales_store_id (store_id),
    INDEX idx_after_sales_status (status)
) COMMENT='售后表';
