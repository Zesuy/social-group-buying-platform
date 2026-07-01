# Frontend Batch 08.5：全前端组件化重构与页面瘦身

## 1. 目标

在 Frontend Batch 08 完成后、Frontend Batch 09 全链路回归冻结前，新增一个专项重构批次，解决当前前端按页面推进导致的重复 UI、重复样式和组件复用不足问题。

本批目标是把全前端页面从“页面内堆 UI 和样式”重构为“组件负责展示与交互基元，page 只负责取数、路由、状态管理和事件编排”。

本批不新增业务能力、不新增后端接口、不改变既有路由、接口字段或业务状态机。

## 2. 前置条件

- Frontend Batch 00 到 Frontend Batch 08 已完成。
- 全部 MVP 页面已具备初始功能。
- `docs/dev/batches/frontend-batches/demo/DESIGN-starbucks.md` 和 `docs/dev/batches/frontend-batches/demo/pages/` 已作为当前移动端视觉参考。
- 本批是 Frontend Batch 09 前的专项重构批次，用于统一组件系统和页面实现方式。

## 3. 页面范围

覆盖前端 MVP 全部页面：

- 5 个底部 Tab：首页、订单、一键开团、消息、个人中心。
- 公共浏览：团购详情、团长主页。
- 买家链路：登录、地址、下单、订单列表、订单详情。
- 团长链路：创建店铺、我的店铺、商品管理、发布团购、团购管理、团长订单、发货。
- 私域关系：我的订阅、会员卡。
- 非 MVP 占位页和灰态入口。

页面参考 HTML：

- 继续按各页面对应的 `docs/dev/batches/frontend-batches/demo/pages/*.html` 参考视觉和交互。
- 继续遵守 `docs/dev/batches/frontend-batches/demo/DESIGN-starbucks.md` 的移动端视觉方向：温暖浅背景、绿色零售 CTA、圆角卡片、胶囊主按钮和清晰 marketplace 信息流。

## 4. 涉及接口

本批不新增接口。

允许继续使用各页面已接入的 MVP 接口，包括认证、公共浏览、店铺、商品、普通团购、地址、订单、模拟支付、团长订单、订阅和会员卡接口。

禁止为了组件化或复刻 demo 增加以下接口或真实逻辑：

- 购物车接口。
- 优惠券 / 红包接口。
- 帮卖分销接口。
- 积分商城接口。
- 公众号推送 / 消息通知接口。
- 完整售后 / 退款接口。
- 平台后台接口。

## 5. 实现任务

### 5.1 基础组件系统

新增或整理基础 UI 组件，并统一由页面复用：

- `AppButton`：统一 primary、ghost、danger、plain、disabled、loading 和 44px 触控要求。
- `AppCard`：统一卡片背景、圆角、间距、阴影和可点击态。
- `AppStatusPill`：统一订单、支付、团购、配送和非 MVP 灰态标签。
- `AppTabs`：统一频道、状态筛选、管理列表 Tab 样式。
- `AppListPage`：统一 loading、error、empty、pull refresh、pagination 的页面骨架。
- `AppFormCard`：统一表单卡片标题、分组和底部分隔。
- `AppFormRow`：统一 label、value、control、arrow、disabled 和错误提示布局。
- `AppFixedActions`：统一固定底部操作栏、安全区和单双按钮布局。
- `AppNoticeStrip`：统一轻提示条、非 MVP 说明和运营提醒。
- `AppPageNote`：统一页面说明、灰态规则说明和轻量提示文本。

### 5.2 业务组件系统

新增或整理业务组件，减少 page 内重复模板：

- `ProductListItem`：商品列表卡片，覆盖封面、名称、价格、库存、上下架和编辑 / 删除入口。
- `GroupBuyManageCard`：团长团购管理卡片，覆盖封面、标题、状态、配送方式、结束时间和点击进入详情。
- `GroupBuyDetailHeader`：团购详情头部，覆盖标题、封面、配送标签、热度和基础信息。
- `GroupBuyItemSelector`：团购商品选择区，覆盖商品图片、规格名、团购价、库存、销量和选中态。
- `OrderListCard`：买家和团长订单列表卡片，支持不同 mode 下的按钮和摘要展示。
- `OrderSnapshotCard`：订单详情商品快照卡，统一商品、数量、单价、小计和地址快照引用方式。
- `OrderStatusSteps`：订单状态步骤条，统一待支付、已支付、已发货、已完成、已取消展示。
- `LeaderStoreHeader`：团长 / 店铺信息头部，统一头像、店铺、信任徽章、订阅状态和入口按钮。
- `ProfileFeatureGrid`：个人中心功能宫格，统一真实入口与非 MVP 灰态入口。
- `NonMvpEntryCard`：非 MVP 入口统一卡片，保证置灰、toast 或占位，不触发真实接口。
- `CheckoutSection`：结算页分区卡片，统一地址、商品、金额明细、协议和备注区块。

