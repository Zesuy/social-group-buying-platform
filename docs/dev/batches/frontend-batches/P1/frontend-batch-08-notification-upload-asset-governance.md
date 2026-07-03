# Frontend P1 Batch 08：站内通知和上传资产前端联调

## 1. 目标

将当前消息页从占位卡片升级为真实站内通知页，通过 REST 轮询展示未读数和通知列表；同时兼容图片上传接口新增的 `assetId`、`status` 字段，为后端上传资产治理联调提供前端入口。

本批与后端 `docs/dev/batches/backend-batches/P1/batch-08-notification-upload-asset-governance.md` 建议同批执行。后端负责通知生成、未读数、已读状态和图片资产清理；前端负责消息页、Tabbar 角标、轮询、点击跳转和上传响应兼容。

## 2. 需求澄清

| 项目 | 结论 |
|---|---|
| 页面目标 | `/messages` 展示当前登录用户的站内通知，支持查看未读、标记已读和跳转业务对象 |
| 使用角色 | 已登录买家和团长可查看自己的通知；未登录用户访问消息页需先登录 |
| 页面入口 | 底部 Tabbar“消息”、通知点击后的业务页返回、个人中心消息入口（如存在） |
| MVP / P1 边界 | 本批做站内通知，不做公众号推送、微信服务通知、WebSocket、客服聊天 |
| 业务对象 | 通知关联订单、团购、团长或店铺，通过 `targetType`、`targetId`、`actionUrl` 跳转 |
| 数据字段 | `id`、`type`、`title`、`summary`、`targetType`、`targetId`、`actionUrl`、`readStatus`、`createdAt`、`unreadCount` |
| 接口依赖 | 通知列表、未读数、通知详情、标记单条已读、全部已读、图片上传 |
| 权限边界 | `/messages` 改为 `requiresAuth`；未登录点击 Tabbar 进入登录拦截，登录后回到消息页 |
| 状态场景 | loading、empty、error、下拉/按钮重试、未读筛选、标记已读中、防重复点击 |
| 待确认问题 | 是否需要通知详情独立页；本批默认不新增详情页，点击通知直接跳 `actionUrl` |

## 3. 涉及接口

新增前端 API 封装：

- `GET /api/v1/my/notifications?page=&pageSize=&type=&unreadOnly=`
- `GET /api/v1/my/notifications/unread-count`
- `GET /api/v1/my/notifications/{notificationId}`
- `POST /api/v1/my/notifications/{notificationId}/read`
- `POST /api/v1/my/notifications/read-all`

更新既有上传 API 类型：

- `POST /api/v1/my/uploads/images` 响应兼容 `assetId`、`status`。

## 4. 页面范围

- `/messages`：真实通知列表页。
- `BottomTabBar`：消息 Tab 未读角标。
- 全局应用布局或组合逻辑：登录状态下轮询未读数。
- 图片上传相关表单：不改变交互，只保证新增字段不破坏类型和测试。

不新增聊天页，不新增图片管理页，不新增通知运营后台。

## 5. 前端技术方案

### 5.1 页面结构

`MessagesView` 页面结构：

1. 顶部标题和未读数；
2. Tab 筛选：全部、未读、订单、活动；
3. 公众号提示条保留为“后续开放”，不请求公众号接口；
4. 通知列表；
5. loading / empty / error 状态；
6. “全部已读”次要操作。

消息卡片应展示：

- 类型图标或状态色；
- 标题；
- 摘要；
- 时间；
- 未读点；
- 与订单 / 团购相关的轻量提示。

### 5.2 组件和文件拆分

建议新增：

- `frontend/src/api/notifications.ts`
- `frontend/src/composables/useNotificationPolling.ts`
- `frontend/src/types/notification.ts` 或合并到 `frontend/src/types/api.ts`
- `frontend/src/components/NotificationListItem.vue`

建议修改：

- `frontend/src/views/MessagesView.vue`
- `frontend/src/components/BottomTabBar.vue`
- `frontend/src/router/index.ts`
- `frontend/src/api/uploads.ts`
- `frontend/src/types/api.ts`
- `frontend/tests/unit/messages-view.test.ts`
- `frontend/tests/unit/bottom-tab-bar.test.ts`
- `frontend/tests/unit/uploads-api.test.ts`
- `frontend/tests/e2e/smoke.spec.ts` 或新增通知联调 E2E

