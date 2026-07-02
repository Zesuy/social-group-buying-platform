# P1 Batch 1：商品库、分类搜索、收藏浏览

## 1. 目标

补齐团长商品库复用效率和买家发现能力。团长可按分类、关键词、状态管理商品；买家可在首页商品流搜索 / 分类筛选，并可收藏团购或商品、查看浏览历史。

## 2. 前置条件

MVP 后端已完成；商品、团购、公共浏览、当前用户登录态和团长店铺权限均可用。

## 3. 涉及接口

`GET /api/v1/categories`；`GET /api/v1/group-buys?keyword=&categoryId=&page=&pageSize=`；`GET /api/v1/my/store/products?keyword=&categoryId=&status=&page=&pageSize=`；`GET /api/v1/my/store/products/{productId}/usages`；`POST /api/v1/group-buys/{groupBuyId}/favorite`；`DELETE /api/v1/group-buys/{groupBuyId}/favorite`；`GET /api/v1/my/favorites`；`GET /api/v1/my/browsing-history`；`DELETE /api/v1/my/browsing-history/{historyId}`。

## 4. 涉及数据表

`products`、`group_buys`、`group_buy_items`、新增 `product_categories`、新增 `favorite_items`、新增 `browsing_histories`。

## 5. 实现任务

补充商品分类数据结构和初始化数据；允许商品绑定 `categoryId` 并支持商品库筛选；公共团购列表支持关键词和分类筛选，关键词覆盖团购标题、团购介绍、团购商品展示名和商品名称；团购详情访问后为已登录用户记录浏览历史；实现收藏 / 取消收藏和我的收藏列表；商品被团购引用时提供使用情况查询，删除商品仍沿用 MVP 软删除规则且不得影响历史订单快照。

## 6. 测试要求

MockMvc 覆盖分类列表、商品库筛选、公共搜索、空结果、收藏、取消收藏、重复收藏、未登录收藏拒绝、浏览历史记录和删除；Service 覆盖关键词筛选、分类筛选、商品归属、收藏唯一约束、浏览历史去重或最近访问更新策略。

## 7. 验收标准

首页商品流可按关键词和分类返回稳定分页结果；团长商品库可作为后续发布团购的选择来源；用户收藏和浏览历史只返回自己的数据；未登录用户浏览不写入个人历史。

## 8. 联调文档更新项

新增“商品搜索与分类筛选”“收藏团购”“浏览历史”链路；更新“商品管理”和“公共浏览团购”链路的筛选参数、响应样例和空状态。

## 9. 禁止事项

不实现平台商品审核、复杂 SKU、商品评价、推荐算法、跨店铺商品复制；不改变 MVP 下单价格仍以 `group_buy_items.group_price_amount` 为准的口径；不跳过测试；不把已知问题留到下一批。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。

