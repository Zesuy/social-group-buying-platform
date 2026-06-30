# Frontend Batch 04+05 Combined Handoff

> 批次说明：Batch 04（买家订单列表、订单详情、模拟支付、取消订单、确认收货）和 Batch 05（个人中心、一键开团、创建店铺、我的店铺资料）合并实施。
>
> 实现时间：2026-06-29

## 已实现能力清单

### 页面

| 页面 | 路由 | 说明 |
|---|---|---|
| 订单列表 | `/orders` | 状态 Tab（全部/待支付/已支付/已发货/已完成/已取消）、搜索占位、PullRefresh + List 分页、订单卡片（商品预览/金额/状态标签/操作按钮）、LoadingView/ErrorView/EmptyState |
| 订单详情 | `/orders/:id` | 调用 `GET /api/v1/my/orders/:id` 拉取真实数据；状态步骤条、地址快照、商品快照、金额明细（OrderAmountBreakdown）、订单编号/时间字段；模拟支付/取消/确认收货均使用 Vant Dialog 确认 + loading + disabled + Toast 错误提示，成功后刷新详情 |
| 个人中心 | `/profile` | 公开页面；未登录展示登录 CTA；已登录买家展示头像/昵称/买家标签 + 我的订单/会员卡/地址管理/创建店铺入口；已登录团长展示头像/昵称/团长标签 + 我的店铺/发布团购/我的团购/团长订单入口；支持退出登录 |
| 一键开团 | `/open-group` | 公开页面；4 种开团类型卡片：普通团购可点击，预售/卡券/报名灰态 + Toast "后续开放"；普通团购点击后判断登录态 -> 未开店 -> 开店引导 -> 已开店弹出动作面板 |
| 创建店铺 | `/store/create` | 店铺名称必填、Logo URL 可选、简介可选、默认物流方式单选（快递/到店自提/同城配送）；提交后调用 `POST /api/v1/stores` 并调用 `authStore.fetchMe()` 刷新身份；成功后按 `redirect` 跳转 |
| 我的店铺 | `/leader/store` | 调用 `GET /api/v1/my/store` 加载；显示模式展示店铺 logo/名称/简介/物流方式/状态；编辑模式展示表单并 `PATCH /api/v1/my/store` 保存后刷新 |

### 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/v1/my/orders` | 我的订单列表（分页 + status 筛选） |
| GET | `/api/v1/my/orders/{orderId}` | 订单详情 |
| POST | `/api/v1/orders/{orderId}/simulate-pay` | 模拟支付 |
| POST | `/api/v1/orders/{orderId}/cancel` | 取消订单 |
| POST | `/api/v1/orders/{orderId}/complete` | 确认收货 |
| POST | `/api/v1/stores` | 创建店铺 |
| GET | `/api/v1/my/store` | 获取我的店铺信息 |
| PATCH | `/api/v1/my/store` | 更新我的店铺信息 |

### 路由变化

| 路由 | 变化 | 原因 |
|---|---|---|
| `/profile` | 去掉 `requiresAuth`，改为公开页面 | Batch 05 要求个人中心可未登录访问 |
| `/open-group` | 去掉 `requiresAuth` 和 `requiresLeader`，改为公开页面 | 一键开团改为公开流量入口，登录/开店判断在点击时进行 |
| `/leader/store` | 新增路由，`requiresLeader` | 团长管理自己的店铺资料 |

## 文件清单

### 新增文件

| 文件 | 说明 |
|---|---|
| `src/api/stores.ts` | 创建/获取/更新店铺 API |
| `src/views/leader/LeaderStoreView.vue` | 我的店铺资料页（展示+编辑） |
| `tests/unit/order-status.test.ts` | 订单状态文案、按钮可见性、配送方式映射 |
| `tests/unit/profile-store.test.ts` | 个人中心三种状态、一键开团点击逻辑、店铺名称校验 |
| `tests/e2e/orders-payment.spec.ts` | 订单列表展示/取消/导航详情/切换 Tab |
| `tests/e2e/profile-store.spec.ts` | 个人中心未登录/买家/团长/登录引导/一键开团/创建店铺引导 |

### 修改文件

