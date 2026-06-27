# Batch Context — Latest

> 记录最近完成的 batch 成果，供后续 batch 参考。
> 当前最新：**Batch 04-06 — 公共浏览、地址管理、订单预览与创建**

---

## 1. 已完成批次

| Batch | 状态 | 说明 |
|---|---|---|
| 0 | ✅ 完成 | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |
| 1 | ✅ 完成 | 模拟登录、当前用户、token 认证拦截 |
| 2 | ✅ 完成 | 创建店铺、获取/更新当前用户店铺、激活团长身份 |
| 3 | ✅ 完成 | 商品管理 CRUD、发布普通团购、团购管理（列表/详情/更新/结束） |
| 4-6 | ✅ 完成 | 公共浏览（列表/详情/团长主页）、地址管理（CRUD/默认地址）、订单预览/创建/列表/详情/取消 |

## 2. 项目结构

```
backend/
├── pom.xml                               # Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Flyway
├── docker-compose.yml                    # MySQL 8.0
├── src/main/java/com/example/groupshop/
│   ├── GroupshopApplication.java
│   ├── config/
│   │   ├── WebMvcConfig.java             # 注册 AuthInterceptor，白名单新增 public paths
│   │   └── MyBatisPlusConfig.java        # MyBatis-Plus 分页插件
│   ├── common/
│   │   ├── response/
│   │   │   ├── ApiResponse.java
│   │   │   └── PageResponse.java
│   │   ├── enums/
│   │   │   ├── ErrorCode.java            # 28 个错误码（含 GROUP_BUY_*, ADDRESS_*, ORDER_*）
│   │   │   └── DeliveryType.java
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── trace/
│   │   │   ├── TraceIdFilter.java
│   │   │   └── TraceIdAdvice.java
│   │   └── util/
│   │       └── CurrentStoreHelper.java
│   ├── model/
│   │   ├── entity/
│   │   │   ├── User.java, Leader.java, Store.java
│   │   │   ├── Product.java, GroupBuy.java, GroupBuyItem.java
│   │   │   ├── Address.java             # [新增] 地址实体
│   │   │   ├── Order.java               # [扩展] V1 全字段订单实体
│   │   │   └── OrderItem.java           # [扩展] V1 全字段订单明细实体
│   │   └── mapper/
│   │       ├── UserMapper.java, LeaderMapper.java, StoreMapper.java
│   │       ├── ProductMapper.java, GroupBuyMapper.java, GroupBuyItemMapper.java
│   │       ├── AddressMapper.java        # [新增]
│   │       ├── OrderMapper.java, OrderItemMapper.java
│   ├── auth/ ...                        # 无变更
│   ├── store/ ...                       # 无变更
│   ├── product/ ...                     # 无变更
│   ├── groupbuy/                        # [扩展] 追加公共浏览方法
│   │   ├── service/GroupBuyService.java
│   │   └── ...
│   ├── publicbrowsing/                  # [新增] 公共浏览模块
│   │   ├── PublicBrowsingController.java
│   │   └── dto/ (PublicGroupBuyItem, GroupBuyDetailResponse, ViewerInfo)
│   ├── leader/                          # [新增] 团长主页模块
│   │   ├── controller/ (无，通过 Service 调用)
│   │   ├── service/LeaderService.java
│   │   └── dto/LeaderHomepageResponse.java
│   ├── address/                         # [新增] 地址模块
│   │   ├── controller/AddressController.java
│   │   ├── service/AddressService.java
│   │   └── dto/ (CreateAddressRequest, UpdateAddressRequest, AddressResponse)
│   ├── order/                           # [新增] 订单模块
│   │   ├── controller/OrderController.java
│   │   ├── service/OrderService.java
│   │   └── dto/ (OrderPreviewRequest/Response, CreateOrderRequest, OrderResponse, OrderItemEntry)
│   ├── controller/HealthController.java
│   └── testconfig/TestValidationController.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/V1__create_mvp_tables.sql  # 未变更
└── src/test/
    ├── resources/application-test.yml
    └── java/com/example/groupshop/
        ├── base/ (MockMvcTestBase, ServiceTestBase)
        ├── publicbrowsing/PublicBrowsingControllerTest.java    # [新增] 6 个测试
        ├── address/AddressControllerTest.java                  # [新增] 8 个测试
        ├── address/AddressServiceTest.java                     # [新增] 6 个测试
        ├── order/OrderControllerTest.java                      # [新增] 12 个测试
        ├── order/OrderServiceTest.java                         # [新增] 9 个测试
        ├── ... 其他已有测试不变
        └── migration/FlywayMigrationTest.java  # 18 个测试（新增 address 检查）
```

