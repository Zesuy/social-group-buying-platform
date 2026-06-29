# Frontend Batch 02+03 Handoff

> 批次说明：Batch 02（公共浏览、团购详情、团长主页与订阅）和 Batch 03（地址管理、下单预览与创建订单）合并实施。
>
> 实现时间：2026-06-29

## 已实现能力清单

### 页面

| 页面 | 路由 | 说明 |
|---|---|---|
| 首页团购流 | `/` | 品牌区、提醒横幅、频道 Tab（占位）、分类横滑（占位）、团购卡片列表、下拉刷新、触底加载、空态、错误重试 |
| 团购详情 | `/group-buys/:id` | 封面图、团长/店铺信任区、标题介绍、配送/时间信息、商品列表、数量选择、底部固定操作栏（购买+订阅）；库存不足/已结束禁用购买 |
| 团长主页 | `/leaders/:id` | 团长信息、店铺摘要、信任信息、订阅按钮、团购列表分页 |
| 地址列表 | `/addresses` | 地址卡片、默认地址标识、选择模式返回 checkout、编辑/删除、无地址引导 |
| 新增地址 | `/addresses/new` | 地址表单（姓名/手机/省市区/详细地址/默认开关） |
| 编辑地址 | `/addresses/:id/edit` | 地址表单回填、更新提交 |
| 下单确认 | `/checkout` | 地址卡片（可选）、商品明细、数量调整、备注、协议勾选、preview 金额区、底部提交栏；数量/地址变更重新 preview |
| 订单详情（占位） | `/orders/:id` | 订单提交成功视图、订单号、待支付状态、不接真实订单查询 |

### 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/v1/group-buys` | 公开团购列表（分页） |
| GET | `/api/v1/group-buys/{groupBuyId}` | 公开团购详情（含团长/店铺/商品/viewer） |
| GET | `/api/v1/leaders/{leaderId}/homepage` | 团长主页（含店铺信息和团购列表） |
| POST | `/api/v1/leaders/{leaderId}/subscription` | 订阅团长 |
| DELETE | `/api/v1/leaders/{leaderId}/subscription` | 取消订阅 |
| GET | `/api/v1/my/addresses` | 地址列表 |
| POST | `/api/v1/my/addresses` | 创建地址 |
| PATCH | `/api/v1/my/addresses/{addressId}` | 更新地址 |
| DELETE | `/api/v1/my/addresses/{addressId}` | 删除地址 |
| POST | `/api/v1/orders/preview` | 订单预览（不创建、不扣库存） |
| POST | `/api/v1/orders` | 创建订单 |

## 文件清单

### 新增文件

| 文件 | 说明 |
|---|---|
| `src/api/groupBuys.ts` | 公开团购 API |
| `src/api/leaders.ts` | 团长 API（主页、订阅） |
| `src/api/addresses.ts` | 地址管理 API |
| `src/api/orders.ts` | 订单预览/创建 API |
| `src/stores/checkout.ts` | Checkout 上下文 Store（非购物车） |
| `src/composables/usePagination.ts` | 通用分页组合式 |
| `src/components/PriceText.vue` | 价格展示（formatAmount 封装） |
| `src/components/ImageWithFallback.vue` | 图片组件（加载失败占位） |
| `src/components/GroupBuyFeedCard.vue` | 团购卡片 |
| `src/components/ChannelTabs.vue` | 频道 Tab 占位 |
| `src/components/CategoryChips.vue` | 分类横滑占位 |
| `src/components/LeaderTrustBlock.vue` | 团长/店铺信任信息块 |
| `src/components/StoreSummaryCard.vue` | 店铺摘要卡片 |
| `src/components/AddressCard.vue` | 地址卡片（含编辑/删除/选择） |
| `src/components/AddressForm.vue` | 地址表单（含校验） |
| `src/components/OrderAmountBreakdown.vue` | 金额明细组件 |
| `src/views/AddressNewView.vue` | 新增地址页 |
| `src/views/AddressEditView.vue` | 编辑地址页 |
| `src/views/OrderDetailView.vue` | 订单详情占位页 |
| `tests/unit/price-text.test.ts` | PriceText 组件测试 |
| `tests/unit/group-buy-feed-card.test.ts` | GroupBuyFeedCard 组件测试 |
| `tests/unit/pagination.test.ts` | usePagination 单元测试 |
| `tests/unit/group-buy-detail.test.ts` | 团购详情禁用状态测试 |
| `tests/unit/address-card.test.ts` | AddressCard 组件测试 |
| `tests/unit/address-form.test.ts` | 地址表单测试 |
| `tests/unit/checkout.test.ts` | OrderAmountBreakdown 测试 |

