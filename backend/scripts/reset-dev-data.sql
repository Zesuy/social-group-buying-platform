-- Reset local development business data.
--
-- Usage:
--   docker exec -i groupshop-mysql mysql -uroot -proot groupshop < backend/scripts/reset-dev-data.sql
--
-- This keeps Flyway history and product_categories, then clears all user-
-- generated business data so API seed scripts can rebuild a clean demo set.
-- Do not run this against shared or production databases.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM chat_messages;
DELETE FROM chat_conversations;
DELETE FROM notifications;
DELETE FROM upload_asset_references;
DELETE FROM upload_assets;
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

ALTER TABLE chat_messages AUTO_INCREMENT = 1;
ALTER TABLE chat_conversations AUTO_INCREMENT = 1;
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
