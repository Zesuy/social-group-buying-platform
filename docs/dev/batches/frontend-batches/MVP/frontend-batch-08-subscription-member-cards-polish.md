# Frontend Batch 08：我的订阅、会员卡与非 MVP 入口收口

## 1. 目标

完成私域关系展示链路：我的订阅、基础会员卡，并统一收口非 MVP 入口的灰态和占位行为。

## 2. 前置条件

Frontend Batch 07 已完成；公共浏览订阅操作、买家支付、团长发货链路可用；后端订阅列表和会员卡接口可用。

## 3. 涉及接口

- `GET /api/v1/my/subscriptions`
- `GET /api/v1/my/member-cards`
- `DELETE /api/v1/leaders/{leaderId}/subscription`

## 4. 页面范围

- `/subscriptions`
- `/member-cards`
- 个人中心、团长主页、一键开团、首页中的非 MVP 入口

页面参考 HTML：

- `docs/dev/batches/frontend-batches/demo/pages/subscriptions.html`
- `docs/dev/batches/frontend-batches/demo/pages/member-cards.html`
- `docs/dev/batches/frontend-batches/demo/pages/profile.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-home.html`
- `docs/dev/batches/frontend-batches/demo/pages/open-choose.html`
- `docs/dev/batches/frontend-batches/demo/pages/open-group.html`
- `docs/dev/batches/frontend-batches/demo/pages/messages.html`
- `docs/dev/batches/frontend-batches/demo/pages/messages-rich.html` 只作为消息空态/占位视觉参考，不新增通知或消息接口。
- `docs/dev/batches/frontend-batches/demo/pages/cart-sheet.html`、`docs/dev/batches/frontend-batches/demo/pages/sku-sheet.html`、`docs/dev/batches/frontend-batches/demo/pages/help-sell.html`、`docs/dev/batches/frontend-batches/demo/pages/privacy-settings.html` 只作为非 MVP 入口收口参考，必须灰态、占位或 toast。

## 5. 实现任务

实现我的订阅列表，展示团长 / 店铺卡片，支持进入团长主页和取消订阅；实现会员卡列表，展示店铺、团长、会员关系基础信息和空状态；封装 `SubscriptionCard`、`MemberCardItem`；从个人中心补齐会员卡、我的订阅入口；统一检查预售团购、卡券团购、报名团购、优惠券 / 红包、购物车、帮卖、积分商城、公众号关注、消息通知、客服、分享海报、售后等非 MVP 入口，确保只灰态、占位或 toast，不接真实逻辑和不存在接口。

## 6. 测试要求

组件测试覆盖订阅列表、取消订阅确认、会员卡列表态和空态；单元测试覆盖非 MVP 入口灰态配置；E2E 覆盖订阅 / 取消订阅、进入我的订阅、支付后查看会员卡基础展示、消息页空态和一键开团非 MVP 类型置灰。

## 7. 验收标准

用户可以查看自己的订阅和会员卡；取消订阅后列表刷新；会员卡只展示基础关系，不做等级升级、折扣或积分计算；全站非 MVP 入口均不会触发真实功能或后端未定义接口。

## 8. 联调文档更新项

更新 `docs/前后端联调文档.md` 中订阅列表、会员卡和非 MVP 入口检查项，补充页面入口、请求样例、成功判定、空状态和占位处理规则。

## 9. 禁止事项

不做积分商城；不计算折扣；不做等级升级；不做公众号真实推送；不做客服会话；不实现售后退款流程；不新增通知、消息或推荐接口。

## 10. 本批完成后必须输出的结果

- 已实现页面 / 能力清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
