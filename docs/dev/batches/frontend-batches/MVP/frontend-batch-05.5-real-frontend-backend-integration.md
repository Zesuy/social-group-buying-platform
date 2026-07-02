# Frontend Batch 05.5：真实前后端联调与 Live E2E 固化

## 1. 目标

在 Frontend Batch 05 完成后、Frontend Batch 06 开始前，补齐真实前后端联调层：前端不再只依赖 Playwright route mock 或本地假数据，而是连接本地 Spring Boot 后端和 MySQL/Flyway 数据库，验证 Batch 01-05 已实现页面链路与后端冻结 API 的真实兼容性。

本批不新增业务页面，不提前实现商品管理、发布团购、团长订单发货、订阅会员卡等后续批次能力；重点是发现并修复前端与真实 API、真实状态流转、真实错误响应之间的差异。

## 2. 前置条件

- Frontend Batch 05 已完成，并已产出 `docs/dev/batches/frontend-batches/context/frontend-batch-04-05-buyer-orders-payment-profile-store.md`。
- 后端已完成并可运行 MVP 当前接口，至少覆盖本批涉及的认证、店铺、公开团购、地址、订单、模拟支付、确认收货接口。
- 本地 MySQL 可用，开发数据库建议使用 `groupshop_dev`，后端 `dev` profile 默认端口为 `8080`。
- 前端 Vite dev server 默认端口为 `5173`，`frontend/.env.development` 或 `.env.local` 中 `VITE_API_BASE_URL=http://localhost:8080`。
- 首次运行 Playwright 前已执行 `npx playwright install`。

## 3. 本批执行计划

1. 新增本批执行文档 `docs/dev/batches/frontend-batches/frontend-batch-05.5-real-frontend-backend-integration.md`。
2. 同步创建本批 handoff 文档 `docs/dev/batches/frontend-batches/context/frontend-batch-05.5-real-frontend-backend-integration.md`，用于记录真实联调执行结果、修复清单、测试数据和未解决问题。
3. 启动真实后端和真实前端，确认 `GET /api/v1/health`、Vite proxy、认证头、统一响应解包全部可用。
4. 建立 Live E2E 测试数据准备方式。允许 Playwright 在测试前通过后端 API 准备团长、店铺、商品/团购、买家、地址、订单等前置数据，但页面验证必须走真实浏览器和真实前端 API client。
5. 将 Batch 01-05 的关键页面链路切换为至少一套真实后端 E2E 覆盖，并保留原有单元测试和必要 mock E2E。
6. 修复真实联调发现的前端问题，包括字段名不一致、空 data 处理、状态枚举、金额整数分、时间格式、错误码展示、登录恢复、重复操作按钮状态等。
7. 更新 `docs/前后端联调文档.md`，新增或补充“真实联调运行方式 / Batch 05.5 验证范围 / 测试数据准备方式 / 已知限制”。

## 4. 涉及接口

本批只联调 Frontend Batch 01-05 已使用或为测试数据准备所必需的接口。

### 页面真实调用接口

- `POST /api/v1/auth/mock-login`
- `GET /api/v1/me`
- `GET /api/v1/group-buys?page=1&pageSize=20`
- `GET /api/v1/group-buys/{groupBuyId}`
- `GET /api/v1/leaders/{leaderId}/homepage?page=1&pageSize=20`
- `GET /api/v1/my/addresses`
- `POST /api/v1/my/addresses`
- `PATCH /api/v1/my/addresses/{addressId}`
- `DELETE /api/v1/my/addresses/{addressId}`
- `POST /api/v1/orders/preview`
- `POST /api/v1/orders`
- `GET /api/v1/my/orders?status=xxx&page=1&pageSize=20`
- `GET /api/v1/my/orders/{orderId}`
- `POST /api/v1/orders/{orderId}/simulate-pay`
- `POST /api/v1/orders/{orderId}/cancel`
- `POST /api/v1/orders/{orderId}/complete`
- `POST /api/v1/stores`
- `GET /api/v1/my/store`
- `PATCH /api/v1/my/store`

### Live E2E 数据准备接口

- `POST /api/v1/my/store/products`
- `POST /api/v1/my/store/group-buys`

