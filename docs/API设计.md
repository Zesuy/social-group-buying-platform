# API 设计

> 本文基于 `docs/功能需求定义.md` 和 `docs/数据模型设计.md` 编写，接口风格遵循 `docs/API风格规范.md`。
>
> 范围：只设计 MVP 必需 API。P1/P2 能力仅在文末列出，不展开接口细节。

---

## 1. 设计边界

MVP API 支撑以下闭环：

```text
用户浏览团购
→ 查看团长主页 / 商品详情
→ 登录后下单
→ 模拟支付
→ 查看订单
→ 团长发货
```

```text
用户登录
→ 创建店铺
→ 自动生成团长身份
→ 创建商品
→ 发布普通团购
→ 管理自己店铺的订单
```

MVP 不设计真实微信支付、优惠券、售后退款完整流程、帮卖分销、积分商城、平台后台、公众号推送。

---

## 2. API 模块总览

| 模块 | 说明 | 登录要求 |
|---|---|---|
| 认证与当前用户 | 登录、获取当前用户信息 | 部分需要 |
| 公共浏览 | 首页团购流、团购详情、团长主页 | 不需要 |
| 店铺与团长 | 创建店铺、查看自己的团长身份和店铺 | 需要 |
| 商品 | 团长管理自己店铺商品 | 需要团长身份 |
| 团购 | 团长发布普通团购，用户查看团购 | 部分需要 |
| 地址 | 用户管理收货地址 | 需要 |
| 订单 | 用户下单、查看订单、模拟支付、取消、确认收货 | 需要 |
| 团长订单 | 团长查看自己店铺订单、发货 | 需要团长身份 |
| 订阅 | 关注 / 取消关注团长 | 需要 |
| 会员卡 | 查看基础会员关系 | 需要 |

---

## 3. 通用对象摘要

### 3.1 User

```json
{
  "id": 1,
  "nickname": "用户昵称",
  "avatarUrl": "https://example.com/avatar.png",
  "phone": "13800000000",
  "hasLeader": true,
  "leaderId": 10,
  "storeId": 20
}
```

### 3.2 Leader

```json
{
  "id": 10,
  "userId": 1,
  "displayName": "某某团长",
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "主营生鲜水果",
  "memberCount": 0,
  "followerCount": 0
}
```

### 3.3 Store

```json
{
  "id": 20,
  "leaderId": 10,
  "name": "某某的小店",
  "logoUrl": "https://example.com/logo.png",
  "description": "店铺简介",
  "defaultDeliveryType": "express",
  "distributionEnabled": false,
  "status": "active"
}
```

### 3.4 GroupBuy

```json
{
  "id": 100,
  "storeId": 20,
  "leaderId": 10,
  "title": "山东蜜桃团购",
  "introduction": "产地直发，香甜多汁",
  "coverImageUrl": "https://example.com/cover.png",
  "groupType": "normal",
  "deliveryType": "express",
  "shippingTime": "2026-06-30T18:00:00+08:00",
  "startTime": "2026-06-24T12:00:00+08:00",
  "endTime": "2026-07-01T12:00:00+08:00",
  "visibility": "public",
  "status": "published"
}
```

### 3.5 GroupBuyItem

```json
{
  "id": 1001,
  "groupBuyId": 100,
  "productId": 501,
  "skuId": null,
  "displayName": "白玉蜜桃 5 斤装",
  "groupPriceAmount": 2990,
  "groupStock": 100,
  "soldCount": 0,
  "sortOrder": 1
}
```

### 3.6 Order

```json
{
  "id": 9001,
  "orderNo": "202606240001",
  "userId": 1,
  "leaderId": 10,
  "storeId": 20,
  "groupBuyId": 100,
  "totalAmount": 2990,
  "discountAmount": 0,
  "payAmount": 2990,
  "payStatus": "unpaid",
  "orderStatus": "pendingPay",
  "remark": "请尽快发货",
  "items": []
}
```

---

## 4. 认证与当前用户

### 4.1 模拟登录

```http
POST /api/v1/auth/mock-login
```

用途：MVP 阶段用于模拟登录，后续替换为微信登录。

请求：

```json
{
  "nickname": "用户昵称",
  "avatarUrl": "https://example.com/avatar.png",
  "phone": "13800000000"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "accessToken": "mock_access_token",
    "user": {
      "id": 1,
      "nickname": "用户昵称",
      "avatarUrl": "https://example.com/avatar.png",
      "phone": "13800000000",
      "hasLeader": false,
      "leaderId": null,
      "storeId": null
    }
  },
  "traceId": "req_001"
}
```

