# P1 Batch 5：售后退款和交易链路回归

## 1. 目标

补齐售后退款闭环，并完成交易相关后端接口、状态、文档和测试回归，为后续轻量浏览增强能力提供稳定基础。

## 2. 前置条件

P1 Batch 1 到 Batch 4 均已完成；订单、支付、发货、优惠券、会员成长和购物车链路稳定。

## 3. 涉及接口

`POST /api/v1/orders/{orderId}/after-sales`；`GET /api/v1/my/after-sales`；`GET /api/v1/my/after-sales/{afterSaleId}`；`GET /api/v1/my/store/after-sales`；`GET /api/v1/my/store/after-sales/{afterSaleId}`；`POST /api/v1/my/store/after-sales/{afterSaleId}/approve`；`POST /api/v1/my/store/after-sales/{afterSaleId}/reject`；`POST /api/v1/my/store/after-sales/{afterSaleId}/complete-refund`；增强 `GET /api/v1/orders/my`；增强 `GET /api/v1/orders/{orderId}`；增强 `GET /api/v1/my/store/orders`。

## 4. 涉及数据表

新增 `after_sales`，复用 `orders`、`order_items`、`group_buy_items`、`user_coupons`、`member_relations`。如需记录售后操作轨迹，可新增 `after_sale_logs`。

## 5. 实现任务

买家可对已支付、已发货或已完成订单发起售后申请；同一订单同一时间只能存在一个进行中的售后单；团长只能处理自己店铺订单的售后；审核通过后进入待退款，拒绝后恢复订单可展示状态；完成退款使用模拟退款，不接真实支付渠道回调；退款完成后订单进入 `refunded` 或明确的已退款展示状态，支付状态变为 `refunded`，售后状态变为 `completed`；按业务规则冲正会员累计消费和成长值，优惠券默认不退回，除非 API 文档明确配置；库存是否返还必须在本批文档中固定口径并覆盖测试。

## 6. 测试要求

MockMvc 覆盖提交售后、重复提交、买家列表、团长列表、审核通过、审核拒绝、完成模拟退款、未登录、非订单用户、非店铺团长、跨店铺拒绝；Service 覆盖售后状态流转、订单状态回写、支付状态回写、会员统计冲正、优惠券处理口径、库存返还口径、重复退款幂等和并发处理。

## 7. 验收标准

售后申请、审核、模拟退款形成完整闭环；订单列表和详情能展示售后 / 已退款状态；退款不会重复执行；交易相关 P1 接口与文档一致，MVP 主链路仍可回归通过。

## 8. 联调文档更新项

新增“售后申请”“团长售后处理”“模拟退款”“交易链路回归”链路；更新订单状态、支付状态、优惠券售后口径、会员成长冲正口径、库存返还口径和测试数据准备方式。

## 9. 禁止事项

不接真实支付退款、不实现退货物流、不实现平台客服仲裁、不实现复杂部分退款或多售后单并行；不把售后问题留到 P2；不破坏 MVP 订单、支付、发货和会员卡接口兼容性。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
