# Frontend Batch 05.8：移动端设计基线与已实现页面视觉统一

## 1. 目标

在 Frontend Batch 05.5 真实前后端联调完成后、Frontend Batch 06 商品管理与发布团购开始前，先完成移动端设计风格统一。

本批只做 Design Foundation 和已实现页面视觉改造，不新增业务能力、不新增后端接口调用、不提前实现商品管理、发布团购、团长订单发货、订阅列表或会员卡页面。

设计方向参考：

- Starbucks：温暖奶油色背景、绿色零售 CTA、50px 胶囊主按钮、深绿色重点功能区、56px 浮动入口。
- Airbnb：移动 marketplace 的 pill 搜索栏、圆角商品卡、photo-first 商品展示、信任徽章、轻量卡片信息流。

## 2. 前置条件

- Frontend Batch 05.5 已完成；Batch 01-05 页面链路已可连接真实后端运行。
- `docs/dev/batches/frontend-batches/demo/lintuan-mobile-pages-demo-clean.html` 已作为页面内容和控件参考。
- 单页 HTML 参考已拆分到 `docs/dev/batches/frontend-batches/demo/pages/`，本批优先查看与页面范围匹配的单页，而不是直接通读完整 demo。
- 非 MVP 功能边界仍以 `AGENTS.md`、`docs/功能需求定义.md`、`docs/页面与交互文档.md` 和 `docs/前后端联调文档.md` 为准。

## 3. 页面范围

本批只覆盖已实现或已存在的页面：

- `/login`
- `/`
- `/group-buys/:id`
- `/leaders/:id`
- `/checkout`
- `/addresses`
- `/addresses/new`
- `/addresses/:id/edit`
- `/orders`
- `/orders/:id`
- `/profile`
- `/open-group`
- `/messages`
- `/store/create`
- `/leader/store`

页面参考 HTML：

- `docs/dev/batches/frontend-batches/demo/pages/login.html`
- `docs/dev/batches/frontend-batches/demo/pages/home.html`
- `docs/dev/batches/frontend-batches/demo/pages/group-detail.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-home.html`
- `docs/dev/batches/frontend-batches/demo/pages/checkout.html`
- `docs/dev/batches/frontend-batches/demo/pages/addresses.html`
- `docs/dev/batches/frontend-batches/demo/pages/address-new.html`
- `docs/dev/batches/frontend-batches/demo/pages/address-edit.html`
- `docs/dev/batches/frontend-batches/demo/pages/orders.html`
- `docs/dev/batches/frontend-batches/demo/pages/order-detail.html`
- `docs/dev/batches/frontend-batches/demo/pages/profile.html`
- `docs/dev/batches/frontend-batches/demo/pages/open-group.html`
- `docs/dev/batches/frontend-batches/demo/pages/messages.html`
- `docs/dev/batches/frontend-batches/demo/pages/store-create.html`
- `docs/dev/batches/frontend-batches/demo/pages/leader-store.html`
- `docs/dev/batches/frontend-batches/demo/pages/orders-states.html`
- `docs/dev/batches/frontend-batches/demo/pages/messages-rich.html` 只作为消息占位视觉参考，不实现消息接口。
- `docs/dev/batches/frontend-batches/demo/pages/cart-sheet.html` 和 `docs/dev/batches/frontend-batches/demo/pages/sku-sheet.html` 只作为非 MVP 浮层/灰态控件参考，不实现购物车、SKU 加购或购物车结算。

以下页面只允许保持占位，不在本批实现：

- `/leader/products`
- `/leader/group-buys`
- `/leader/orders`
- `/subscriptions`
- `/member-cards`

## 4. 涉及接口

本批不新增接口。

允许继续使用已实现页面原本已有的接口，例如登录、当前用户、公开团购、团长主页、地址、订单、模拟支付、创建店铺和我的店铺接口。

禁止为了视觉 demo 增加以下接口或假装已有能力：

- 购物车接口
- 优惠券 / 红包接口
- 完整售后 / 退款接口
- 帮卖分销接口
- 公众号推送 / 消息通知接口
- 商品库分类、批量导入或素材库接口
- 平台后台接口

## 5. 实现任务

### 5.1 设计基线

统一全局 design tokens，建议集中在 `frontend/src/styles/variables.css`、`frontend/src/styles/global.css` 和 `frontend/src/styles/vant-theme.css`：

- 背景：温暖奶油色页面背景，避免纯白页面堆叠。
- 主色：沿用微信绿色作为零售 CTA 主色。
- 深色功能区：提供深绿色 surface token，用于首页品牌区、个人中心重点入口或关键功能区。
- 价格色：保持清晰的橙红价格色，严禁把金额弱化为普通正文。
- 圆角：普通卡片 8px；商品图和大型容器可使用 12px；主 CTA 使用 50px 胶囊。
- 阴影：轻量卡片阴影，不使用重阴影和玻璃拟态。
- 间距：移动端 4 / 8 / 12 / 16 / 24 的节奏。
- 触控：所有主要按钮、图标按钮、表单控件可点击区域不小于 44px。

### 5.2 基础组件统一

改造已有组件风格，不改变业务语义：

