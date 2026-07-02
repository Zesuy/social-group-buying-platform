# P1 Batch 1：商品库、分类搜索、收藏浏览

## 1. 目标

补齐团长商品库复用效率和买家发现能力。团长可按分类、关键词、状态管理商品；买家可在首页商品流搜索 / 分类筛选，并可收藏团购或商品、查看浏览历史。

## 2. 前置条件

MVP 后端已完成；商品、团购、公共浏览、当前用户登录态和团长店铺权限均可用。

## 3. 涉及接口

公开接口（无需登录）：
- `GET /api/v1/categories` — 获取分类列表
- `GET /api/v1/group-buys?keyword=&categoryId=&page=&pageSize=` — 公共团购列表（支持关键词和分类筛选，固定返回 published+public，不支持 status 参数）

需登录接口（买家）：
- `GET /api/v1/group-buys/{groupBuyId}` — 团购详情（已登录用户返回 favorited 状态）
- `POST /api/v1/group-buys/{groupBuyId}/favorite` — 收藏团购
- `DELETE /api/v1/group-buys/{groupBuyId}/favorite` — 取消收藏
- `GET /api/v1/my/favorites?page=&pageSize=` — 我的收藏列表
- `GET /api/v1/my/browsing-histories?page=&pageSize=` — 我的浏览历史
- `DELETE /api/v1/my/browsing-histories/{historyId}` — 删除单条浏览历史

需登录接口（团长）：
- `GET /api/v1/my/store/products?keyword=&categoryId=&status=&page=&pageSize=` — 商品库列表（支持关键词、分类、状态筛选）
- `GET /api/v1/my/store/products/{productId}/usages?page=&pageSize=` — 商品使用情况（被哪些团购引用）

## 4. 涉及数据表

`products`、`group_buys`、`group_buy_items`、新增 `product_categories`、新增 `favorite_items`、新增 `browsing_histories`。

## 5. 实现任务

### 5.1 商品分类（product_categories）

- Flyway V2 迁移创建 `product_categories` 表，并初始化 6 个一级种子分类：生鲜水果、蔬菜食品、肉禽蛋奶、熟食烘焙、日用百货、其他。
- `ProductCategory` 实体，`CategoryController` + `CategoryService` 提供公开 `GET /api/v1/categories` 接口。
- 分类为平台固定一级分类（`level=1`, `parent_id=null`），本批不实现二级分类管理。

### 5.2 商品绑定分类

- `Product` 实体新增 `categoryId` 字段关联 `product_categories.id`。
- `CreateProductRequest` 中 `categoryId` 为必填（standalone 创建），内联创建商品时为可选（发布团购时自动创建商品可不传分类）。
- `UpdateProductRequest` 支持更新 `categoryId`。
- 商品列表 `GET /api/v1/my/store/products` 新增 `keyword`、`categoryId`、`status` 查询参数。

### 5.3 公共搜索与分类筛选

- `GET /api/v1/group-buys` 新增 `keyword` 和 `categoryId` 查询参数。
- `keyword` 匹配范围：团购标题（`title`）、团购介绍（`introduction`）、团购商品展示名（`display_name`）、商品名称（`product.name`）。
- `categoryId` 通过 `products` → `group_buy_items` 链过滤：只返回团购商品至少一个属于该分类的团购。
- 移除了原有的 `status` 参数（公共列表始终只返回 `published` 状态团购）。

### 5.4 浏览历史（browsing_histories）

- 未登录用户访问团购详情不记录浏览历史。
- 已登录用户访问 `GET /api/v1/group-buys/{groupBuyId}` 时自动记录。
- 采用 upsert 策略：同一用户 + 同一目标类型 + 同一目标 ID 重复访问时，更新 `viewed_at` 为最新时间。
- 本批只记录 `target_type = "group_buy"`。
- `GET /api/v1/my/browsing-histories` 返回分页列表，按 `viewed_at` 降序排列。
- `DELETE /api/v1/my/browsing-histories/{historyId}` 删除单条记录，只能删除自己的。

### 5.5 收藏团购（favorite_items）

- 收藏仅针对团购活动（`target_type = "group_buy"`），不支持商品收藏。
- `favorite_items` 表使用唯一约束 `(user_id, target_type, target_id)`。
- `POST /api/v1/group-buys/{groupBuyId}/favorite` 收藏：如果已有 `canceled` 记录，重新激活为 `active`。
- `DELETE /api/v1/group-buys/{groupBuyId}/favorite` 取消收藏：更新 `status = canceled`，保留历史。
- `GET /api/v1/my/favorites` 返回当前用户活跃收藏列表，包含团购摘要信息。
- 团购详情和团长主页的 `viewer.favorited` 字段标识当前用户是否已收藏。

### 5.6 商品使用情况

- `GET /api/v1/my/store/products/{productId}/usages` 返回引用该商品的团购列表（团购活动及其团购商品信息）。
- 包括团购标题、状态、团购价格、库存、销量等。
- 仅返回当前店铺范围内的引用，不含已 `deleted` 的商品。

## 6. 测试要求

MockMvc 覆盖分类列表、商品库筛选、公共搜索、空结果、收藏、取消收藏、重复收藏、未登录收藏拒绝、浏览历史记录和删除；Service 覆盖关键词筛选、分类筛选、商品归属、收藏唯一约束、浏览历史去重或最近访问更新策略。

## 7. 验收标准

首页商品流可按关键词和分类返回稳定分页结果；团长商品库可作为后续发布团购的选择来源；用户收藏和浏览历史只返回自己的数据；未登录用户浏览不写入个人历史。

