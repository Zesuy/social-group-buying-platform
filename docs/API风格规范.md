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
| 金额格式 | 使用整数分或 decimal 字符串二选一；MVP 建议使用整数分，字段名以 `Amount` 结尾 |
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

### 2.3 动作命名

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

### 4.2 登录要求

| 操作 | 是否需要登录 |
|---|---:|
| 查看首页团购列表 | 否 |
| 查看团购详情 | 否 |
| 查看团长主页 | 否 |
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

状态字段使用英文枚举，页面展示时再转换为中文。

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

### 7.4 支付状态

| 枚举 | 说明 |
|---|---|
| unpaid | 未支付 |
| paid | 已支付 |
| refunded | 已退款，P1 |

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
| `ORDER_NOT_PAYABLE` | 订单不可支付 |
| `ORDER_NOT_SHIPPABLE` | 订单不可发货 |
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

MVP 建议金额使用整数分：

| 字段 | 示例 | 说明 |
|---|---:|---|
| priceAmount | 1990 | 19.90 元 |
| totalAmount | 3980 | 39.80 元 |
| discountAmount | 0 | MVP 不做优惠，默认 0 |
| payAmount | 3980 | 实付金额 |

### 9.3 快照字段

订单明细必须保存快照字段，不能只引用商品 ID。

| 字段 | 说明 |
|---|---|
| productName | 下单时商品名称 |
| skuName | 下单时规格名称 |
| unitPriceAmount | 下单时单价 |
| quantity | 下单数量 |
| totalAmount | 明细总额 |

---

## 10. 权限边界

API 必须在服务端校验权限，不能只依赖前端隐藏入口。

| 规则 | 说明 |
|---|---|
| 当前用户只能查看自己的私有订单 | 团长订单除外 |
| 团长只能管理自己的店铺 | 通过店铺归属校验 |
| 团长只能管理自己店铺下的商品 | 不能跨店铺操作 |
| 团长只能管理自己店铺发布的团购 | 不能发布到他人店铺 |
| 团长只能处理自己店铺产生的订单 | 发货、查看团长订单均需校验 |

---

## 11. MVP 不设计的接口

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