### 5.3 复用并收口已有组件

保留并优先改造现有组件，而不是在页面中继续复制样式：

- `PageLayout`
- `LoadingView`
- `ErrorView`
- `EmptyState`
- `PriceText`
- `ImageWithFallback`
- `AddressCard`
- `AddressForm`
- `ProductForm`
- `ShipmentForm`
- `SubscriptionCard`
- `MemberCardItem`

这些组件应继续承担原有业务语义，并补齐统一 token、状态、触控尺寸、空态和错误态表现。

### 5.4 页面瘦身规则

所有页面重构后必须满足：

- page 只保留 API 调用、路由跳转、状态管理、事件处理和少量页面级布局。
- 通用展示和交互必须下沉到组件。
- 页面内不得重复定义 `.card`、`.btn`、`.field`、`.fixed-actions`、`.order-card`、`.status-chip`、`.list-item` 等通用样式。
- 页面内不得重复维护订单状态文案、团购状态文案、配送文案、金额格式和日期格式。
- 清理大多数内联 `style="..."`，改为组件 props、统一 class 或 token。
- 页面 scoped style 只允许保留页面独有布局，不允许承载可复用视觉系统。

### 5.5 工具函数收口

扩展和统一工具函数：

- `frontend/src/utils/format.ts`：统一金额、日期、日期时间和数量展示。
- `frontend/src/utils/status.ts`：统一订单、支付、团购、配送状态文案和 tone。
- `frontend/src/utils/non-mvp.ts`：继续作为非 MVP 入口灰态判断来源，并补齐本批涉及入口的测试覆盖。

页面和组件必须复用这些工具，不得各自维护重复 `formatDate`、状态颜色 map 或金额展示逻辑。

### 5.6 非 MVP 边界

本批只允许整理非 MVP 入口的统一展示，不允许实现真实能力：

- 购物车只允许灰态、占位或 toast，不得维护购物车状态、SKU 加购、结算或接口调用。
- 优惠券 / 红包只允许灰态或提示，不参与订单金额。
- 帮卖、积分商城、公众号推送、客服、分享海报、完整售后退款和平台后台只允许灰态、占位或 toast。
- 所有非 MVP 入口必须可通过统一组件或 `isFeatureDisabled(feature)` 判断识别。

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

测试重点：

- 单元测试覆盖格式化工具、状态工具和非 MVP 入口配置。
- 组件测试覆盖 loading / error / empty、按钮状态、状态标签、订单卡片、商品卡片、团购卡片。
- E2E 覆盖首页浏览、团购详情、下单、地址、订单支付、确认收货。
- E2E 覆盖团长商品、发布团购、团购管理、订单发货。
- E2E 覆盖订阅、会员卡、消息空态和非 MVP 入口灰态。
- 重构后无横向滚动，固定底部操作栏不遮挡页面内容。
- 金额仍以整数分展示和传输，不引入浮点金额业务值。

## 7. 验收标准

- 全前端页面形成稳定组件复用结构，page 明显瘦身。
- 通用卡片、按钮、表单行、状态标签、固定操作栏、列表骨架不再在页面内重复定义。
- 订单、商品、团购、店铺、个人中心、订阅和会员卡的重复展示逻辑已沉淀为组件。
- 日期、金额、状态和非 MVP 灰态判断统一从工具函数或组件入口获取。
- Starbucks-inspired 移动端视觉基调保持一致：温暖浅背景、绿色零售 CTA、圆角卡片、胶囊主按钮、photo-first 信息流。
- 不新增后端接口，不改变现有业务状态流转，不实现 MVP 外能力。
- `npm run typecheck`、`npm run lint`、`npm run test:unit`、`npm run build` 和 `npm run test:e2e` 均通过。

## 8. 联调文档更新项

更新 `docs/前后端联调文档.md`：

- 增加 Frontend Batch 08.5 组件化重构说明。
- 标记本批不新增接口、不改变调用顺序和请求响应。
- 补充全站组件化后的页面入口、主要组件、非 MVP 灰态入口检查项。
- 记录本批测试命令和结果。

## 9. 禁止事项

- 不新增后端接口。
- 不改变既有 API 请求 / 响应字段。
- 不改变路由路径和登录 / 团长权限拦截规则。
- 不实现购物车、优惠券、红包、帮卖、积分商城、公众号推送、客服会话、分享海报、完整售后退款或平台后台。
- 不为了减少重复而删除必要 loading、empty、error、disabled、confirm 和 toast 反馈。
- 不为了通过测试绕过真实接口、登录拦截、状态流转或非 MVP 灰态约束。

## 10. 本批完成后必须输出的结果

- 已新增 / 整理组件清单。
- 已瘦身页面清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
