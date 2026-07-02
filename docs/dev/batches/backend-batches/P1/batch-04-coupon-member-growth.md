# P1 Batch 4：优惠券 / 红包和会员成长

## 1. 目标

实现轻量营销能力和会员成长规则。团长可创建店铺券，用户可领取并在订单预览 / 创建订单中使用；支付成功后按规则累计会员成长值和等级展示。

## 2. 前置条件

P1 Batch 3 已完成；订单预览、创建订单、模拟支付、购物车结算和幂等能力稳定。

## 3. 涉及接口

`GET /api/v1/my/store/coupons`；`POST /api/v1/my/store/coupons`；`PATCH /api/v1/my/store/coupons/{couponId}`；`POST /api/v1/my/store/coupons/{couponId}/disable`；`GET /api/v1/group-buys/{groupBuyId}/coupons`；`POST /api/v1/coupons/{couponId}/claim`；`GET /api/v1/my/coupons?status=`；增强 `POST /api/v1/orders/preview`；增强 `POST /api/v1/orders`；`GET /api/v1/my/store/member-level-rules`；`PUT /api/v1/my/store/member-level-rules`；增强 `GET /api/v1/my/member-cards`。

## 4. 涉及数据表

新增 `coupons`、新增 `user_coupons`、新增 `member_level_rules`，复用 `orders`、`order_items`、`member_relations`。

## 5. 实现任务

实现店铺优惠券创建、编辑、停用、列表；优惠券支持满减券和红包券两类，其中红包券仅作为订单抵扣券，不涉及现金红包、提现或真实资金；用户可领取可用券，重复领取按配置限制；订单预览返回可用券、不可用原因和抵扣后金额；创建订单时锁定并记录使用的用户券，支付成功后标记已使用，取消未支付订单后释放用户券；支付成功后按会员规则累计成长值、消费金额、订单数并返回会员等级展示；售后退款对会员统计的冲正留到 Batch 5。

## 6. 测试要求

MockMvc 覆盖优惠券创建、编辑、停用、领取、重复领取、我的券列表、订单预览抵扣、创建订单用券、未登录、非团长、跨店铺拒绝；Service 覆盖有效期、门槛、库存或领取限制、用券事务、取消订单释放券、支付成功标记已使用、会员成长累计和等级匹配。

## 7. 验收标准

优惠券只影响 `discountAmount` 和 `payAmount`，金额均为整数分；不满足门槛或不属于当前店铺 / 团购的券不能使用；支付成功后用户券状态、订单金额、会员成长数据保持一致。

## 8. 联调文档更新项

新增“优惠券管理”“领券”“下单用券”“会员成长规则”链路；更新订单预览、创建订单、会员卡展示的请求样例、响应样例、常见错误码和测试数据准备方式。

## 9. 禁止事项

不实现平台券跨店铺结算、真实现金红包、会员折扣自动改价、积分账户、积分商城、复杂促销叠加；不允许优惠券抵扣后 `payAmount` 小于 0；不允许绕过订单事务更新券状态。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。