说明：这两个接口只允许在测试数据准备阶段通过 API helper 使用。不得在本批新增商品管理或发布团购页面，不得把 Batch 06 的 UI 能力提前实现到前端。

## 5. 页面范围

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
- `/store/create`
- `/leader/store`

## 6. 实现任务

### 6.1 真实环境运行

在项目根目录启动后端：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

在 `frontend/` 启动前端：

```bash
npm install
npm run dev
```

验收点：

- `http://localhost:8080/api/v1/health` 返回统一成功响应。
- `http://localhost:5173/api/v1/health` 通过 Vite proxy 转发到后端。
- 前端请求均以 `/api/v1` 开头，不硬编码后端域名。
- `Authorization: Bearer <accessToken>` 能被真实后端识别。

### 6.2 Live E2E 基础设施

新增或整理 Live E2E helper，建议放在 `frontend/tests/e2e/helpers/`：

- `liveApi.ts`：封装后端 API 请求、统一响应解包、错误打印、token 管理。
- `fixtures.ts`：提供创建买家、创建团长、创建店铺、创建商品、发布团购、创建地址、创建订单等可复用准备函数。
- `live-test-data.ts`：集中维护测试手机号、昵称、图片 URL、默认地址、默认商品、默认团购参数。

要求：

- Live E2E spec 文件命名建议使用 `*.live.spec.ts`，例如 `real-auth-public.spec.ts`、`real-checkout-orders.spec.ts`、`real-profile-store.spec.ts`。
- Live E2E 不允许使用 `page.route('**/api/**')` mock 页面接口。
- 测试准备数据可以直接调用真实后端 API，但页面操作和断言必须通过浏览器完成。
- 测试手机号必须使用唯一后缀，避免和历史数据冲突；推荐使用时间戳或测试运行 id，例如 `139${runId后8位}`。
- 不依赖固定自增 ID，所有 `leaderId`、`storeId`、`groupBuyId`、`groupBuyItemId`、`addressId`、`orderId` 都以前一步真实响应为准。

### 6.3 真实链路覆盖

至少补齐以下 Live E2E：

| 链路 | 必测场景 | 成功判定 |
|---|---|---|
| 认证与登录恢复 | 登录页提交真实 `mock-login`；刷新页面后 `GET /api/v1/me` 恢复登录态；退出登录后受保护页面重定向 | token 持久化、用户信息展示、退出后清除本地状态 |
| 公开浏览 | API 准备团长店铺和普通团购；首页展示真实团购；进入详情；进入团长主页 | 首页卡片、详情商品、团长/店铺信息均来自真实响应 |
| 地址与下单 | 买家从团购详情进入 checkout；新增地址；预览订单；创建订单 | preview 金额来自后端，创建成功跳转真实订单详情 |
| 买家订单 | 订单列表拉取真实订单；订单详情展示地址快照、商品快照、金额明细；待支付订单可取消 | 列表和详情状态一致，取消后刷新为 `canceled` |
| 模拟支付 | 创建待支付订单；点击模拟支付；刷新详情和列表 | 订单状态变为 `paid`，重复支付错误能正确提示 |
| 确认收货 | 通过 API 准备已发货订单或使用后端可用方式完成发货；页面点击确认收货 | 订单状态变为 `completed`；不可确认状态展示后端错误 |
| 个人中心 | 未登录、买家、团长三种状态真实展示；买家入口和团长入口符合身份 | `/profile` 公开可访问，已登录状态由真实 `/me` 决定 |
| 开店与店铺资料 | 买家创建店铺；`GET /api/v1/me` 刷新为团长；`/leader/store` 拉取并编辑真实店铺资料 | 创建后出现团长入口，编辑后真实后端数据更新 |
| 一键开团 | 未登录点击普通团购进入登录；买家点击进入创建店铺；团长点击仅进入已实现入口或提示后续开放 | 非 MVP 类型仍置灰，不触发真实 API |

说明：如果当前后端尚无前端可用的发货入口，本批可以通过真实后端 API 准备已发货订单来验证买家确认收货；不得因此实现 Batch 07 的团长订单发货页面。

### 6.4 前端修复重点

真实联调中发现下列问题必须在本批内修复，不得留到下一批：