### 修改文件

| 文件 | 说明 |
|---|---|
| `src/types/api.ts` | 新增公开浏览/地址/订单/订阅类型 |
| `src/types/index.ts` | 导出新类型 |
| `src/api/index.ts` | 导出新 API 模块 |
| `src/stores/index.ts` | 导出 checkoutStore |
| `src/router/index.ts` | 新增 `/addresses/new`、`/addresses/:id/edit`、`/orders/:id` 路由 |
| `src/views/IndexView.vue` | 从占位重写为完整首页 |
| `src/views/GroupBuyDetailView.vue` | 从占位重写为完整详情页 |
| `src/views/LeaderDetailView.vue` | 从占位重写为团长主页 |
| `src/views/AddressesView.vue` | 从占位重写为地址管理 |
| `src/views/CheckoutView.vue` | 从占位重写为下单确认页 |
| `tests/unit/setup.ts` | 注册新增 Vant 组件 |

## Flyway 迁移清单

无（前端批次不涉及数据库迁移）。

## 测试清单

| 测试文件 | 用例数 | 说明 |
|---|---|---|
| `tests/unit/price-text.test.ts` | 7 | 整数分展示、空值、颜色/尺寸样式、插槽 |
| `tests/unit/group-buy-feed-card.test.ts` | 6 | 标题/价格/店铺/销量渲染、click 事件、已结束标签 |
| `tests/unit/pagination.test.ts` | 9 | 初始状态、加载、触底加载、刷新、错误、重试、空态、重置 |
| `tests/unit/group-buy-detail.test.ts` | 5 | 已结束标签、PriceText 整数分展示 |
| `tests/unit/address-card.test.ts` | 8 | 姓名/电话/地址渲染、默认标签、选择/编辑/删除事件、选中样式 |
| `tests/unit/address-form.test.ts` | 4 | 表单渲染、保存按钮、loading 状态、提交按钮类型 |
| `tests/unit/checkout.test.ts` | 4 | 金额明细渲染（含优惠行隐藏）、零金额 |
| `tests/e2e/public-browsing-checkout.spec.ts` | 10 | 售罄项「已售罄」标签、无地址用户「去添加地址」CTA、地址创建后返回 checkout |

## 联调文档位置

- `docs/前后端联调文档.md` 第 6 节"公共浏览团购"：更新页面路径、UI 入口、分页/刷新方式、订阅状态展示、非 MVP 入口 toast
- 第 7 节"地址管理与订单预览"：补充 checkout 上下文来源、地址选择返回、preview 触发时机
- 第 8 节"创建订单"：补充提交防重复、成功跳转、创建阶段说明
- 第 12 节"订阅团长"：补充两种 source 和未登录处理

## 测试命令和结果

```bash
npm run typecheck        # ✅ 通过
npm run lint             # ✅ 通过
npm run test:unit        # ✅ 120 tests passed (17 files)
npm run build            # ✅ 构建成功 (426ms)
npm run test:e2e         # ✅ 15 tests passed (2 个 spec 文件：smoke + public-browsing-checkout)
```

## 已验证修复

以下 issue 在 code review 中发现并已修复（2026-06-29）：

| Issue | 描述 | 修复 |
|---|---|---|
| P1 | Checkout 首次进入用 `addressId: 0` 调 preview，空地址返回后 `RESOURCE_NOT_FOUND` | 改为先拉地址列表，无地址时显示"去添加地址"引导 CTA；有地址则自动选默认/首个再调 preview |
| P1 | 新增地址后不返回 checkout，`from=checkout` 丢失 | `goToNew()` 透传 `from` query；创建成功后 `checkoutStore.setAddress(created.id)` 并 `router.replace('/checkout')` |
| P2 | 库存为 0 的商品仍可被选择并发起购买 | 无库存项显示「已售罄」标签、禁用选择按钮、`selectItem` 加库存守卫、全部无库存时底部栏显示「库存不足」 |
| P1 | rePreview 失败后旧 preview 和提交栏仍存在 | `doPreview` 开始时清空 `preview.value = null`，catch 中也清空；模板 `v-if="preview && !loading && !error"` 避免 error 时残留 |
