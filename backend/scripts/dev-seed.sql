-- Local development seed data for the private-domain group-buying MVP.
-- Usage:
--   docker exec -i groupshop-mysql mysql -uroot -proot groupshop < backend/scripts/dev-seed.sql
--
-- This script intentionally resets business data but keeps Flyway history and
-- platform categories. Do not run it against a shared or production database.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM after_sales;
DELETE FROM shipments;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM idempotency_keys;
DELETE FROM carts;
DELETE FROM user_coupons;
DELETE FROM coupons;
DELETE FROM member_level_rules;
DELETE FROM member_relations;
DELETE FROM subscriptions;
DELETE FROM favorite_items;
DELETE FROM browsing_histories;
DELETE FROM group_buy_share_tokens;
DELETE FROM group_buy_items;
DELETE FROM group_buys;
DELETE FROM products;
DELETE FROM stores;
DELETE FROM leaders;
DELETE FROM addresses;
DELETE FROM users;

ALTER TABLE after_sales AUTO_INCREMENT = 1;
ALTER TABLE shipments AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE idempotency_keys AUTO_INCREMENT = 1;
ALTER TABLE carts AUTO_INCREMENT = 1;
ALTER TABLE user_coupons AUTO_INCREMENT = 1;
ALTER TABLE coupons AUTO_INCREMENT = 1;
ALTER TABLE member_level_rules AUTO_INCREMENT = 1;
ALTER TABLE member_relations AUTO_INCREMENT = 1;
ALTER TABLE subscriptions AUTO_INCREMENT = 1;
ALTER TABLE favorite_items AUTO_INCREMENT = 1;
ALTER TABLE browsing_histories AUTO_INCREMENT = 1;
ALTER TABLE group_buy_share_tokens AUTO_INCREMENT = 1;
ALTER TABLE group_buy_items AUTO_INCREMENT = 1;
ALTER TABLE group_buys AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE stores AUTO_INCREMENT = 1;
ALTER TABLE leaders AUTO_INCREMENT = 1;
ALTER TABLE addresses AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- Users
INSERT INTO users (nickname, avatar_url, phone, status, created_at, updated_at)
VALUES
('王姐鲜果团', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80', '13800000000', 'normal', NOW(), NOW()),
('陈小满', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&q=80', '13900000000', 'normal', NOW(), NOW()),
('周晨', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80', '13900000001', 'normal', NOW(), NOW());

SET @leader_user_id = (SELECT id FROM users WHERE phone = '13800000000' LIMIT 1);
SET @buyer_user_id = (SELECT id FROM users WHERE phone = '13900000000' LIMIT 1);
SET @fresh_fruit_category_id = (SELECT id FROM product_categories WHERE code = 'fresh_fruit' LIMIT 1);

-- Leader and store
INSERT INTO leaders (user_id, display_name, avatar_url, bio, service_status, member_count, follower_count, created_at, updated_at)
VALUES (
  @leader_user_id,
  '王姐鲜果团',
  'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80',
  '小区群每周开团，主做当季鲜果和社区自提。',
  'normal',
  126,
  582,
  NOW(),
  NOW()
);

SET @leader_id = LAST_INSERT_ID();

INSERT INTO stores (
  leader_id, name, logo_url, description, default_delivery_type,
  distribution_enabled, status, latitude, longitude, created_at, updated_at
)
VALUES (
  @leader_id,
  '王姐社区鲜果店',
  'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=400&q=80',
  '当季鲜果集中收单，同城配送到社区。周末团优先服务桂花城、文三路和古墩路附近小区。',
  'local_delivery',
  FALSE,
  'active',
  30.2741500,
  120.1253800,
  NOW(),
  NOW()
);

SET @store_id = LAST_INSERT_ID();

-- Product library
INSERT INTO products (
  store_id, name, description, cover_image_url, category_id,
  base_price_amount, stock, status, detail_image_urls, created_at, updated_at
)
VALUES
(
  @store_id,
  '阳山水蜜桃',
  '单份约 5 斤，中大果混装。偏软甜口，适合现吃；运输中轻微压痕不影响食用。',
  'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=900&q=80',
  @fresh_fruit_category_id,
  3990,
  180,
  'active',
  JSON_ARRAY(
    'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?auto=format&fit=crop&w=1200&q=80'
  ),
  NOW(),
  NOW()
),
(
  @store_id,
  '海南玫珑瓜',
  '果肉细腻，香味足，适合家庭分食。到货后建议阴凉放置 1 天再切开。',
  'https://images.unsplash.com/photo-1571575173700-afb9492e6a50?auto=format&fit=crop&w=900&q=80',
  @fresh_fruit_category_id,
  4990,
  80,
  'active',
  JSON_ARRAY('https://images.unsplash.com/photo-1571575173700-afb9492e6a50?auto=format&fit=crop&w=1200&q=80'),
  NOW(),
  NOW()
),
(
  @store_id,
  '砀山梨',
  '清甜多汁，适合办公室和家庭常备。外皮有自然果点，不影响食用。',
  'https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&w=900&q=80',
  @fresh_fruit_category_id,
  3290,
  120,
  'active',
  JSON_ARRAY('https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&w=1200&q=80'),
  NOW(),
  NOW()
);

SET @peach_product_id = (SELECT id FROM products WHERE store_id = @store_id AND name = '阳山水蜜桃' LIMIT 1);
SET @melon_product_id = (SELECT id FROM products WHERE store_id = @store_id AND name = '海南玫珑瓜' LIMIT 1);
SET @pear_product_id = (SELECT id FROM products WHERE store_id = @store_id AND name = '砀山梨' LIMIT 1);

-- Main group-buy activity: the activity is the container; products live inside it.
INSERT INTO group_buys (
  store_id, leader_id, title, introduction, cover_image_url,
  group_type, delivery_type, shipping_time, start_time, end_time,
  visibility, status, gallery_image_urls, content_blocks, created_at, updated_at
)
VALUES (
  @store_id,
  @leader_id,
  '周末阳山水蜜桃社区团',
  '王姐本周从阳山果园集中收单，适合家庭囤货、办公室拼团和邻里群分享。',
  'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=1200&q=80',
  'normal',
  'local_delivery',
  DATE_ADD(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 2 HOUR),
  DATE_ADD(NOW(), INTERVAL 2 DAY),
  'public',
  'published',
  JSON_ARRAY(
    'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?auto=format&fit=crop&w=1200&q=80'
  ),
  JSON_ARRAY(
    JSON_OBJECT(
      'type', 'section',
      'title', '团长推荐',
      'text', '这次团不是长期货架，王姐按微信群订单量向果园集中订货，凑齐后统一配送到社区。'
    ),
    JSON_OBJECT(
      'type', 'paragraph',
      'text', '桃子是偏软甜口，适合现吃。介意轻微压痕的朋友谨慎下单，介意勿拍。'
    ),
    JSON_OBJECT(
      'type', 'list',
      'title', '适合谁买',
      'items', JSON_ARRAY('家庭囤货', '办公室拼团', '邻里群一起凑单')
    ),
    JSON_OBJECT(
      'type', 'deliveryNote',
      'title', '履约说明',
      'text', '7 月 5 日傍晚前后统一配送，桂花城支持送到楼下，其他小区按群内通知自提。'
    )
  ),
  NOW(),
  NOW()
);

SET @main_group_buy_id = LAST_INSERT_ID();

INSERT INTO group_buy_items (
  group_buy_id, product_id, display_name, group_price_amount,
  group_stock, sold_count, sort_order, show_stock, created_at, updated_at
)
VALUES
(@main_group_buy_id, @peach_product_id, '阳山水蜜桃 5 斤装', 2990, 100, 61, 1, TRUE, NOW(), NOW()),
(@main_group_buy_id, @peach_product_id, '阳山水蜜桃 10 斤家庭装', 5390, 60, 18, 2, TRUE, NOW(), NOW()),
(@main_group_buy_id, @melon_product_id, '海南玫珑瓜 1 个装', 3990, 40, 12, 3, TRUE, NOW(), NOW());

SET @main_item_id = (SELECT id FROM group_buy_items WHERE group_buy_id = @main_group_buy_id AND display_name = '阳山水蜜桃 5 斤装' LIMIT 1);

-- Secondary activity for list richness.
INSERT INTO group_buys (
  store_id, leader_id, title, introduction, cover_image_url,
  group_type, delivery_type, shipping_time, start_time, end_time,
  visibility, status, gallery_image_urls, content_blocks, created_at, updated_at
)
VALUES (
  @store_id,
  @leader_id,
  '本周砀山梨清甜团',
  '适合办公室和家庭常备，王姐按箱集中收单，减少损耗后给到活动价。',
  'https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&w=1200&q=80',
  'normal',
  'local_delivery',
  DATE_ADD(NOW(), INTERVAL 3 DAY),
  DATE_SUB(NOW(), INTERVAL 1 HOUR),
  DATE_ADD(NOW(), INTERVAL 3 DAY),
  'public',
  'published',
  JSON_ARRAY('https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&w=1200&q=80'),
  JSON_ARRAY(
    JSON_OBJECT('type', 'section', 'title', '本团亮点', 'text', '清甜多汁，适合日常水果补给。'),
    JSON_OBJECT('type', 'deliveryNote', 'title', '履约说明', 'text', '成团后次日统一送达社区自提点。')
  ),
  DATE_SUB(NOW(), INTERVAL 10 MINUTE),
  DATE_SUB(NOW(), INTERVAL 10 MINUTE)
);

SET @pear_group_buy_id = LAST_INSERT_ID();

INSERT INTO group_buy_items (
  group_buy_id, product_id, display_name, group_price_amount,
  group_stock, sold_count, sort_order, show_stock, created_at, updated_at
)
VALUES
(@pear_group_buy_id, @pear_product_id, '砀山梨 5 斤装', 2990, 80, 24, 1, TRUE, NOW(), NOW());

-- One historical sold-out activity for edge-state display.
INSERT INTO group_buys (
  store_id, leader_id, title, introduction, cover_image_url,
  group_type, delivery_type, shipping_time, start_time, end_time,
  visibility, status, gallery_image_urls, content_blocks, created_at, updated_at
)
VALUES (
  @store_id,
  @leader_id,
  '本周油桃加购团',
  '油桃少量到货，已售罄，仅保留给售罄状态和历史浏览使用。',
  'https://images.unsplash.com/photo-1629828874514-d09c46b26870?auto=format&fit=crop&w=1200&q=80',
  'normal',
  'local_delivery',
  DATE_ADD(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_ADD(NOW(), INTERVAL 1 DAY),
  'public',
  'published',
  JSON_ARRAY('https://images.unsplash.com/photo-1629828874514-d09c46b26870?auto=format&fit=crop&w=1200&q=80'),
  JSON_ARRAY(JSON_OBJECT('type', 'paragraph', 'text', '少量到货，适合尝鲜。')),
  DATE_SUB(NOW(), INTERVAL 20 MINUTE),
  DATE_SUB(NOW(), INTERVAL 20 MINUTE)
);

SET @sold_out_group_buy_id = LAST_INSERT_ID();

INSERT INTO group_buy_items (
  group_buy_id, product_id, display_name, group_price_amount,
  group_stock, sold_count, sort_order, show_stock, created_at, updated_at
)
VALUES
(@sold_out_group_buy_id, @peach_product_id, '当季油桃 3 斤尝鲜装', 1990, 0, 47, 1, TRUE, NOW(), NOW());

-- Buyer address, subscription, favorite, member relation and sample order.
INSERT INTO addresses (
  user_id, receiver_name, receiver_phone, province, city, district,
  detail, is_default, created_at, updated_at
)
VALUES (
  @buyer_user_id,
  '陈小满',
  '13900000000',
  '浙江省',
  '杭州市',
  '西湖区',
  '桂花城 3 幢 1 单元门口',
  TRUE,
  NOW(),
  NOW()
);

SET @address_id = LAST_INSERT_ID();

INSERT INTO subscriptions (user_id, leader_id, store_id, status, source, subscribed_at, created_at, updated_at)
VALUES (@buyer_user_id, @leader_id, @store_id, 'active', 'homepage', NOW(), NOW(), NOW());

INSERT INTO favorite_items (user_id, target_type, target_id, status, favorited_at, created_at, updated_at)
VALUES (@buyer_user_id, 'group_buy', @main_group_buy_id, 'active', NOW(), NOW(), NOW());

INSERT INTO member_relations (
  user_id, leader_id, store_id, level_name, growth_value,
  total_order_amount, total_orders, last_order_at, created_at, updated_at
)
VALUES (@buyer_user_id, @leader_id, @store_id, 'V1', 299, 2990, 1, NOW(), NOW(), NOW());

INSERT INTO orders (
  order_no, user_id, leader_id, store_id, group_buy_id, address_id,
  receiver_name, receiver_phone, province, city, district, detail, full_address,
  total_amount, discount_amount, pay_amount, pay_status, order_status,
  remark, paid_at, created_at, updated_at
)
VALUES (
  CONCAT('DEV', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')),
  @buyer_user_id,
  @leader_id,
  @store_id,
  @main_group_buy_id,
  @address_id,
  '陈小满',
  '13900000000',
  '浙江省',
  '杭州市',
  '西湖区',
  '桂花城 3 幢 1 单元门口',
  '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
  2990,
  0,
  2990,
  'paid',
  'paid',
  '请傍晚送到楼下门卫处',
  NOW(),
  NOW(),
  NOW()
);

SET @order_id = LAST_INSERT_ID();

INSERT INTO order_items (
  order_id, product_id, group_buy_item_id, product_name, sku_name,
  unit_price_amount, quantity, total_amount, created_at
)
VALUES (
  @order_id,
  @peach_product_id,
  @main_item_id,
  '阳山水蜜桃 5 斤装',
  '',
  2990,
  1,
  2990,
  NOW()
);

-- Store-level member rules and a coupon for P1 screens. Coupons remain optional.
INSERT INTO member_level_rules (store_id, level_name, min_growth_value, sort_order, created_at, updated_at)
VALUES
(@store_id, 'V0', 0, 1, NOW(), NOW()),
(@store_id, 'V1', 100, 2, NOW(), NOW()),
(@store_id, 'V2', 500, 3, NOW(), NOW());

INSERT INTO coupons (
  store_id, name, coupon_type, amount, threshold_amount, total_quantity,
  claimed_quantity, per_user_limit, start_time, end_time, status, created_at, updated_at
)
VALUES (
  @store_id,
  '社区团员 7 元满减券',
  'amount',
  700,
  2990,
  200,
  36,
  1,
  NOW(),
  DATE_ADD(NOW(), INTERVAL 7 DAY),
  'active',
  NOW(),
  NOW()
);

SELECT
  @main_group_buy_id AS mainGroupBuyId,
  @leader_id AS leaderId,
  @store_id AS storeId,
  @buyer_user_id AS buyerUserId,
  @order_id AS sampleOrderId;