- `PageLayout`：统一页面背景、内容内边距、底部 Tab 和 fixed action bar 的安全区预留。
- `BottomTabBar`：保持 5 个主 Tab，视觉上更接近移动 marketplace 底部导航。
- `ReminderBanner`：统一为轻量提示条；公众号推送相关文案只能作为占位提醒，不接真实推送。
- `GroupBuyFeedCard`：改为 photo-first 商品卡，突出图片、团长 / 店铺信任信息、价格和主 CTA。
- `LeaderTrustBlock` / `StoreSummaryCard`：补齐信任徽章、店铺信息和订阅状态展示。
- `EmptyState` / `LoadingView` / `ErrorView`：统一空态、加载态和错误重试样式。
- `FixedActionBar`：统一 50px 胶囊主按钮和次按钮层级。
- `PriceText` / `OrderAmountBreakdown`：保持整数分格式化展示，提升价格层级但不改变金额逻辑。

### 5.3 已实现页面视觉统一

- 首页 `/`：参考 demo 改为 marketplace feed；顶部使用品牌区、pill 搜索占位、频道 Tab、分类 chips、photo-first 团购卡。
- 团购详情 `/group-buys/:id`：强化封面图、团长信任区、商品选择卡、库存 / 已售、底部购买 CTA；购物车入口只允许灰态预留或不展示，不实现购物车。
- 团长主页 `/leaders/:id`：统一头像、店铺卡、信任徽章、订阅按钮和团购卡片流。
- 下单确认 `/checkout`：统一地址卡、商品快照卡、金额明细和底部提交按钮；提交按钮继续防重复点击。
- 地址页：统一地址卡片、默认标签、编辑入口和表单控件。
- 订单列表 `/orders`：参考 demo 增强搜索占位、状态 Tab、订单卡片、状态标签和操作按钮。
- 订单详情 `/orders/:id`：统一步骤条、地址快照、商品快照、金额明细和底部操作按钮。
- 个人中心 `/profile`：按未登录、买家、团长三种状态统一视觉；可展示非 MVP 入口，但必须灰态或 toast。
- 一键开团 `/open-group`：普通团购入口保持现有真实流转；预售、卡券、报名继续灰态。
- 消息 `/messages`：可使用 demo 的消息页视觉，但只展示占位和空态，不请求消息 API。
- 登录、创建店铺、我的店铺：统一表单卡片、主按钮、说明文案和错误反馈。

### 5.4 非 MVP 控件预留规则

允许预留控件，但必须遵守：

- 购物车：可显示 56px 浮动入口作为灰态控件或 toast 入口；不得添加购物车状态、SKU 加购逻辑、结算逻辑或接口调用。
- 优惠券 / 红包：只显示“暂未开放”或置灰，不参与订单金额。
- 售后：订单状态可展示售后预留文案；不得实现申请、审核、退款流程。
- 帮卖：只作为禁用入口或不展示；不得实现佣金、结算、帮卖设置。
- 公众号关注 / 推送 / 消息通知：只做提示占位；不得实现真实触达。
- 商品库、分类管理、批量导入、素材库：不在本批实现。

所有非 MVP 入口必须复用或补充 `isFeatureDisabled(feature)` 判断，并有单元测试覆盖。

## 6. 测试要求

本批完成后必须运行：

```bash
cd frontend
npm run typecheck
npm run lint
npm run test:unit
npm run build
npm run test:e2e
```

如果本地后端和数据库可用，应额外运行：

```bash
cd frontend
npm run test:e2e:live
```

测试重点：

- 主页面无横向滚动。
- 主 CTA 高度和触控区域符合移动端要求。
- 非 MVP 入口不会触发真实 API。
- 已有登录、浏览、下单、订单、支付、创建店铺、编辑店铺链路不回归。
- 金额仍以整数分展示和传输，不引入浮点金额业务值。

## 7. 验收标准

- 已实现页面在视觉上统一为“温暖奶油背景 + 绿色零售 CTA + 移动 marketplace 信息流”。
- 首页和团购卡片完成 photo-first 改造，价格、信任信息和跟团 CTA 清晰。
- 表单、订单、地址、个人中心、一键开团和消息占位页面具有一致的卡片、按钮、状态和空态样式。
- 全站主要按钮不小于 44px，主 CTA 视觉高度约 50px。
- 底部 Tab、固定操作栏、浮动入口均正确处理 safe area。
- 本批没有新增业务接口调用，没有提前实现 Batch 06+ 能力。
- 非 MVP 入口均灰态、占位或 toast，且不会调用不存在接口。

## 8. 联调文档更新项

更新 `docs/前后端联调文档.md`：

- 在“前端本地启动与通用 H5 约定”中补充 Design Foundation 约定。
- 补充移动端 UI 验收项：safe area、触控尺寸、主 CTA、卡片样式、photo-first 团购卡。
- 补充非 MVP 控件预留规则，明确购物车、优惠券、售后、帮卖、公众号推送等只能灰态或占位。
- 在 Batch 05.5 与 Batch 06 之间记录 Frontend Batch 05.8 的验证范围。

## 9. 禁止事项

- 不新增后端接口。
- 不实现商品管理 UI。
- 不实现发布团购 UI。
- 不实现团长订单发货 UI。
- 不实现订阅列表和会员卡页面。
- 不实现购物车、SKU 加购、购物车结算或浮动购物车真实逻辑。
- 不实现优惠券、红包、完整售后退款、帮卖分销、积分商城、公众号推送或平台后台。
- 不为了复刻 demo 破坏现有真实 API、登录拦截、状态流转和测试链路。

## 10. 本批完成后必须输出的结果

- 已统一的设计基线清单。
- 已改造页面 / 组件清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
