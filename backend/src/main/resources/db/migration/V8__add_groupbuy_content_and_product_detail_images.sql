-- Add content fields to group_buys: multi-image gallery and structured content blocks
-- Add detail image urls to products
ALTER TABLE group_buys ADD gallery_image_urls TEXT NULL COMMENT '团购活动多图，TEXT 存 JSON 数组，最多 9 张；Java 层校验';
ALTER TABLE group_buys ADD content_blocks TEXT NULL COMMENT '团购活动结构化正文块，TEXT 存 JSON；最多 20 块；Java 层校验';
ALTER TABLE products ADD detail_image_urls TEXT NULL COMMENT '商品详情图片，TEXT 存 JSON 数组，最多 9 张；Java 层校验';
