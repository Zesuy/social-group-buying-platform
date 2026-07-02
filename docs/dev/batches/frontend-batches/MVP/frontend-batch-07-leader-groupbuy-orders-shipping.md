# Frontend Batch 07：团购管理、团长订单与发货

## 1. 目标

完成团长发布后的经营管理链路：查看和维护自己的团购、查看店铺订单、对已支付订单发货。

## 2. 前置条件

Frontend Batch 06 已完成；团长可以发布普通团购；买家订单和模拟支付链路可用；后端团购管理和团长订单接口可用。

## 3. 涉及接口

- `GET /api/v1/my/store/group-buys?status=published|ended`
- `GET /api/v1/my/store/group-buys/{groupBuyId}`
- `PATCH /api/v1/my/store/group-buys/{groupBuyId}`
- `POST /api/v1/my/store/group-buys/{groupBuyId}/end`
- `GET /api/v1/my/store/orders?status=xxx&page=1&pageSize=20`
- `GET /api/v1/my/store/orders/{orderId}`
- `POST /api/v1/my/store/orders/{orderId}/ship`

## 4. 页面范围

- `/leader/group-buys`
- `/leader/group-buys/:id`
- `/leader/orders`
- `/leader/orders/:id`

页面参考 HTML：

- `docs/dev/batches/frontend-batches/demo/pages/leader-groups.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-group-detail.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-orders.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-order-detail.html`
- `docs/dev/batches/frontend-batches/demo/pages/orders-states.html` 只参考订单状态文案、步骤条和按钮状态，不扩展售后退款流程。
- `docs/dev/batches/frontend-batches/demo/pages/message-detail.html` 只参考发货后关联订单/物流信息的卡片表现，不新增消息详情接口。

## 5. 实现任务

实现我的团购列表，包含进行中、已结束 Tab、团购卡片、分页加载、空状态和错误重试；实现我的团购详情，展示基础信息、商品信息、时间和状态，允许编辑基础信息并支持确认结束团购；已产生订单后修改价格等后端业务错误要清晰提示；实现团长订单列表，按状态筛选并展示订单号、买家、商品摘要、金额和状态；实现团长订单详情，展示买家地址快照、商品快照、支付状态和发货信息；仅 `orderStatus=paid` 时展示 `ShipmentForm`，填写配送方式、物流公司、物流单号后调用发货接口并刷新状态。

## 6. 测试要求

组件测试覆盖团购列表进行中 / 已结束状态、结束团购确认弹窗、团长订单不同状态按钮、发货表单校验；单元测试覆盖团购状态和团长订单状态映射；E2E 覆盖团长查看已发布团购、结束团购、查看已支付订单并发货；异常路径覆盖重复发货、非可发货状态、非自己店铺订单和跨店铺访问错误提示。

## 7. 验收标准

团长可以管理自己店铺的团购和订单；已支付订单可以发货，发货后状态刷新为已发货；不可发货订单不展示发货表单；权限、状态和重复操作错误均能明确提示。

## 8. 联调文档更新项

更新 `docs/前后端联调文档.md` 中我的团购管理、团长订单列表、团长订单详情和发货链路，补充前端页面入口、状态筛选、调用顺序、成功判定和常见错误码。

## 9. 禁止事项

不建设平台后台；不实现复杂数据看板；不做导出订单；不绕过 `/my/store/**` 权限模型；不允许非 paid 状态订单进入真实发货请求；不吞掉后端业务规则错误。

## 10. 本批完成后必须输出的结果

- 已实现页面 / 能力清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
