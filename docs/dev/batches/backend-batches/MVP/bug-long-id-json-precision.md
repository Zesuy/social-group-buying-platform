# Bug：后端 Long ID JSON 精度丢失导致前端详情请求错 ID

## 1. 背景

在 Frontend Batch 05.5 真实前后端联调部署验证时发现：后端接口返回的部分主键 ID 为 MyBatis-Plus 雪花 ID，大于 JavaScript `Number.MAX_SAFE_INTEGER`。前端接收 JSON 后会发生数字精度丢失，导致后续使用该 ID 发起详情、下单等请求时命中错误资源。

本问题只记录在后端 Batch 10 之后处理，不在当前 Frontend Batch 05.5 中修改。

## 2. 影响范围

疑似影响所有以 JSON number 形式返回的 `BIGINT` 主键字段，包括但不限于：

- `user.id`
- `leader.id`
- `store.id`
- `product.id`
- `groupBuy.id`
- `groupBuyItem.id`
- `address.id`
- `order.id`
- `subscription.id`
- `memberRelation.id`

高风险链路：

- 首页团购列表点击进入团购详情。
- 团购详情页使用 `groupBuyId` 查询详情或创建订单。
- 店铺商品创建后使用 `productId` 创建团购。
- 订单、地址、发货、订阅等需要前端回传 ID 的接口。

## 3. 复现现象

真实联调时通过接口创建团购后，后端返回类似：

```json
{
  "id": 2071642363297644546,
  "title": "阳山水蜜桃团购..."
}
```

浏览器 / Node 侧解析后会变为近似值，例如：

```text
2071642363297644500
```

前端路由进入：

```text
/#/group-buys/2071642363297644500
```

详情接口最终返回资源不存在，页面显示“资源不存在 / 重新加载”。

另一个复现场景：

1. 调用 `POST /api/v1/my/store/products` 创建商品。
2. 使用响应中的 `product.id` 调用 `POST /api/v1/my/store/group-buys`。
3. 因 `productId` 精度丢失，后端返回 `RESOURCE_NOT_FOUND 商品不存在`。

## 4. 根因判断

数据库表主键为 `BIGINT AUTO_INCREMENT`，但实体 / MyBatis-Plus 插入策略实际生成了雪花 ID。雪花 ID 超过 JavaScript 安全整数范围，后端又以 JSON number 输出，前端无法无损表示。

这不是前端选择器或 Playwright 测试问题，而是真实 API 契约中的 ID 序列化问题。

## 5. 修复要求

后端需要统一 ID 对外序列化口径，推荐将所有对外响应中的 `Long` ID 序列化为 JSON string，并同步要求请求 DTO 中的 ID 支持字符串输入。

修复时必须覆盖：

- 全局 Jackson `Long` / `long` 序列化策略，或所有 API DTO 的 ID 字段类型调整。
- 请求参数和请求体中 ID 的反序列化兼容。
- OpenAPI / API 文档中的 ID 类型说明。
- `docs/前后端联调文档.md` 中涉及 ID 回传的示例。
- MockMvc 测试断言，确保响应 ID 不再作为 JSON number 输出。

如果选择改为数据库自增小整数 ID，也必须明确评估 MyBatis-Plus `IdType`、现有测试、迁移数据和生产扩展风险；不得只依赖本地运行参数绕过。

## 6. 验收标准

- 前端通过真实接口创建商品后，可以直接使用响应中的 `productId` 创建团购。
- 首页团购列表返回的 `groupBuyId` 点击进入详情页后，详情接口能稳定命中同一条团购。
- 所有 API 响应中的业务 ID 在浏览器中不会发生精度丢失。
- `npm run test:e2e:live` 不需要使用小整数手工种子数据即可跑通团购详情、下单、订单、支付和发货链路。
- 后端测试覆盖至少一个超过 `Number.MAX_SAFE_INTEGER` 的 ID 序列化用例。

## 7. 禁止事项

- 不在 Frontend Batch 05.5 中临时修复或绕过。
- 不要求前端通过正则解析原始 JSON 文本保留 ID。
- 不只修复某一个接口；必须统一所有对外 ID 字段口径。
- 不把 ID 精度问题隐藏为测试数据约束。
