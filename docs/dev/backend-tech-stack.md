# 后端技术栈与实现约定

## 1. 技术栈

| 类型 | 技术 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot |
| 构建 | Maven |
| Web | Spring Web |
| 参数校验 | Validation |
| ORM | MyBatis-Plus |
| 数据库 | MySQL |
| 数据库迁移 | Flyway |
| 单元测试 | JUnit 5 |
| 接口测试 | MockMvc |

## 2. 包结构建议

后端代码按模块组织，开发批次按功能链路推进。

| 模块 | 说明 |
|---|---|
| auth | 模拟登录、token、当前用户 |
| user | 用户基础信息 |
| store | 团长身份和店铺 |
| product | 商品管理 |
| groupbuy | 普通团购和团购商品 |
| address | 收货地址 |
| order | 订单、支付、发货 |
| subscription | 订阅团长 |
| member | 会员关系和会员卡 |
| common | 统一响应、错误码、异常、traceId |

## 3. 统一响应

成功响应统一包裹在 `data` 中：

```json
{
  "success": true,
  "data": {},
  "traceId": "req_001"
}
```

错误响应：

```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_PAYABLE",
    "message": "当前订单不能支付",
    "details": {}
  },
  "traceId": "req_001"
}
```

## 4. 错误码

错误码使用大写蛇形命名。常用错误码以 `docs/API风格规范.md` 为准，包括 `UNAUTHORIZED`、`VALIDATION_ERROR`、`LEADER_REQUIRED`、`STORE_ALREADY_EXISTS`、`STORE_FORBIDDEN`、`GROUP_BUY_NOT_PURCHASABLE`、`GROUP_BUY_ENDED`、`ITEM_NOT_IN_GROUP_BUY`、`ADDRESS_FORBIDDEN`、`ORDER_NOT_PAYABLE`、`ORDER_ALREADY_PAID`、`ORDER_NOT_SHIPPABLE`、`ORDER_ALREADY_SHIPPED`、`INSUFFICIENT_STOCK`、`SUBSCRIPTION_EXISTS`。

## 5. 认证

MVP 使用模拟登录令牌：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

所有 `/my/**` 和需要登录的接口必须从 token 获取当前用户身份。`/my/store/**` 必须由服务端根据当前用户查找自己的团长身份和店铺，客户端不能通过 `storeId` 决定操作对象。

## 6. 金额

API 层全部使用整数分，字段名必须以 `Amount` 结尾。数据库推荐使用 `bigint` 保存分。不得让 API 返回 decimal 字符串或浮点数金额。

## 7. 状态

API 状态字段使用 `camelCase` 英文枚举。数据库如使用 `snake_case`，必须在 DTO 层转换。

| 业务 | API 状态 |
|---|---|
| 团购 | `draft`、`published`、`ended`、`removed` |
| 订单 | `pendingPay`、`paid`、`shipped`、`completed`、`canceled`、`afterSale` |
| 支付 | `unpaid`、`paid`、`refunded` |
| 店铺 | `active`、`disabled` |

## 8. Flyway 迁移约定

Batch 0 直接创建全部 MVP 核心表：

```text
users
leaders
stores
products
group_buys
group_buy_items
addresses
orders
order_items
shipments
subscriptions
member_relations
```

后续 batch 只在确有必要时追加迁移。追加迁移必须说明原因，不得反复补已知 MVP 基础结构。