### 4.2 获取当前用户

```http
GET /api/v1/me
```

登录：需要。

响应：返回当前用户、团长身份和店铺摘要。

```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "nickname": "用户昵称",
      "avatarUrl": "https://example.com/avatar.png",
      "phone": "13800000000",
      "hasLeader": true,
      "leaderId": 10,
      "storeId": 20
    },
    "leader": {
      "id": 10,
      "displayName": "某某团长",
      "avatarUrl": "https://example.com/avatar.png"
    },
    "store": {
      "id": 20,
      "name": "某某的小店",
      "logoUrl": "https://example.com/logo.png",
      "status": "active"
    }
  },
  "traceId": "req_001"
}
```

---

## 5. 公共浏览 API

### 5.1 首页团购列表

```http
GET /api/v1/group-buys
```

登录：不需要。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| status | string | 默认 `published` |
| keyword | string | P1 搜索使用，MVP 可不支持 |
| page | number | 页码 |
| pageSize | number | 每页数量 |

响应：

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 100,
        "title": "山东蜜桃团购",
        "coverImageUrl": "https://example.com/cover.png",
        "status": "published",
        "endTime": "2026-07-01T12:00:00+08:00",
        "minPriceAmount": 2990,
        "soldCount": 12,
        "leader": {
          "id": 10,
          "displayName": "某某团长",
          "avatarUrl": "https://example.com/avatar.png"
        },
        "store": {
          "id": 20,
          "name": "某某的小店"
        }
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

### 5.2 团购详情

```http
GET /api/v1/group-buys/{groupBuyId}
```

登录：不需要。

响应：返回团购、团长、店铺、团购商品列表。

```json
{
  "success": true,
  "data": {
    "groupBuy": {
      "id": 100,
      "title": "山东蜜桃团购",
      "introduction": "产地直发，香甜多汁",
      "coverImageUrl": "https://example.com/cover.png",
      "groupType": "normal",
      "deliveryType": "express",
      "shippingTime": "2026-06-30T18:00:00+08:00",
      "startTime": "2026-06-24T12:00:00+08:00",
      "endTime": "2026-07-01T12:00:00+08:00",
      "status": "published"
    },
    "leader": {
      "id": 10,
      "displayName": "某某团长",
      "avatarUrl": "https://example.com/avatar.png",
      "followerCount": 0
    },
    "store": {
      "id": 20,
      "name": "某某的小店",
      "logoUrl": "https://example.com/logo.png"
    },
    "items": [
      {
        "id": 1001,
        "productId": 501,
        "skuId": null,
        "displayName": "白玉蜜桃 5 斤装",
        "groupPriceAmount": 2990,
        "groupStock": 100,
        "soldCount": 12,
        "coverImageUrl": "https://example.com/product.png"
      }
    ],
    "viewer": {
      "subscribed": false
    }
  },
  "traceId": "req_001"
}
```

### 5.3 团长主页

```http
GET /api/v1/leaders/{leaderId}/homepage
```

登录：不需要。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| page | number | 团购列表页码 |
| pageSize | number | 团购列表每页数量 |

响应：

```json
{
  "success": true,
  "data": {
    "leader": {
      "id": 10,
      "displayName": "某某团长",
      "avatarUrl": "https://example.com/avatar.png",
      "bio": "主营生鲜水果",
      "memberCount": 0,
      "followerCount": 0
    },
    "store": {
      "id": 20,
      "name": "某某的小店",
      "logoUrl": "https://example.com/logo.png",
      "description": "店铺简介",
      "defaultDeliveryType": "express"
    },
    "viewer": {
      "subscribed": false
    },
    "groupBuys": {
      "items": [],
      "page": 1,
      "pageSize": 20,
      "total": 0,
      "hasMore": false
    }
  },
  "traceId": "req_001"
}
```

---

## 6. 店铺与团长 API

### 6.1 创建店铺

```http
POST /api/v1/stores
```

登录：需要。

业务规则：

- 当前用户没有团长身份时，创建店铺会同时生成团长身份。
- MVP 阶段一个用户最多一个团长身份，一个团长最多一个店铺。
- 如果当前用户已创建店铺，返回 `STORE_ALREADY_EXISTS`。

请求：

