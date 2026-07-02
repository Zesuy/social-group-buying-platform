# API 风格规范

> 本文定义本项目 API 的统一风格。后续 `API设计.md`、前端联调、后端实现应遵循本文。
>
> 范围：MVP 阶段以用户、团长、店铺、团购、商品、订单、地址、订阅、会员关系为核心；P1/P2 能力只保留扩展口径，不在 MVP 中展开。

---

## 1. 总体风格

本项目 API 采用 REST 风格，以资源为中心设计接口。

| 规则 | 说明 |
|---|---|
| 协议 | HTTP / HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 接口版本 | 路径中包含版本号，如 `/api/v1` |
| 资源命名 | 使用复数名词，如 `/users`、`/stores`、`/group-buys` |
| 字段命名 | 请求和响应字段统一使用 `camelCase` |
| 时间格式 | ISO 8601 字符串，如 `2026-06-24T12:00:00+08:00` |
| 金额格式 | API 层全部使用整数分，字段名必须以 `Amount` 结尾 |
| ID 格式 | 文档中使用数字 ID，实际实现可替换为字符串 ID |

---

## 2. 路径规范

### 2.1 基础路径

```text
/api/v1
```

### 2.2 资源路径

| 场景 | 示例 |
|---|---|
| 资源列表 | `GET /api/v1/group-buys` |
| 资源详情 | `GET /api/v1/group-buys/{groupBuyId}` |
| 创建资源 | `POST /api/v1/group-buys` |
| 更新资源 | `PATCH /api/v1/group-buys/{groupBuyId}` |
| 删除资源 | `DELETE /api/v1/products/{productId}` |
| 子资源列表 | `GET /api/v1/stores/{storeId}/products` |
| 资源动作 | `POST /api/v1/orders/{orderId}/simulate-pay` |

### 2.3 `/my/store` 路径语义

`/my/store` 表示当前登录团长自己的店铺。

| 规则 | 说明 |
|---|---|
| 身份来源 | 服务端必须从 `Authorization` 对应的 token 获取当前用户身份 |
| 店铺来源 | 服务端根据当前用户的团长身份查找自己的店铺 |
| 客户端限制 | 所有 `/my/store/**` 接口不允许客户端通过 `storeId` 决定操作对象 |
| 权限校验 | 即使路径中有 `productId`、`groupBuyId`、`orderId`，也必须校验资源是否属于当前团长自己的店铺 |

### 2.4 动作命名

REST 资源优先使用标准方法；当业务动作无法自然表达为 CRUD 时，使用动作路径。

| 动作 | 推荐路径 |
|---|---|
| 发布团购 | `POST /api/v1/group-buys/{groupBuyId}/publish` |
| 结束团购 | `POST /api/v1/group-buys/{groupBuyId}/end` |
| 模拟支付 | `POST /api/v1/orders/{orderId}/simulate-pay` |
| 取消订单 | `POST /api/v1/orders/{orderId}/cancel` |
| 确认收货 | `POST /api/v1/orders/{orderId}/complete` |
| 订单发货 | `POST /api/v1/orders/{orderId}/ship` |
| 订阅团长 | `POST /api/v1/leaders/{leaderId}/subscription` |
| 取消订阅 | `DELETE /api/v1/leaders/{leaderId}/subscription` |

---

## 3. HTTP 方法

| 方法 | 用途 | 是否有请求体 |
|---|---|---|
| GET | 查询资源 | 否 |
| POST | 创建资源或执行业务动作 | 是 |
| PATCH | 局部更新资源 | 是 |
| DELETE | 删除资源或取消关系 | 通常否 |

不使用 `PUT` 作为 MVP 默认更新方式，避免要求客户端提交完整资源。

---

## 4. 认证与当前用户

MVP 使用登录态令牌表达用户身份，具体登录方式可后续接入微信授权。

### 4.1 请求头

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Idempotency-Key（P1 Batch 03 正式规则）：

```http
Idempotency-Key: <client-generated-unique-key>
```

| 规则 | 说明 |
|---|---|
| 使用条件 | 可选，仅在 `Idempotency-Key` 请求头存在时启用；未传时保持旧行为 |
| 幂等维度 | `userId + HTTP method + 实际请求路径 + Idempotency-Key` |
| 请求体校验 | 首次请求保存请求体 SHA-256 hash；重复请求 hash 不一致返回 `IDEMPOTENCY_KEY_MISMATCH` |
| 成功重放 | 首次成功后重复请求返回首次响应 data |
| 失败重放 | 首次失败后重复请求返回首次错误 |
| 处理中超时 | 首次请求处理中：等待 500ms 重读；仍未完成返回 `IDEMPOTENCY_IN_PROCESSING` |
| 作用域 | 只对同一用户有效；不同用户使用相同 key 独立计算 |
| 适用接口 | `POST /api/v1/orders`、`POST /api/v1/orders/{orderId}/simulate-pay`、`POST /api/v1/orders/{orderId}/cancel`、`POST /api/v1/orders/{orderId}/complete`、`POST /api/v1/my/store/orders/{orderId}/ship` |

