-- ───────────────────────────────────────────────────────────────────
-- V11: Add coupon claim condition
-- ───────────────────────────────────────────────────────────────────
-- general: existing store coupon behavior
-- new_subscriber: buyer must subscribe to the store leader before claiming

ALTER TABLE coupons
    ADD COLUMN claim_condition VARCHAR(32) NOT NULL DEFAULT 'general';

CREATE INDEX idx_coupons_claim_condition ON coupons (claim_condition);
