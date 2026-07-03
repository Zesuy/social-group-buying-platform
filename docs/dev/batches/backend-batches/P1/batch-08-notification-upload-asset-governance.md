# P1 Batch 8：站内通知和上传资产治理

## 1. 目标

把现有消息页从“视觉占位”推进为真实站内通知能力，并为已经落到本地 `uploads` 目录的图片补齐资产登记、引用追踪和超限清理机制。

本批只做站内通知和上传资产治理，不做真实公众号推送、微信服务通知、WebSocket、在线客服系统或完整聊天会话。前端可通过轮询获取未读数和通知列表。

本批建议与前端 `docs/dev/batches/frontend-batches/P1/frontend-batch-08-notification-upload-asset-governance.md` 同批联调执行。后端完成通知生成和资产引用，前端同步替换消息页占位数据、接入未读轮询和 Tabbar 角标。

## 2. 是否与聊天分开

本批必须与“商家和用户聊天”分开。

原因：

- 站内通知是订单、发货、订阅和开团事件的系统消息，核心是事件触发、幂等生成、已读状态和跳转目标。
- 聊天是用户之间的会话系统，核心是会话成员、消息发送、双方未读数、图片消息和权限边界。
- 两者都可能用图片，但图片资产引用登记是共同底座，应先在本批完成。
- 分开后，旧文档中的 P2 消息/客服方向仍保留价值；本批只是先落地“不依赖外部平台的站内通知”。

后续聊天批次可复用本批的 `upload_assets`、图片引用服务、未读数轮询和消息页基础交互。

## 3. 前置条件

P1 Batch 1 到 Batch 7 已完成；订单创建、模拟支付、团长发货、确认收货、订阅关系、团购发布、公开团购详情和图片上传链路稳定。

当前图片上传接口 `POST /api/v1/my/uploads/images` 已返回 `url`、`objectKey`、`contentType` 和 `size`，并将文件保存到本地 `uploads/images/yyyy/MM/`。

## 4. 涉及接口

### 4.1 上传资产接口增强

增强既有接口：

- `POST /api/v1/my/uploads/images`

响应新增字段：

- `assetId`：上传资产 ID，JSON string。
- `status`：`temporary` / `in_use` / `deleted`。

保持兼容：

- 继续返回 `url`、`objectKey`、`originalFilename`、`contentType`、`size`。
- 旧前端只使用 `url` 时不应断裂。

### 4.2 我的站内通知

新增登录接口：

- `GET /api/v1/my/notifications?page=&pageSize=&type=&unreadOnly=`
- `GET /api/v1/my/notifications/unread-count`
- `GET /api/v1/my/notifications/{notificationId}`
- `POST /api/v1/my/notifications/{notificationId}/read`
- `POST /api/v1/my/notifications/read-all`

### 4.3 管理端清理任务触发

本批不建设平台后台页面。可选提供测试或内部端点时，必须放在 `_test` 或仅 test profile 下，不暴露生产管理 API。

## 5. 数据模型调整

### 5.1 upload_assets

新增表 `upload_assets`：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 资产 ID |
| uploader_user_id | bigint | 上传用户 |
| object_key | varchar(512) | 相对 `uploads` 根目录的对象键，如 `images/2026/07/uuid.png` |
| url | varchar(512) | 公开访问 URL |
| original_filename | varchar(255) | 原始文件名 |
| content_type | varchar(64) | `image/jpeg` / `image/png` / `image/webp` |
| size_bytes | bigint | 文件大小 |
| checksum_sha256 | varchar(64) | 文件 SHA-256，用于排查和后续去重 |
| status | varchar(20) | `temporary` / `in_use` / `deleted` |
| reference_count | int | 当前引用数量 |
| last_referenced_at | datetime | 最近引用时间 |
| deleted_at | datetime | 删除时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

约束和索引：

- `object_key` 唯一。
- `idx_upload_assets_uploader`：`uploader_user_id, created_at`。
- `idx_upload_assets_status_created`：`status, created_at`。
- `idx_upload_assets_cleanup`：`status, reference_count, created_at`。

### 5.2 upload_asset_references

新增表 `upload_asset_references`：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 引用 ID |
| asset_id | bigint | 上传资产 ID |
| object_key | varchar(512) | 冗余对象键，便于清理排查 |
| ref_type | varchar(64) | `store` / `leader` / `product` / `group_buy` / `group_buy_content` / `notification` |
| ref_id | bigint | 业务对象 ID |
| field_name | varchar(64) | 引用字段，如 `logoUrl`、`coverImageUrl`、`galleryImageUrls`、`contentBlocks` |
| created_at | datetime | 创建时间 |

约束和索引：

- 唯一约束：`asset_id, ref_type, ref_id, field_name`。
- 索引：`ref_type, ref_id`。

