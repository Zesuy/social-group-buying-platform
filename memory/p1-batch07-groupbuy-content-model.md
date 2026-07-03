---
name: p1-batch07-groupbuy-content-model
description: P1 Batch 07 - GroupBuy activity content model refactoring (content blocks, gallery, product detail images, featuredItem)
metadata:
  type: project
---

P1 Batch 07 refactored the backend model so users enter a group-buy **activity** (with its own title, gallery, structured content blocks) rather than a product detail page.

**Key changes:**
- `group_buys` got `gallery_image_urls` (TEXT, JSON array) and `content_blocks` (TEXT, JSON array with type-safe blocks)
- `products` got `detail_image_urls` (TEXT, JSON array)
- `GroupBuyDetailResponse` now has `featuredItem` (soldCount desc → sortOrder asc → id asc) and `items[].product` (nested ProductSummaryData)
- `contentBlocks` support 5 types: paragraph/section/image/list/deliveryNote — validated by `ContentValidationUtil`
- `ContentValidationUtil` centralizes JSON serialization, URL validation, HTML safety, field constraints
- PATCH semantics: null = skip, [] = clear, array = replace
- Copy group buy copies gallery+content blocks too

**Why:** Fix "group buy detail = product detail" modeling bug. Activity content stays with the activity, product content stays with the product. A product reused across group buys doesn't carry marketing copy.

**How to apply:** Flyway V8 migration creates the columns. The `toGroupBuyData()` converter automatically deserializes old null data to empty `[]`. Content block validation rejects HTML/script/iframe/style/javascript: and enforces field limits.

**Related:** [[product-entity-categoryId]]