### 5.3 数据流和轮询

- 登录后每 30 秒请求一次 `unread-count`。
- 进入 `/messages` 时请求通知列表第一页。
- 页面处于隐藏状态、用户未登录、token 失效时暂停轮询。
- 消息页激活时可在标记已读后立即刷新未读数。
- 轮询失败不弹全局 toast，避免打扰；消息页内展示错误态和重试。

### 5.4 点击跳转

点击通知：

1. 如果 `readStatus=unread`，先调用标记已读；
2. 标记已读成功后更新本地状态和未读数；
3. 按 `actionUrl` 跳转；
4. 标记已读失败时允许继续跳转，但需要在控制台或页面弱提示记录失败。

`actionUrl` 必须限制为站内路径，前端不跳外链。未知 `targetType` 或空 `actionUrl` 时进入通知详情或保持当前页提示“关联内容暂不可查看”。本批默认优先使用 `actionUrl`。

## 6. 视觉和交互要求

- 遵守 `DESIGN.md` 和 `docs/前端产品与页面设计准则.md`。
- 消息页表达私域团购的订单履约和复购触达，不做社交聊天界面。
- 只突出一个主操作：空态时引导“去看看团购”；列表态中“全部已读”为次要操作。
- 通知卡片不做大面积装饰，不使用聊天气泡布局。
- Tabbar 未读角标只展示数字或红点，不挤压“消息”文字。
- 移动端 320px 宽度下标题、摘要、时间不得重叠。
- 公众号提示条保留“后续开放”语义，不能像已接入真实推送。

## 7. 实现任务

1. 定义通知类型和 API 封装。
2. 将 `/messages` 路由改为需要登录。
3. 改造 `MessagesView`，移除占位静态消息，接入真实通知列表。
4. 实现未读 / 类型筛选、loading、empty、error、retry。
5. 实现单条标记已读和全部已读。
6. 实现未读数轮询 composable，并接入 Tabbar 角标。
7. 更新上传响应类型，兼容 `assetId` 和 `status`。
8. 更新单元测试和 E2E，把“消息页不请求 API”的旧断言改为真实接口断言。
9. 更新 `docs/前后端联调文档.md` 中消息页入口和前端调用顺序。

## 8. 测试要求

单元测试覆盖：

- `notifications` API 封装路径和参数；
- 消息页 loading、empty、error、列表态；
- 未读筛选和类型筛选；
- 点击未读通知会调用标记已读并跳转；
- 全部已读按钮；
- 未登录访问 `/messages` 触发登录拦截；
- Tabbar 消息未读角标；
- 上传 API 新增字段兼容。

E2E 覆盖：

- 未登录点击消息 Tab 跳转登录，登录后回到 `/messages`；
- 支付 / 发货 / 确认收货事件后消息页出现对应通知；
- 点击发货通知进入订单详情；
- 标记已读后未读数变化；
- 公众号推送入口仍为后续开放提示，不请求真实公众号接口。

必须运行：

```bash
cd frontend
npm run typecheck
npm run lint
npm run test:unit
npm run build
npm run test:e2e
```

## 9. 验收标准

消息页不再展示硬编码占位消息，而是展示当前登录用户真实通知；Tabbar 能显示未读状态；用户可以筛选未读、标记已读并跳转订单或团购等业务对象；未登录用户不能直接访问消息页；上传图片接口新增字段不影响现有店铺、商品和团购图片上传流程。

前端不实现公众号推送、客服聊天、图片管理后台和 WebSocket。所有通知状态必须来自后端接口，不能用前端假数据替代。

## 10. 联调文档更新项

更新 `docs/前后端联调文档.md`：

- 消息页 `/messages` 从占位改为真实站内通知；
- 补充未登录拦截；
- 补充未读数轮询；
- 补充支付、发货、确认收货、订阅、发布团购后的前端验证步骤；
- 补充点击通知跳转规则；
- 更新上传响应字段 `assetId`、`status` 的前端兼容说明。

## 11. 禁止事项

不做真实公众号推送、微信服务通知、WebSocket、SSE、商家用户聊天、在线客服、图片素材库、图片删除管理页、平台运营消息后台；不保留“消息页不请求 API”的旧逻辑；不为了通过 E2E 使用前端假通知绕过真实业务事件。

## 12. 本批完成后必须输出的结果

- 已实现页面 / 能力清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