### 4.2 登录要求

| 操作 | 是否需要登录 |
|---|---:|
| 查看首页团购列表 | 否 |
| 查看团购详情 | 否 |
| 查看团长主页 | 否 |
| 购物车管理 | 是 |
| 购物车结算预览 | 是 |
| 下单 | 是 |
| 查看自己的订单 | 是 |
| 创建店铺 | 是 |
| 发布团购 | 是，且必须是团长 |
| 管理商品 | 是，且只能管理自己的店铺商品 |
| 发货 | 是，且只能处理自己的店铺订单 |
| 订阅团长 | 是 |
| 查看会员卡 | 是 |

---

## 5. 响应结构

### 5.1 成功响应

所有成功响应统一包裹在 `data` 中。

```json
{
  "success": true,
  "data": {},
  "traceId": "req_202606241200000001"
}
```

列表响应：

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0,
    "hasMore": false
  },
  "traceId": "req_202606241200000001"
}
```

### 5.2 空响应

删除、取消订阅等无需返回实体的操作：

```json
{
  "success": true,
  "data": null,
  "traceId": "req_202606241200000001"
}
```

### 5.3 错误响应

```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_PAYABLE",
    "message": "当前订单不能支付",
    "details": {}
  },
  "traceId": "req_202606241200000001"
}
```

---

## 6. 分页、排序与筛选

### 6.1 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---:|---|
| page | number | 1 | 页码，从 1 开始 |
| pageSize | number | 20 | 每页数量，建议最大 100 |

### 6.2 排序参数

| 参数 | 示例 | 说明 |
|---|---|---|
| sort | `createdAt.desc` | 字段和方向用点连接 |

### 6.3 筛选参数

筛选使用明确字段，不使用复杂表达式。

```text
GET /api/v1/orders?status=paid&page=1&pageSize=20
GET /api/v1/group-buys?storeId=1&status=published
```

---

## 7. 状态字段规范

API 状态字段使用 `camelCase` 英文枚举，页面展示时再转换为中文。

如果数据库使用 `snake_case` 枚举，需要在 DTO 层做转换，不能把数据库枚举原样泄露到 API。

| 业务含义 | 数据库示例 | API 示例 |
|---|---|---|
| 待支付 | `pending_pay` | `pendingPay` |
| 售后中 | `after_sale` | `afterSale` |
| 已发布 | `published` | `published` |
| 已退款 | `refunded` | `refunded` |

### 7.1 店铺状态

| 枚举 | 说明 |
|---|---|
| active | 已创建 / 正常 |
| disabled | 已停用，P2 |

### 7.2 团购状态

| 枚举 | 说明 |
|---|---|
| draft | 草稿，P1 |
| published | 已发布 |
| ended | 已结束 |
| removed | 已下架，P2 |

### 7.3 订单状态

| 枚举 | 说明 |
|---|---|
| pendingPay | 待支付 |
| paid | 已支付 / 待发货 |
| shipped | 已发货 |
| completed | 已完成 |
| canceled | 已取消 |
| afterSale | 售后中，P1 |
| refunded | 已退款，P1 |

### 7.4 支付状态

| 枚举 | 说明 |
|---|---|
| unpaid | 未支付 |
| paid | 已支付 |
| refunded | 已退款，P1 |

### 7.5 售后状态

| 枚举 | 说明 |
|---|---|
| pending | 待审核 |
| approved | 审核通过 |
| rejected | 已拒绝 |
| completed | 已退款 |

---

## 8. 错误码规范

错误码使用大写蛇形命名。

| HTTP 状态 | 错误码示例 | 场景 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录或令牌无效 |
| 403 | `FORBIDDEN` | 无权限操作 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在 |
| 409 | `RESOURCE_CONFLICT` | 状态冲突或重复创建 |
| 422 | `BUSINESS_RULE_VIOLATION` | 不满足业务规则 |
| 500 | `INTERNAL_ERROR` | 服务端异常 |

业务错误码示例：

| 错误码 | 说明 |
|---|---|
| `STORE_ALREADY_EXISTS` | 当前用户已创建店铺 |
| `LEADER_REQUIRED` | 当前操作需要团长身份 |
| `STORE_FORBIDDEN` | 不能操作他人店铺 |
| `GROUP_BUY_NOT_PURCHASABLE` | 团购不可购买 |
| `GROUP_BUY_ENDED` | 团购已结束 |
| `ITEM_NOT_IN_GROUP_BUY` | 下单商品不属于该团购 |
| `ADDRESS_FORBIDDEN` | 收货地址不属于当前用户 |
| `ORDER_NOT_PAYABLE` | 订单不可支付 |
| `ORDER_ALREADY_PAID` | 订单已支付，重复支付请求不应再次扣库存 |
| `ORDER_NOT_CANCELABLE` | 订单不可取消 |
| `ORDER_NOT_SHIPPABLE` | 订单不可发货 |
| `ORDER_ALREADY_SHIPPED` | 订单已发货，重复发货请求不应再次创建发货记录 |
| `INSUFFICIENT_STOCK` | 团购商品库存不足 |
| `SUBSCRIPTION_EXISTS` | 已订阅该团长 |

---

## 9. 字段约定

### 9.1 通用字段

| 字段 | 说明 |
|---|---|
| id | 资源 ID |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |
| status | 业务状态 |

### 9.2 金额字段

API 层全部使用整数分，字段名必须以 `Amount` 结尾：

| 字段 | 示例 | 说明 |
|---|---:|---|
| priceAmount | 1990 | 19.90 元 |
| totalAmount | 3980 | 39.80 元 |
| discountAmount | 0 | MVP 不做优惠，默认 0 |
| payAmount | 3980 | 实付金额 |

数据库与 API 金额映射：

| 层级 | 建议 | 说明 |
|---|---|---|
| API DTO | 整数分，字段以 `Amount` 结尾 | 例如 `payAmount: 3980` |
| 数据库 | 推荐使用 `bigint` 保存分 | 后端实现最简单，避免浮点精度问题 |
| 若数据库使用 decimal | DTO 输出前必须转换为整数分 | 不允许 API 返回 decimal 字符串或浮点数 |

### 9.3 快照字段

订单明细必须保存快照字段，不能只引用商品 ID。

| 字段 | 说明 |
|---|---|
| productName | 下单时商品名称 |
| skuName | 下单时规格名称 |
| unitPriceAmount | 下单时单价 |
| quantity | 下单数量 |
| totalAmount | 明细总额 |

### 9.4 地址快照字段

订单创建时必须保存收货地址快照，不能只保存 `addressId`。

| 字段 | 说明 |
|---|---|
| receiverName | 下单时收货人 |
| receiverPhone | 下单时收货手机号 |
| province | 下单时省份 |
| city | 下单时城市 |
| district | 下单时区县 |
| detail | 下单时详细地址 |
| fullAddress | 下单时完整拼接地址 |

用户后续修改或删除地址，不得影响历史订单展示和发货。

---

## 10. 幂等规则

MVP 不建设独立幂等表，也不要求支持 `Idempotency-Key`。重复提交主要通过前端防重复点击、后端状态校验和数据库事务控制；`Idempotency-Key` 明确作为 P1 增强能力。

| 接口 | MVP 落地方式 |
|---|---|
| `POST /api/v1/orders` | MVP 不做强幂等，前端需要防重复点击；后端通过业务校验和事务创建订单，避免单次请求产生不完整订单 |
| `POST /api/v1/orders/{orderId}/simulate-pay` | 仅允许 `orderStatus = pendingPay` 且 `payStatus = unpaid` 的订单支付；支付、扣库存、增销量、更新会员关系在同一事务中完成 |
| `POST /api/v1/orders/{orderId}/cancel` | 仅允许 `orderStatus = pendingPay` 且 `payStatus = unpaid` 的订单取消 |
| `POST /api/v1/my/store/orders/{orderId}/ship` | 仅允许 `orderStatus = paid` 的订单发货；发货记录创建和订单状态更新在同一事务中完成 |

P1 可增强支持 `Idempotency-Key` 请求头：

| 接口 | P1 幂等要求 |
|---|---|
| `POST /api/v1/orders` | 重复提交不能重复创建订单 |
| `POST /api/v1/orders/{orderId}/simulate-pay` | 重复提交不能重复支付、重复扣库存或重复更新会员关系 |
| `POST /api/v1/orders/{orderId}/cancel` | 重复提交不能造成异常状态 |
| `POST /api/v1/my/store/orders/{orderId}/ship` | 重复提交不能重复创建发货记录 |

P1 处理建议：

- 客户端对一次业务提交生成唯一 `Idempotency-Key`。
- 服务端以当前用户、接口路径、`Idempotency-Key` 作为幂等识别维度。
- 同一个幂等键重复请求时，返回第一次请求的结果或当前等价状态。
- 不允许用幂等键绕过权限校验。

---

## 11. 权限边界

API 必须在服务端校验权限，不能只依赖前端隐藏入口。

| 规则 | 说明 |
|---|---|
| 当前用户只能查看自己的私有订单 | 团长订单除外 |
| 团长只能管理自己的店铺 | 通过店铺归属校验 |
| 团长只能管理自己店铺下的商品 | 不能跨店铺操作 |
| 团长只能管理自己店铺发布的团购 | 不能发布到他人店铺 |
| 团长只能处理自己店铺产生的订单 | 发货、查看团长订单均需校验 |

---

## 12. MVP 不设计的接口

以下能力在目标产品中重要，但 MVP API 不展开：

| 能力 | 处理方式 |
|---|---|
| 真实微信支付 | 仅提供模拟支付接口 |
| 售后退款完整流程 | 只保留状态，P1 再设计售后接口 |
| 优惠券 / 红包 | P1 |
| 商品库独立管理 | P1 |
| 购物车 | P1 |
| 帮卖分销和佣金 | P2 |
| 积分商城 | P2 |
| 公众号推送 / 订阅邀请卡 | P2 |
| 平台管理后台 | P2 |
