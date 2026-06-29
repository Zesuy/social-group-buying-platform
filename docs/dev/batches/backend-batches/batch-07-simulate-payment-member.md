# Batch 7：模拟支付，状态校验、扣 groupStock、增加 soldCount、会员关系创建/更新

## 1. 目标

完成模拟支付和交易核心状态流转。

## 2. 前置条件

Batch 6 已完成；系统可创建 `pendingPay + unpaid` 订单。

## 3. 涉及接口

`POST /api/v1/orders/{orderId}/simulate-pay`。

## 4. 涉及数据表

`orders`、`order_items`、`group_buy_items`、`member_relations`。

## 5. 实现任务

仅允许 `pendingPay + unpaid` 订单支付；支付成功后更新 `payStatus=paid`、`orderStatus=paid`、`paidAt`；扣减 `group_buy_items.group_stock`，增加 `sold_count`；创建或更新会员关系；全部在同一事务中完成。

## 6. 测试要求

MockMvc 覆盖支付成功、重复支付、非本人订单、订单不可支付、库存不足；Service 覆盖事务回滚、库存扣减、销量增加、会员累计金额和累计订单。

## 7. 验收标准

重复支付不能重复扣库存或重复增加销量；库存不足时订单保持未支付。

## 8. 联调文档更新项

完成“模拟支付”链路，补充成功后订单状态、库存变化和会员卡数据变化。

## 9. 禁止事项

不实现非本批接口；不接入真实微信支付、支付回调、对账、退款；不扣减 `products.stock`；不扩大 MVP 范围；不跳过测试；不把已知问题留到下一批；不私自修改 API 字段、状态枚举、错误码、金额口径；需要修改上游文档时，必须明确说明原因。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