## 8. 联调文档更新项

新增“商品搜索与分类筛选”“收藏团购”“浏览历史”链路；更新“商品管理”和“公共浏览团购”链路的筛选参数、响应样例和空状态。

## 9. 禁止事项

不实现平台商品审核、复杂 SKU、商品评价、推荐算法、跨店铺商品复制；不改变 MVP 下单价格仍以 `group_buy_items.group_price_amount` 为准的口径；不跳过测试；不把已知问题留到下一批。

## 10. 本批完成后必须输出的结果

### 已实现接口清单

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| Category | GET | `/api/v1/categories` | 获取分类列表（公开） |
| PublicBrowsing | GET | `/api/v1/group-buys` | 新增 keyword, categoryId 参数 |
| PublicBrowsing | GET | `/api/v1/group-buys/{groupBuyId}` | 新增 favorited 字段到 viewer |
| Favorite | POST | `/api/v1/group-buys/{groupBuyId}/favorite` | 收藏团购 |
| Favorite | DELETE | `/api/v1/group-buys/{groupBuyId}/favorite` | 取消收藏 |
| Favorite | GET | `/api/v1/my/favorites` | 我的收藏列表 |
| BrowsingHistory | GET | `/api/v1/my/browsing-histories` | 我的浏览历史 |
| BrowsingHistory | DELETE | `/api/v1/my/browsing-histories/{historyId}` | 删除浏览历史 |
| Product | GET | `/api/v1/my/store/products` | 新增 keyword, categoryId, status 参数 |
| Product | POST | `/api/v1/my/store/products` | 新增 categoryId 字段 |
| Product | PATCH | `/api/v1/my/store/products/{productId}` | 新增 categoryId 字段 |
| Product | GET | `/api/v1/my/store/products/{productId}/usages` | 商品使用情况 |

### 已新增 / 修改文件清单

新增文件：
- `src/main/java/.../browsing/controller/BrowsingHistoryController.java`
- `src/main/java/.../browsing/dto/BrowsingHistoryResponse.java`
- `src/main/java/.../browsing/service/BrowsingHistoryService.java`
- `src/main/java/.../category/controller/CategoryController.java`
- `src/main/java/.../category/dto/CategoryResponse.java`
- `src/main/java/.../category/service/CategoryService.java`
- `src/main/java/.../favorite/controller/FavoriteController.java`
- `src/main/java/.../favorite/dto/FavoriteResponse.java`
- `src/main/java/.../favorite/service/FavoriteService.java`
- `src/main/java/.../model/entity/ProductCategory.java`
- `src/main/java/.../model/entity/FavoriteItem.java`
- `src/main/java/.../model/entity/BrowsingHistory.java`
- `src/main/java/.../model/mapper/ProductCategoryMapper.java`
- `src/main/java/.../model/mapper/FavoriteItemMapper.java`
- `src/main/java/.../model/mapper/BrowsingHistoryMapper.java`
- `src/main/java/.../product/dto/ProductUsageResponse.java`

修改文件：
- `src/main/java/.../model/entity/Product.java`（新增 categoryId）
- `src/main/java/.../product/dto/CreateProductRequest.java`（新增 categoryId）
- `src/main/java/.../product/dto/UpdateProductRequest.java`（新增 categoryId）
- `src/main/java/.../product/dto/ProductResponse.java`（新增 categoryId）
- `src/main/java/.../product/service/ProductService.java`（新增 keyword/categoryId/status 筛选、getProductUsages）
- `src/main/java/.../product/controller/ProductController.java`（新增 usages 端点、查询参数）
- `src/main/java/.../publicbrowsing/PublicBrowsingController.java`（新增 keyword/categoryId params、favorited）
- `src/main/java/.../publicbrowsing/dto/ViewerInfo.java`（新增 favorited）
- `src/main/java/.../groupbuy/service/GroupBuyService.java`（新增 getPublicGroupBuys with keyword/categoryId, 收藏状态, 浏览历史记录）

### 已新增 / 修改 Flyway 迁移清单

`V2__add_categories_favorites_browsing.sql`

创建表：`product_categories`、`favorite_items`、`browsing_histories`
新增索引：`idx_products_category`、`idx_products_store_status_category`
初始化 6 个种子分类数据。

### 已新增 / 修改测试清单

- `category/CategoryControllerTest.java` — MockMvc 测试分类列表
- `category/CategoryServiceTest.java` — Service 测试分类查询
- `favorite/FavoriteControllerTest.java` — MockMvc 测试收藏/取消收藏/列表/未登录拒绝
- `favorite/FavoriteServiceTest.java` — Service 测试收藏唯一约束、重新激活
- `browsing/BrowsingHistoryControllerTest.java` — MockMvc 测试浏览历史列表/删除
- `browsing/BrowsingHistoryServiceTest.java` — Service 测试 upsert、去重
- `publicbrowsing/PublicBrowsingControllerTest.java` — 更新：测试 keyword/categoryId 筛选
- `product/ProductControllerTest.java` — 更新：测试 keyword/categoryId 筛选、分类验证、usages
- `product/ProductServiceTest.java` — 更新：测试关键词筛选、分类筛选、商品使用情况

### 已更新联调文档的位置

`docs/前后端联调文档.md` — 新增”商品分类与筛选”、”收藏团购活动”、”浏览历史”、”商品引用情况”链路，更新现有链路筛选参数。

### 测试运行命令和结果

```bash
cd backend
mvn test
# 291 tests, 0 failures, 0 errors
```

### 本批未解决问题

无。

