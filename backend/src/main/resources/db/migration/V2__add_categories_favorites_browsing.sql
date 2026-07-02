-- ───────────────────────────────────────────────────────────────────
-- V2: Add product_categories, favorite_items, browsing_histories
-- ───────────────────────────────────────────────────────────────────
-- P1 Batch 01: 商品分类、搜索筛选、团购活动收藏、浏览历史

-- ── 1. product_categories ──────────────────────────────────────────
CREATE TABLE product_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    code        VARCHAR(64)  NOT NULL COMMENT '分类编码',
    parent_id   BIGINT       NULL COMMENT '父分类ID，本批初始化 parent_id=null (一级)',
    level       INT          NOT NULL DEFAULT 1 COMMENT '层级：1=一级',
    sort_order  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='商品分类表（平台固定分类）';

-- 初始化社区团购分类（一级）
INSERT INTO product_categories (name, code, parent_id, level, sort_order, status) VALUES
('生鲜水果', 'fresh_fruit', NULL, 1, 1, 'active'),
('蔬菜食品', 'vegetable_food', NULL, 1, 2, 'active'),
('肉禽蛋奶', 'meat_egg_dairy', NULL, 1, 3, 'active'),
('熟食烘焙', 'prepared_food', NULL, 1, 4, 'active'),
('日用百货', 'daily_goods', NULL, 1, 5, 'active'),
('其他', 'other', NULL, 1, 6, 'active');

-- ── 2. favorite_items ──────────────────────────────────────────────
CREATE TABLE favorite_items (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    target_type   VARCHAR(32)  NOT NULL COMMENT '收藏对象类型：group_buy',
    target_id     BIGINT       NOT NULL COMMENT '收藏对象ID（group_buy_id）',
    status        VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active / canceled',
    favorited_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at   DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_user_target UNIQUE (user_id, target_type, target_id)
) COMMENT='收藏表（本批只支持团购活动收藏）';

CREATE INDEX idx_favorite_items_user ON favorite_items (user_id, target_type, status, favorited_at);
CREATE INDEX idx_favorite_items_target ON favorite_items (target_type, target_id, status);

-- ── 3. browsing_histories ──────────────────────────────────────────
CREATE TABLE browsing_histories (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    target_type   VARCHAR(32)  NOT NULL COMMENT '浏览对象类型：group_buy',
    target_id     BIGINT       NOT NULL COMMENT '浏览对象ID（group_buy_id）',
    viewed_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_browsing_user_target UNIQUE (user_id, target_type, target_id)
) COMMENT='浏览历史表（本批只记录团购详情访问）';

CREATE INDEX idx_browsing_histories_user ON browsing_histories (user_id, target_type, viewed_at);

-- ── 4. 补索引（products 已有 category_id 列）─────────────────────
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_store_status_category ON products (store_id, status, category_id);