```json
{
  "name": "某某的小店",
  "logoUrl": "https://example.com/logo.png",
  "description": "店铺简介",
  "defaultDeliveryType": "express"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "leader": {
      "id": 10,
      "displayName": "某某的小店",
      "avatarUrl": "https://example.com/logo.png"
    },
    "store": {
      "id": 20,
      "leaderId": 10,
      "name": "某某的小店",
      "logoUrl": "https://example.com/logo.png",
      "description": "店铺简介",
      "defaultDeliveryType": "express",
      "distributionEnabled": false,
      "status": "active"
    }
  },
  "traceId": "req_001"
}
```

### 6.2 获取我的店铺

```http
GET /api/v1/my/store
```

登录：需要。

响应：返回当前用户自己的团长身份和店铺。未创建时返回 `data: null`。

### 6.3 更新我的店铺

```http
PATCH /api/v1/my/store
```

登录：需要团长身份。

请求：

```json
{
  "name": "新的店铺名称",
  "logoUrl": "https://example.com/logo.png",
  "description": "新的店铺简介",
  "defaultDeliveryType": "express"
}
```

响应：返回更新后的店铺。

---

## 7. 商品 API

### 7.1 我的店铺商品列表

```http
GET /api/v1/my/store/products
```

登录：需要团长身份。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| page | number | 页码 |
| pageSize | number | 每页数量 |

响应：返回当前团长自己店铺下的商品列表。

### 7.2 创建商品

```http
POST /api/v1/my/store/products
```

登录：需要团长身份。

请求：

```json
{
  "name": "白玉蜜桃",
  "description": "山东蒙阴产地直发",
  "coverImageUrl": "https://example.com/product.png",
  "basePriceAmount": 2990,
  "stock": 100
}
```

响应：

```json
{
  "success": true,
  "data": {
    "id": 501,
    "storeId": 20,
    "name": "白玉蜜桃",
    "description": "山东蒙阴产地直发",
    "coverImageUrl": "https://example.com/product.png",
    "basePriceAmount": 2990,
    "stock": 100,
    "status": "active"
  },
  "traceId": "req_001"
}
```

### 7.3 更新商品

```http
PATCH /api/v1/my/store/products/{productId}
```

登录：需要团长身份，且商品必须属于自己的店铺。

### 7.4 删除商品

```http
DELETE /api/v1/my/store/products/{productId}
```

登录：需要团长身份，且商品必须属于自己的店铺。

说明：MVP 可做软删除。若商品已被团购引用，不应影响历史订单快照。

---

## 8. 团购 API

### 8.1 创建并发布普通团购

```http
POST /api/v1/my/store/group-buys
```

登录：需要团长身份。

业务规则：

- MVP 只支持 `groupType = normal`。
- 一个团购至少包含一个团购商品。
- 团购商品价格是用户下单价格。
- MVP 可直接创建为 `published`，不做草稿保存。

请求：

```json
{
  "title": "山东蜜桃团购",
  "introduction": "产地直发，香甜多汁",
  "coverImageUrl": "https://example.com/cover.png",
  "deliveryType": "express",
  "shippingTime": "2026-06-30T18:00:00+08:00",
  "startTime": "2026-06-24T12:00:00+08:00",
  "endTime": "2026-07-01T12:00:00+08:00",
  "items": [
    {
      "productId": 501,
      "skuId": null,
      "displayName": "白玉蜜桃 5 斤装",
      "groupPriceAmount": 2990,
      "groupStock": 100,
      "sortOrder": 1
    }
  ]
}
```

响应：返回团购和团购商品列表。

### 8.2 我的团购列表

```http
GET /api/v1/my/store/group-buys
```

登录：需要团长身份。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| status | string | `published` / `ended` |
| page | number | 页码 |
| pageSize | number | 每页数量 |

响应：返回当前团长自己店铺发布的团购列表。

### 8.3 更新我的团购

```http
PATCH /api/v1/my/store/group-buys/{groupBuyId}
```

登录：需要团长身份，且团购必须属于自己的店铺。

说明：MVP 可允许更新标题、介绍、封面、时间、发货时间。已产生订单后的价格和商品变更需谨慎，历史订单不受影响。

### 8.4 结束团购

```http
POST /api/v1/my/store/group-buys/{groupBuyId}/end
```

登录：需要团长身份，且团购必须属于自己的店铺。

业务规则：