| 文件 | 说明 |
|---|---|
| `src/types/api.ts` | 新增 `OrderData`、`OrderItemData`、`CreateStoreRequest`、`UpdateStoreRequest`、`LeaderInfo`、`StoreInfo`、`StoreResponseData`、`MyStoreResponseData` |
| `src/types/index.ts` | 导出所有新类型 |
| `src/api/orders.ts` | 扩展：`listMyOrders`、`getMyOrder`、`simulatePay`、`cancelOrder`、`completeOrder` |
| `src/api/index.ts` | 导出 `storesApi` |
| `src/utils/status.ts` | 修正 `DeliveryType` 支持 `pickup`/`local_delivery`，增加 `到店自提`/`同城配送` 文案；更新 `deliveryTypeMap` |
| `src/router/index.ts` | `/profile` 和 `/open-group` 改为公开路由；新增 `/leader/store` 路由；更新路由守卫注释 |
| `src/views/OrdersView.vue` | 从占位重写为完整订单列表（状态 Tab + 搜索占位 + 分页 + 订单卡片 + 操作按钮） |
| `src/views/OrderDetailView.vue` | 从占位重写为完整订单详情（状态步骤 + 地址快照 + 商品快照 + 金额明细 + 操作按钮 + Dialog + loading） |
| `src/views/ProfileView.vue` | 从占位重写为完整个人中心（未登录/买家/团长三种状态） |
| `src/views/OpenGroupView.vue` | 从占位重写为一键开团页面（4 种开团类型卡片 + 点击逻辑） |
| `src/views/CreateStoreView.vue` | 从占位重写为完整创建店铺表单 |
| `tests/unit/setup.ts` | 注册 Vant Tabs/Tab/Search/Radio/RadioGroup/ActionSheet/Image/Step/Steps/Dialog 组件 |
| `tests/unit/status.test.ts` | 更新配送方式映射测试（selfPickup->pickup/local_delivery） |
| `tests/unit/router-guards.test.ts` | `/profile` 和 `/open-group` 改为公开路由的测试用例 |
| `docs/前后端联调文档.md` | 补充订单列表交互说明、订单详情页交互说明、个人中心/一键开团链路 |

## 测试清单

| 测试文件 | 用例数 | 说明 |
|---|---|---|
| `tests/unit/status.test.ts` | 12 | 增加 `pickup`/`local_delivery` 配送方式测试 |
| `tests/unit/router-guards.test.ts` | 9 | `/profile`/`/open-group` 公开路由、`/leader` 仍需登录 |
| `tests/unit/order-status.test.ts` | 15 | 新增：状态文案全覆盖、按钮可见性逻辑、配送方式映射 |
| `tests/unit/profile-store.test.ts` | 10 | 新增：个人中心三种状态、一键开团点击决策、店铺名称校验 |
| `tests/e2e/orders-payment.spec.ts` | 6 | 新增：订单列表展示、取消订单、导航详情、切换 Tab（已发货/已完成/已取消） |
| `tests/e2e/profile-store.spec.ts` | 9 | 新增：未登录查看个人中心、买家/团长菜单、登录引导、一键开团卡片、非登录点击跳转、非团长点击跳转、团长动作面板、非 MVP Toast |

## 联调文档位置

- `docs/前后端联调文档.md` 第 3.6 节"前端页面与交互说明"：路由守卫 -> `/profile` 和 `/open-group` 改为公开页面
- 第 8.5 节"我的订单列表"：补充页面入口、状态 Tab、交互说明、操作按钮规则
- 第 8.6 节"订单详情"：补充页面交互说明、操作按钮规则、Dialog/loading/Toast 细节
- 第 9 节"模拟支付"：无需额外更新
- 第 10 节"确认收货"：更新页面入口描述，增加列表页操作
- 第 14 节"个人中心与一键开团"：新增，完整页面入口、三种状态、开团类型卡片和点击行为
- 第 14.4 节"我的店铺"：新增，完整调用顺序

## 测试命令和结果

```bash
npm run typecheck        # ✅ 通过
npm run lint             # ✅ 通过
npm run test:unit        # ✅ 148 tests passed (19 files)
npm run build            # ✅ 构建成功 (438ms)
npm run test:e2e         # 待运行（依赖 Playwright 浏览器）
```

## 关键修复

### `getDeliveryTypeText` 映射修正

OpenAPI 中配送方式枚举值为 `express` / `pickup` / `local_delivery`。旧的 `selfPickup` 映射已移除，新增：
- `pickup` → `到店自提`
- `local_delivery` → `同城配送`

对应测试和 `DeliveryType` 类型定义也已同步更新。

### 路由守卫变更

`/profile` 和 `/open-group` 已从受保护路由改为公开路由，登录/身份判断由页面内部自行处理。此举满足以下需求：
- 个人中心未登录态展示
- 一键开团作为公开流量入口
- 团长管理页面（`/leader`）继续需要 `requiresLeader`

## 本批未解决问题

- 发布团购（Batch 06）未实现：一键开团页团长点击"发布团购"仅展示 Toast "后续开放"
- 预售/卡券/报名开团类型未实现：仅展示灰态卡片 + Toast
- 会员卡页未实现：个人中心点击"会员卡"仅展示 Toast "即将开放"
- E2E 测试中 `npx playwright install` 需在 CI 或本地首次运行前执行