## 3. 本批实现的接口

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/v1/group-buys` | 公共团购分页列表 | 不需要 |
| GET | `/api/v1/group-buys/{groupBuyId}` | 公共团购详情（含店主/商品/订阅） | 不需要 |
| GET | `/api/v1/leaders/{leaderId}/homepage` | 团长主页（含店铺+团购列表） | 不需要 |
| GET | `/api/v1/my/addresses` | 地址列表 | 需要 |
| POST | `/api/v1/my/addresses` | 创建地址 | 需要 |
| PATCH | `/api/v1/my/addresses/{addressId}` | 更新地址 | 需要 |
| DELETE | `/api/v1/my/addresses/{addressId}` | 删除地址 | 需要 |
| POST | `/api/v1/orders/preview` | 订单预览（不创建/不扣库存） | 需要 |
| POST | `/api/v1/orders` | 创建订单 | 需要 |
| GET | `/api/v1/my/orders` | 我的订单列表（支持 status 筛选） | 需要 |
| GET | `/api/v1/my/orders/{orderId}` | 订单详情 | 需要 |
| POST | `/api/v1/orders/{orderId}/cancel` | 取消订单（待支付） | 需要 |

## 4. 关键业务逻辑

### 公共浏览
- 免登录访问公开已发布团购；仅 `status=published & visibility=public`
- 不存在/非公开/非发布全部返回 `RESOURCE_NOT_FOUND`
- 列表聚合 `minPriceAmount`（items 最小值）、`soldCount`（sum）
- `viewer.subscribed=false`（Batch 9 后改为真实订阅状态）

### 地址管理
- 首个地址自动默认；设置默认时取消其他默认
- 删除默认地址后最新创建地址升为默认
- 跨用户操作返回 `ADDRESS_FORBIDDEN`

### 订单预览
- 校验：团购可购买、商品属团购、地址属当前用户、库存充足
- 计算：totalAmount=sum(price*qty), discountAmount=0, payAmount=totalAmount
- 不创建订单、不扣库存、不占库存

### 创建订单
- 再次执行所有预览校验
- 地址快照和商品快照（后续修改不影响历史订单）
- DB 状态：`order_status=pending_pay`, `pay_status=unpaid`
- API 响应：`orderStatus=pendingPay`（camelCase）
- 不扣减 `groupStock`、不增加 `soldCount`

### 取消订单
- 仅允许 `pendingPay+unpaid` 状态
- 成功：`orderStatus=canceled`

## 5. 错误码

新增业务错误码（已有基础上追加）：

| 错误码 | HTTP | 场景 |
|---|---|---|
| `GROUP_BUY_NOT_PURCHASABLE` | 422 | 团购不存在/不可购买/未开始 |
| `GROUP_BUY_ENDED` | 422 | 团购已结束 |
| `ITEM_NOT_IN_GROUP_BUY` | 422 | 商品不属于该团购 |
| `ADDRESS_FORBIDDEN` | 403 | 地址不属于当前用户 |
| `INSUFFICIENT_STOCK` | 422 | 库存不足 |
| `ORDER_NOT_CANCELABLE` | 422 | 订单不可取消 |

## 6. 测试命令

```bash
cd backend
mvn test                                    # 全部 157 个测试
```

## 7. 测试结果

```
Tests run: 157, Failures: 0, Errors: 0, Skipped: 0
```

| 测试类 | 测试数 |
|---|---|
| PublicBrowsingControllerTest | 6 |
| AddressControllerTest | 8 |
| AddressServiceTest | 6 |
| OrderControllerTest | 12 |
| OrderServiceTest | 9 |
| 已有测试 | 116 |
| **总计** | **157** |

## 8. 联调文档更新

已在 `docs/前后端联调文档.md` 中更新：
- §6 公共浏览团购：补充 `endTime`、`viewer.subscribed=false`、RESOURCE_NOT_FOUND
- §7 地址管理与订单预览：补充请求样例、错误码
- §8 创建订单：补充 my orders 列表/详情、取消订单

## 9. OpenAPI 契约

已更新 `docs/openapi/groupshop-api.yaml` 至版本 0.6.0：
- 新 tags: GroupBuyPublic, Address, Order, Leader
- 新增 12 个路径及请求/响应 schema
- 新增 Address、OrderPreview、Order 等 schema
- 新增 6 个错误响应类型
- 所有 Controller 测试均通过 `contractResult()` 验证

## 10. Flyway 迁移

无新增迁移文件。V1 已包含所需全部表结构。

## 11. Files created or modified

### New files

| File | Description |
|---|---|
| `model/entity/Address.java` | 地址实体 |
| `model/mapper/AddressMapper.java` | 地址 Mapper |
| `address/dto/CreateAddressRequest.java` | 创建地址请求 DTO |
| `address/dto/UpdateAddressRequest.java` | 更新地址请求 DTO |
| `address/dto/AddressResponse.java` | 地址响应 DTO |
| `address/service/AddressService.java` | 地址 Service |
| `address/controller/AddressController.java` | 地址 Controller |
| `publicbrowsing/dto/PublicGroupBuyItem.java` | 公共浏览列表项 DTO |
| `publicbrowsing/dto/GroupBuyDetailResponse.java` | 团购详情响应 DTO |
| `publicbrowsing/dto/ViewerInfo.java` | 浏览者信息 DTO |
| `publicbrowsing/PublicBrowsingController.java` | 公共浏览 Controller |
| `leader/dto/LeaderHomepageResponse.java` | 团长主页响应 DTO |
| `leader/service/LeaderService.java` | 团长主页 Service |
| `order/dto/OrderItemEntry.java` | 订单商品条目接口 |
| `order/dto/OrderPreviewRequest.java` | 订单预览请求 DTO |
| `order/dto/OrderPreviewResponse.java` | 订单预览响应 DTO |
| `order/dto/CreateOrderRequest.java` | 创建订单请求 DTO |
| `order/dto/OrderResponse.java` | 订单响应 DTO |
| `order/service/OrderService.java` | 订单 Service |
| `order/controller/OrderController.java` | 订单 Controller |
| `publicbrowsing/PublicBrowsingControllerTest.java` | 公共浏览 Controller 测试 |
| `address/AddressControllerTest.java` | 地址 Controller 测试 |
| `address/AddressServiceTest.java` | 地址 Service 测试 |
| `order/OrderControllerTest.java` | 订单 Controller 测试 |
| `order/OrderServiceTest.java` | 订单 Service 测试 |

### Modified files

| File | Change |
|---|---|
| `config/WebMvcConfig.java` | 白名单新增 `/api/v1/group-buys/**`, `/api/v1/leaders/*/homepage` |
| `model/entity/Order.java` | 扩展为 V1 全字段（地址快照、金额、时间戳等） |
| `model/entity/OrderItem.java` | 扩展为 V1 全字段（商品快照、金额等） |
| `groupbuy/service/GroupBuyService.java` | 新增 `getPublicGroupBuys()`, `getPublicGroupBuyDetail()`, `toPublicGroupBuyItem()`；新增 LeaderMapper/StoreMapper 依赖 |
| `docs/openapi/groupshop-api.yaml` | 版本 0.6.0，新增 tags/paths/schemas/error responses |
| `docs/前后端联调文档.md` | 更新 §6/§7/§8，补充 my orders/cancel |
| `docs/dev/context/latest-context.md` | 本次更新 |

### Flyway 迁移

无新增迁移文件。

## 12. Remaining issues

None

## 13. Suggestions for next batch

- Batch 07 将实现模拟支付、库存扣减、会员关系
- Batch 08 将实现团长订单管理与发货
- Batch 09 将实现订阅关系和会员卡展示
- `viewer.subscribed` 当前固定为 `false`，Batch 09 后改为真实订阅状态