- 只有 `published` 状态的团购可以结束。
- 结束后状态变为 `ended`。
- 已结束团购不允许继续下单。

响应：返回更新后的团购。

---

## 9. 地址 API

### 9.1 地址列表

```http
GET /api/v1/my/addresses
```

登录：需要。

### 9.2 创建地址

```http
POST /api/v1/my/addresses
```

登录：需要。

请求：

```json
{
  "receiverName": "张三",
  "receiverPhone": "13800000000",
  "province": "浙江省",
  "city": "杭州市",
  "district": "西湖区",
  "detail": "某某路 1 号",
  "isDefault": true
}
```

### 9.3 更新地址

```http
PATCH /api/v1/my/addresses/{addressId}
```

登录：需要，且只能更新自己的地址。

### 9.4 删除地址

```http
DELETE /api/v1/my/addresses/{addressId}
```

登录：需要，且只能删除自己的地址。

---

## 10. 订单 API

### 10.1 创建订单

```http
POST /api/v1/orders
```

登录：需要。

业务规则：

- 只能购买 `published` 状态且未结束的团购。
- 下单价格以团购商品 `groupPriceAmount` 为准。
- 下单时写入商品名称、规格、单价、数量快照。
- MVP 不计算优惠券、红包、会员折扣和积分。

请求：

```json
{
  "groupBuyId": 100,
  "addressId": 300,
  "remark": "请尽快发货",
  "items": [
    {
      "groupBuyItemId": 1001,
      "quantity": 1
    }
  ]
}
```

响应：

```json
{
  "success": true,
  "data": {
    "id": 9001,
    "orderNo": "202606240001",
    "groupBuyId": 100,
    "storeId": 20,
    "leaderId": 10,
    "totalAmount": 2990,
    "discountAmount": 0,
    "payAmount": 2990,
    "payStatus": "unpaid",
    "orderStatus": "pendingPay",
    "remark": "请尽快发货",
    "items": [
      {
        "id": 1,
        "groupBuyItemId": 1001,
        "productId": 501,
        "skuId": null,
        "productName": "白玉蜜桃 5 斤装",
        "skuName": "",
        "unitPriceAmount": 2990,
        "quantity": 1,
        "totalAmount": 2990
      }
    ]
  },
  "traceId": "req_001"
}
```

### 10.2 我的订单列表

```http
GET /api/v1/my/orders
```

登录：需要。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| status | string | `pendingPay` / `paid` / `shipped` / `completed` / `canceled` |
| page | number | 页码 |
| pageSize | number | 每页数量 |

响应：返回当前用户自己的订单列表。

### 10.3 订单详情

```http
GET /api/v1/my/orders/{orderId}
```

登录：需要。

权限：

- 买家可以查看自己的订单。
- 团长查看店铺订单使用团长订单接口。

### 10.4 模拟支付

```http
POST /api/v1/orders/{orderId}/simulate-pay
```

登录：需要，且订单必须属于当前用户。

业务规则：

- 只有 `pendingPay` / `unpaid` 的订单可以模拟支付。
- 成功后 `payStatus = paid`，`orderStatus = paid`。
- 不处理真实支付渠道、回调、对账和退款。

响应：返回更新后的订单。

### 10.5 取消订单

```http
POST /api/v1/orders/{orderId}/cancel
```

登录：需要，且订单必须属于当前用户。

业务规则：

- MVP 只允许取消未支付订单。
- 取消后 `orderStatus = canceled`。

### 10.6 确认收货

```http
POST /api/v1/orders/{orderId}/complete
```

登录：需要，且订单必须属于当前用户。

业务规则：

- 只有 `shipped` 状态订单可以确认收货。
- 确认后 `orderStatus = completed`。

---

## 11. 团长订单 API

### 11.1 我的店铺订单列表

```http
GET /api/v1/my/store/orders
```

登录：需要团长身份。

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| groupBuyId | number | 按团购筛选 |
| status | string | 订单状态 |
| page | number | 页码 |
| pageSize | number | 每页数量 |

权限：只能返回当前团长自己店铺产生的订单。

### 11.2 我的店铺订单详情

```http
GET /api/v1/my/store/orders/{orderId}
```

登录：需要团长身份。

权限：只能查看当前团长自己店铺产生的订单。

### 11.3 订单发货

```http
POST /api/v1/my/store/orders/{orderId}/ship
```

登录：需要团长身份。

业务规则：