### 5.3 notifications

新增表 `notifications`：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 通知 ID |
| recipient_user_id | bigint | 接收用户 |
| sender_user_id | bigint | 触发用户，可为空 |
| type | varchar(32) | `order_paid` / `order_shipped` / `order_completed` / `group_buy_published` / `subscription_created` |
| title | varchar(80) | 标题 |
| summary | varchar(200) | 列表摘要 |
| body | text | 详情正文 |
| target_type | varchar(32) | `order` / `group_buy` / `leader` / `store` |
| target_id | bigint | 关联对象 ID |
| action_url | varchar(255) | 前端跳转路径，如 `/orders/{id}` |
| dedupe_key | varchar(128) | 幂等键，防止重复事件生成重复通知 |
| read_status | varchar(16) | `unread` / `read` |
| read_at | datetime | 已读时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

约束和索引：

- `dedupe_key` 唯一。
- `idx_notifications_recipient_created`：`recipient_user_id, created_at`。
- `idx_notifications_recipient_read`：`recipient_user_id, read_status, created_at`。
- `idx_notifications_target`：`target_type, target_id`。

## 6. 通知事件来源

本批至少接入以下事件：

| 事件 | 触发点 | 接收人 | 类型 | 跳转 |
|---|---|---|---|---|
| 买家模拟支付成功 | `simulatePay` 事务成功后 | 买家 | `order_paid` | `/orders/{orderId}` |
| 买家模拟支付成功 | `simulatePay` 事务成功后 | 团长对应用户 | `order_paid` | `/leader/orders/{orderId}` |
| 团长发货成功 | `shipOrder` 事务成功后 | 买家 | `order_shipped` | `/orders/{orderId}` |
| 买家确认收货 | `completeOrder` 事务成功后 | 团长对应用户 | `order_completed` | `/leader/orders/{orderId}` |
| 用户订阅团长 | 订阅关系创建或重新激活后 | 团长对应用户 | `subscription_created` | `/leaders/{leaderId}` |
| 团长发布团购 | 团购发布成功后 | 该团长活跃订阅用户 | `group_buy_published` | `/group-buys/{groupBuyId}` |

幂等要求：

- 重复支付、重复发货、重复确认收货不能生成重复通知。
- `dedupeKey` 建议格式：`eventType:businessId:recipientUserId`。
- 通知生成必须在业务事务成功后执行；若使用同事务写入，必须保证业务回滚时通知也回滚。

## 7. API 契约

### 7.1 通知列表

```http
GET /api/v1/my/notifications?page=1&pageSize=20&type=order_shipped&unreadOnly=true
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "2071642363297644546",
        "type": "order_shipped",
        "title": "发货通知",
        "summary": "团长已填写物流：顺丰速运 SF1234567890。",
        "targetType": "order",
        "targetId": "2071642363297644547",
        "actionUrl": "/orders/2071642363297644547",
        "readStatus": "unread",
        "createdAt": "2026-07-03T12:00:00+08:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "hasMore": false
  },
  "traceId": "req_001"
}
```

### 7.2 未读数

```http
GET /api/v1/my/notifications/unread-count
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  },
  "traceId": "req_001"
}
```

### 7.3 标记已读

```http
POST /api/v1/my/notifications/{notificationId}/read
Authorization: Bearer <accessToken>
```

响应：返回更新后的通知详情。

权限要求：

- 只能读取和标记自己的通知。
- 非接收人访问返回 `NOTIFICATION_FORBIDDEN` 或通用权限错误。

## 8. 上传资产引用规则

上传接口只创建 `temporary` 资产，不直接视为业务引用。

以下业务保存成功后，必须同步登记引用：

- 店铺 `logoUrl`。
- 团长 `avatarUrl`。
- 商品 `coverImageUrl`、`detailImageUrls`。
- 团购 `coverImageUrl`、`galleryImageUrls`、`contentBlocks` 中的图片 URL。
- 通知中如后续包含图片字段，也必须登记引用。

引用登记要求：

- 只登记本系统上传返回的 URL 或 `objectKey`，外部 `http/https` 图片不纳入本地清理。
- 同一业务对象更新图片字段时，必须先释放旧引用，再登记新引用。
- 引用计数必须与 `upload_asset_references` 保持一致。
- 业务保存失败时不得留下新的引用记录。
- 已被引用的图片不得被清理任务删除。

## 9. 上传目录清理策略

新增配置项：

```yaml
groupshop:
  upload:
    max-total-size-mb: 1024
    temporary-retention-hours: 24
    cleanup-batch-size: 100
```

清理触发：

- 可使用 Spring `@Scheduled` 周期任务。
- 测试中允许直接调用 Service 方法验证。

清理规则：