- `GET /api/v1/my/store` 未开店时 200 但省略 `data`，前端必须兼容。
- `GET /api/v1/me` 中 `leader`、`store` 可能为空或省略，不能只依赖旧版 `user.hasLeader`。
- 后端金额为整数分，提交金额字段仍必须是 `xxxAmount` 整数，不得引入浮点元。
- 配送方式只使用 OpenAPI 枚举：`express`、`pickup`、`local_delivery`。
- 订单状态只使用 API 状态：`pendingPay`、`paid`、`shipped`、`completed`、`canceled`、`afterSale`。
- 时间字段按 ISO 8601 处理，页面展示可本地格式化，但请求体不得提交不可解析的本地化字符串。
- 所有表单提交、支付、取消、确认收货、创建店铺、编辑店铺操作必须有 loading / disabled 防重复点击。
- 后端错误响应必须展示 `error.message` 或错误码中文映射，不能吞错或只显示网络失败。
- 真实后端返回空列表时必须展示空态，不能因 `items` 为空而报错。

### 6.5 Playwright 配置

保留原有 mock E2E 的前提下，新增 Live E2E 运行方式。可选择以下任一方案，但必须在文档和 `package.json` 脚本中说明清楚：

方案 A：新增脚本。

```json
{
  "scripts": {
    "test:e2e:live": "playwright test tests/e2e/*.live.spec.ts"
  }
}
```

方案 B：使用环境变量控制。

```bash
E2E_LIVE=1 npm run test:e2e
```

无论采用哪种方案，普通 `npm run test:e2e` 不应在没有后端和数据库时突然失败；Live E2E 可以要求先启动后端，或在 Playwright `webServer` 中仅启动前端。

## 7. 测试要求

本批完成后必须运行：

```bash
cd frontend
npm run typecheck
npm run lint
npm run test:unit
npm run build
npm run test:e2e
npm run test:e2e:live
```

后端侧至少运行：

```bash
mvn test
```

如果 `test:e2e:live` 依赖外部 MySQL 或本地后端服务，必须在 handoff 中写明实际启动命令、数据库名、后端端口、前端端口、测试账号规则和失败重试方式。

## 8. 验收标准

- 前端 Batch 01-05 的核心链路可以在真实后端和真实数据库上跑通。
- Live E2E 覆盖认证、公开浏览、地址下单、订单支付/取消/确认收货、个人中心、一键开团、创建/编辑店铺。
- Live E2E 页面接口没有 Playwright route mock。
- 测试数据准备过程可重复执行，不依赖固定 ID，不污染必须手工清理的共享数据。
- `docs/前后端联调文档.md` 已补充真实联调运行方式和测试数据准备方式。
- 未实现的后续能力保持灰态或占位，不因联调而提前扩展 MVP 范围。

## 9. 联调文档更新项

更新 `docs/前后端联调文档.md`：

- 在通用约定中补充“真实前后端联调运行方式”，包含后端启动、前端启动、Vite proxy、环境变量、健康检查。
- 在测试数据总口径中补充 Live E2E 的账号命名规则、数据准备顺序、不得写死 ID。
- 在认证、公开浏览、地址下单、订单、个人中心/一键开团、创建店铺链路中补充真实联调注意事项。
- 新增 Batch 05.5 验证范围说明，明确本批只验证 Frontend Batch 01-05，不提前实现 Batch 06+ 页面能力。

## 10. 禁止事项

- 不实现商品管理 UI。
- 不实现发布团购 UI。
- 不实现团长订单发货 UI。
- 不实现订阅列表、会员卡页面或会员卡增强展示。
- 不接真实微信支付。
- 不把 Playwright route mock 当作本批 Live E2E 验收结果。
- 不硬编码后端返回的 ID。
- 不为了通过测试绕过登录拦截、真实接口、真实状态流转或非 MVP 灰态约束。
- 不修改 OpenAPI 已冻结字段名、状态枚举、金额单位或错误码口径；如发现实现差异，先记录差异并按文档口径修复。

## 11. 本批完成后必须输出的结果

- 已完成真实联调链路清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单，明确哪些是 Live E2E。
- 已更新联调文档的位置。
- 测试数据准备方式和测试账号规则。
- 测试运行命令和结果，包括 `npm run test:e2e:live`。
- 真实联调发现并修复的问题清单。
- 本批未解决问题，如无则写“无”。