- 只能处理自己店铺产生的订单。
- 只有 `paid` 状态订单可以发货。
- 发货后订单状态变为 `shipped`。

请求：

```json
{
  "deliveryType": "express",
  "logisticsCompany": "顺丰速运",
  "trackingNo": "SF123456789"
}
```

响应：返回更新后的订单和发货记录。

---

## 12. 订阅 API

### 12.1 订阅团长

```http
POST /api/v1/leaders/{leaderId}/subscription
```

登录：需要。

请求：

```json
{
  "source": "homepage"
}
```

业务规则：

- MVP 订阅只表示关注团长 / 店铺。
- 不做公众号推送、订阅邀请卡、订阅红包。
- 重复订阅可返回已有订阅关系，或返回 `SUBSCRIPTION_EXISTS`，实现时二选一并保持一致。

响应：

```json
{
  "success": true,
  "data": {
    "id": 7001,
    "userId": 1,
    "leaderId": 10,
    "storeId": 20,
    "status": "subscribed",
    "source": "homepage",
    "subscribedAt": "2026-06-24T12:00:00+08:00"
  },
  "traceId": "req_001"
}
```

### 12.2 取消订阅

```http
DELETE /api/v1/leaders/{leaderId}/subscription
```

登录：需要。

响应：`data: null`。

### 12.3 我的订阅列表

```http
GET /api/v1/my/subscriptions
```

登录：需要。

响应：返回当前用户订阅的团长 / 店铺列表。

---

## 13. 会员卡 API

### 13.1 我的会员卡列表

```http
GET /api/v1/my/member-cards
```

登录：需要。

MVP 展示范围：

- 团长 / 店铺信息。
- 会员等级，例如 `V0`。
- 成长值。
- 累计消费。
- 累计订单。
- 不做等级升级规则、折扣计算、积分商城。

响应：

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 8001,
        "leader": {
          "id": 10,
          "displayName": "某某团长",
          "avatarUrl": "https://example.com/avatar.png"
        },
        "store": {
          "id": 20,
          "name": "某某的小店",
          "logoUrl": "https://example.com/logo.png"
        },
        "level": "V0",
        "growthValue": 0,
        "totalOrderAmount": 2990,
        "totalOrderCount": 1,
        "lastOrderAt": "2026-06-24T12:00:00+08:00"
      }
    ]
  },
  "traceId": "req_001"
}
```

---

## 14. 状态流转接口归属

| 状态变化 | 触发接口 |
|---|---|
| 用户无店铺 -> 已创建店铺 | `POST /api/v1/stores` |
| 用户无团长身份 -> 已生成团长身份 | `POST /api/v1/stores` |
| 团购未发布 -> 已发布 | `POST /api/v1/my/store/group-buys` |
| 团购已发布 -> 已结束 | `POST /api/v1/my/store/group-buys/{groupBuyId}/end` |
| 订单待支付 -> 已支付 | `POST /api/v1/orders/{orderId}/simulate-pay` |
| 订单待支付 -> 已取消 | `POST /api/v1/orders/{orderId}/cancel` |
| 订单已支付 -> 已发货 | `POST /api/v1/my/store/orders/{orderId}/ship` |
| 订单已发货 -> 已完成 | `POST /api/v1/orders/{orderId}/complete` |
| 未订阅 -> 已订阅 | `POST /api/v1/leaders/{leaderId}/subscription` |
| 已订阅 -> 已取消 | `DELETE /api/v1/leaders/{leaderId}/subscription` |

---

## 15. MVP 暂不展开的 API

| 能力 | 后续优先级 | 说明 |
|---|---:|---|
| 微信登录 | P1/P2 | MVP 使用模拟登录 |
| 真实微信支付 | P1/P2 | MVP 使用模拟支付 |
| 购物车 | P1 | MVP 直接下单 |
| 商品库独立管理 | P1 | MVP 商品直接挂店铺 |
| 优惠券 / 红包 | P1 | 不参与订单金额 |
| 售后退款 | P1 | MVP 只预留状态 |
| 隐私权限细分 | P1 | MVP 默认公开团购 |
| 分享海报 | P1 | MVP 可使用普通分享链接 |
| 帮卖分销 | P2 | 不设计佣金和结算 |
| 积分商城 | P2 | 不设计积分账户和兑换 |
| 公众号推送 | P2 | 不设计真实触达 |
| 平台管理后台 | P2 | 不设计后台 API |