1. 统计 `upload_assets` 中未删除文件的总大小，必要时可与文件系统实际大小交叉校验。
2. 未超过 `maxTotalSizeMb` 时不删除。
3. 超过阈值时，只删除满足以下条件的资产：
   - `status = temporary` 或 `referenceCount = 0`；
   - 创建时间早于 `temporaryRetentionHours`；
   - 没有 `upload_asset_references`；
   - 文件路径规范化后仍位于 `upload.localDir` 下。
4. 按 `createdAt` 从旧到新、每批最多 `cleanupBatchSize` 删除。
5. 文件删除成功后将资产状态更新为 `deleted`，记录 `deletedAt`。
6. 文件不存在但数据库为未删除状态时，可标记为 `deleted` 并记录日志。
7. 删除失败不得影响业务主流程，应记录日志并留待下次重试。

安全要求：

- 禁止根据客户端传入路径删除文件。
- 禁止删除 `uploads` 根目录外的任何文件。
- 禁止删除已有引用的图片。
- 清理任务不得扫描或删除非本系统登记的业务文件，除非后续明确设计迁移策略。

## 10. 前端轮询约定

后端只提供普通 REST 接口，前端轮询建议如下：

- 底部 Tab 或全局布局：每 30 秒调用一次 `GET /api/v1/my/notifications/unread-count`。
- 消息页可每 15 到 30 秒刷新列表第一页。
- 页面隐藏、用户未登录或 token 失效时暂停轮询。
- 点击通知先调用标记已读，再按 `actionUrl` 跳转；标记失败不应阻止用户查看业务对象。

## 11. 实现任务

新增上传资产模块 `upload` 内的资产实体、Mapper、Service、清理 Service 和测试；改造 `ImageUploadService`，上传成功写入 `upload_assets`。

新增通知模块 `notification`，包含 Controller、DTO、Service、事件生成方法和查询已读接口。

在订单支付、团长发货、确认收货、订阅、团购发布成功路径中接入通知生成。事件接入必须保持原有业务接口响应兼容，不改变订单、库存、会员、订阅和团购状态口径。

同步更新 `docs/API设计.md`、`docs/数据模型设计.md`、`docs/前后端联调文档.md` 和 OpenAPI 文档。

## 12. 测试要求

MockMvc 覆盖：

- 上传图片返回 `assetId`，旧字段仍存在；
- 未登录上传拒绝；
- 通知列表分页、类型筛选、未读筛选；
- 未读数接口；
- 通知详情；
- 标记单条已读；
- 全部标记已读；
- 访问他人通知拒绝；
- 未登录访问通知接口拒绝；
- 支付成功后买家和团长收到通知；
- 发货成功后买家收到通知；
- 确认收货后团长收到通知；
- 订阅团长后团长收到通知；
- 发布团购后活跃订阅用户收到通知。

Service 覆盖：

- 通知 `dedupeKey` 防重复；
- 通知生成事务回滚不留下脏数据；
- 上传资产登记；
- 图片 URL 到 `objectKey` 的解析；
- 业务字段更新时释放旧引用并登记新引用；
- 外部图片 URL 不登记本地引用；
- 清理任务只删除超期无引用资产；
- 清理任务不删除有引用资产；
- 清理任务路径穿越防护；
- 文件不存在时数据库状态修正；
- 清理阈值、批大小和保护期边界。

回归测试至少覆盖订单创建、模拟支付、库存扣减、发货、确认收货、订阅、团购发布、图片上传、团购内容块图片字段。

## 13. 验收标准

消息页可以通过 REST 轮询展示真实通知和未读数；订单支付、发货、确认收货、订阅和开团事件都能生成幂等通知；用户只能查看自己的通知；上传图片都有资产记录；已引用图片不会被清理；超过配置容量时，系统只清理超期且无引用的本地上传图片。

旧图片 URL 字段仍可正常展示，旧前端不因上传响应新增字段而断裂。真实公众号推送、微信服务通知、WebSocket 和商家用户聊天仍不在本批范围。

## 14. 联调文档更新项

新增“站内通知轮询”链路；更新“文件上传 v1”为“文件上传和资产治理”；补充支付、发货、确认收货、订阅、发布团购后的通知校验步骤。

联调文档必须明确：

- 消息页 `/messages` 改为请求真实通知接口；
- 公众号推送仍是后续能力，不在本批接入；
- 前端轮询频率和暂停条件；
- 通知点击跳转规则；
- 上传图片保存业务对象后才算引用；
- 超期无引用图片可被清理。

## 15. 禁止事项

不接真实公众号推送、微信服务通知、短信、邮件、WebSocket、SSE、在线客服分配、聊天撤回、客服评价、敏感词审核、平台后台消息运营；不删除有引用图片；不根据客户端路径删除文件；不改变订单金额、支付、库存、售后、优惠券、会员成长、距离计算和团购内容块口径。

## 16. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
