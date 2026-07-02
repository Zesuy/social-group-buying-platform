# P1 Batch 3：购物车和订单幂等

## 1. 目标

实现买家购物车，并为创建订单、支付、取消、确认收货、团长发货等关键交易动作补充 `Idempotency-Key` 幂等能力。

## 2. 前置条件

P1 Batch 2 已完成；公共团购详情、订单预览、创建订单、模拟支付、取消、确认收货和发货接口可用。

## 3. 涉及接口

`GET /api/v1/cart/items`；`POST /api/v1/cart/items`；`PATCH /api/v1/cart/items/{cartItemId}`；`DELETE /api/v1/cart/items/{cartItemId}`；`DELETE /api/v1/cart/items`；`POST /api/v1/cart/checkout-preview`；增强 `POST /api/v1/orders/preview`；增强 `POST /api/v1/orders`；增强 `POST /api/v1/orders/{orderId}/simulate-pay`；增强 `POST /api/v1/orders/{orderId}/cancel`；增强 `POST /api/v1/orders/{orderId}/complete`；增强 `POST /api/v1/my/store/orders/{orderId}/ship`。

## 4. 涉及数据表

新增 `carts`、新增 `idempotency_keys`，复用 `orders`、`order_items`、`group_buy_items`、`shipments`。

## 5. 实现任务

实现购物车项新增、数量修改、删除、清空、列表；购物车项必须绑定当前用户和有效团购商品；购物车结算预览只允许同一团购或同一结算约束下的商品进入一次订单，具体规则需写入 API 文档；订单预览和创建订单支持从 `cartItemIds` 生成明细；订单创建成功后清理已结算购物车项；为交易动作实现 `Idempotency-Key`，同一用户、同一接口、同一 key 的重复请求必须返回首次处理结果或稳定错误，不得重复创建订单、重复扣库存、重复发货。

## 6. 测试要求

MockMvc 覆盖购物车增删改查、库存不足、团购结束、跨用户购物车项拒绝、购物车预览、购物车创建订单、未登录拒绝、重复 `Idempotency-Key`；Service 覆盖购物车归属、结算清理、订单创建幂等、支付幂等、取消幂等、确认收货幂等、发货幂等和并发请求。

## 7. 验收标准

买家可把团购商品加入购物车并从购物车进入订单预览和创建订单；重复提交订单不会产生多笔订单；重复支付不会重复扣库存；重复发货不会生成多条有效发货记录。

## 8. 联调文档更新项

新增“购物车管理”“购物车下单”“Idempotency-Key 使用约定”链路；更新订单预览、创建订单、模拟支付、取消、确认收货、团长发货的请求头、重复提交响应样例和错误码。

## 9. 禁止事项

不实现跨店铺合并结算、购物车营销凑单、购物车推荐、真实支付幂等回调；不改变订单商品快照口径；不允许用前端防重复替代后端事务和幂等保护。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。

