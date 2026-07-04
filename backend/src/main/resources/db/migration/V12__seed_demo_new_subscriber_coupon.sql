-- ───────────────────────────────────────────────────────────────────
-- V12: Demo homepage coupon seed
-- ───────────────────────────────────────────────────────────────────
-- Existing demo databases can have only legacy general coupons after V11.
-- Seed one explicit new_subscriber coupon for the default store so the
-- leader homepage subscription coupon flow is visible without manual setup.

INSERT INTO coupons (
    store_id,
    name,
    coupon_type,
    claim_condition,
    amount,
    threshold_amount,
    total_quantity,
    claimed_quantity,
    per_user_limit,
    start_time,
    end_time,
    status
)
SELECT
    1,
    '新人订阅立减 10 元',
    'amount',
    'new_subscriber',
    1000,
    2990,
    100,
    0,
    1,
    '2026-01-01 00:00:00',
    '2099-12-31 23:59:59',
    'active'
FROM stores
WHERE id = 1
  AND NOT EXISTS (
      SELECT 1
      FROM coupons
      WHERE store_id = 1
        AND claim_condition = 'new_subscriber'
  )
LIMIT 1;
