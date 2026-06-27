# Batch Context — Latest

> 记录最近完成的 batch 成果，供后续 batch 参考。
> 当前最新：**Batch 03 — 商品与团购完整闭环**

---

## 1. 已完成批次

| Batch | 状态 | 说明 |
|---|---|---|
| 0 | ✅ 完成 | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |
| 1 | ✅ 完成 | 模拟登录、当前用户、token 认证拦截 |
| 2 | ✅ 完成 | 创建店铺、获取/更新当前用户店铺、激活团长身份 |
| 3 | ✅ 完成 | 商品管理 CRUD、发布普通团购、团购管理（列表/详情/更新/结束） |

## 2. 项目结构

```
backend/
├── pom.xml                               # Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Flyway
├── docker-compose.yml                    # MySQL 8.0
├── src/main/java/com/example/groupshop/
│   ├── GroupshopApplication.java
│   ├── config/
│   │   ├── WebMvcConfig.java             # 注册 AuthInterceptor
│   │   └── MyBatisPlusConfig.java        # MyBatis-Plus 分页插件
│   ├── common/
│   │   ├── response/
│   │   │   ├── ApiResponse.java          # 统一响应 {success, data/traceId, error/traceId}
│   │   │   └── PageResponse.java         # 分页 {items, page, pageSize, total, hasMore}
│   │   ├── enums/
│   │   │   ├── ErrorCode.java            # 20+ 个错误码
│   │   │   └── DeliveryType.java         # 配送方式枚举
│   │   ├── exception/
│   │   │   ├── BusinessException.java    # 携带 ErrorCode
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── trace/
│   │   │   ├── TraceIdFilter.java
│   │   │   └── TraceIdAdvice.java
│   │   └── util/
│   │       └── CurrentStoreHelper.java   # [新增] 当前用户 -> leader -> store 解析
│   ├── model/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Leader.java
│   │   │   ├── Store.java
│   │   │   ├── Product.java              # [新增] 商品实体
│   │   │   ├── GroupBuy.java             # [新增] 团购活动实体
│   │   │   ├── GroupBuyItem.java         # [新增] 团购商品关系实体
│   │   │   ├── Order.java                # [新增] 最小订单实体（仅用于保护校验）
│   │   │   └── OrderItem.java            # [新增] 最小订单明细实体（仅用于保护校验）
│   │   └── mapper/
│   │       ├── UserMapper.java
│   │       ├── LeaderMapper.java
│   │       ├── StoreMapper.java
│   │       ├── ProductMapper.java        # [新增]
│   │       ├── GroupBuyMapper.java       # [新增]
│   │       ├── GroupBuyItemMapper.java   # [新增]
│   │       ├── OrderMapper.java          # [新增] 仅用于保护校验
│   │       └── OrderItemMapper.java      # [新增] 仅用于保护校验
│   ├── auth/
│   │   ├── TokenStore.java
│   │   ├── AuthInterceptor.java
│   │   ├── controller/AuthController.java
│   │   ├── dto/ (MockLoginRequest, MockLoginResponse, CurrentUserResponse)
│   │   └── service/AuthService.java
│   ├── store/
│   │   ├── controller/StoreController.java
│   │   ├── service/StoreService.java
│   │   └── dto/ (CreateStoreRequest, UpdateStoreRequest, StoreResponse)
│   ├── product/                          # [新增] 商品模块
│   │   ├── controller/ProductController.java
│   │   ├── service/ProductService.java
│   │   └── dto/ (CreateProductRequest, UpdateProductRequest, ProductResponse)
│   ├── groupbuy/                         # [新增] 团购模块
│   │   ├── controller/GroupBuyController.java
│   │   ├── service/GroupBuyService.java
│   │   └── dto/ (CreateGroupBuyRequest, UpdateGroupBuyRequest, GroupBuyResponse)
│   ├── controller/HealthController.java
│   └── testconfig/TestValidationController.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/V1__create_mvp_tables.sql  # 未变更
└── src/test/
    ├── resources/application-test.yml
    └── java/com/example/groupshop/
        ├── base/
        │   ├── MockMvcTestBase.java
        │   └── ServiceTestBase.java
        ├── GroupshopApplicationTests.java
        ├── auth/ (AuthControllerTest, AuthServiceTest)
        ├── store/ (StoreControllerTest, StoreServiceTest)
        ├── product/                      # [新增]
        │   ├── ProductControllerTest.java  # 13 个测试
        │   └── ProductServiceTest.java     # 11 个测试
        ├── groupbuy/                     # [新增]
        │   ├── GroupBuyControllerTest.java # 12 个测试
        │   └── GroupBuyServiceTest.java    # 13 个测试
        ├── common/ (ErrorResponseTest, ValidationErrorTest)
        ├── contract/OpenApiContractTest.java
        └── migration/FlywayMigrationTest.java
```

## 3. Batch 03 实现的接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/my/store/products` | 商品列表（排除 deleted） |
| POST | `/api/v1/my/store/products` | 创建商品（需团长身份） |
| GET | `/api/v1/my/store/products/{productId}` | 商品详情 |
| PATCH | `/api/v1/my/store/products/{productId}` | 更新商品（部分更新） |
| DELETE | `/api/v1/my/store/products/{productId}` | 软删除商品 |
| POST | `/api/v1/my/store/group-buys` | 创建并发布普通团购（支持内联/复用商品） |
| GET | `/api/v1/my/store/group-buys` | 我的团购列表（支持 status 筛选） |
| GET | `/api/v1/my/store/group-buys/{groupBuyId}` | 我的团购详情 |
| PATCH | `/api/v1/my/store/group-buys/{groupBuyId}` | 更新团购（含订单价格保护） |
| POST | `/api/v1/my/store/group-buys/{groupBuyId}/end` | 结束团购（published -> ended） |

### 关键业务逻辑

**商品管理：**
- 金额使用整数分，`basePriceAmount >= 0`，`stock >= 0`
- 软删除：`status=deleted`，列表自动排除 deleted 商品
- 跨店铺操作返回 `STORE_FORBIDDEN`

**团购发布：**
- MVP 仅支持 `groupType=normal`，创建即 `status=published`
- 同一个事务内创建 `group_buys`、必要的 `products`、`group_buy_items`
- item 支持 `productId`（复用已有商品）或 `product`（内联创建新商品），互斥
- 复用商品必须属于当前店铺且状态不是 `deleted`

**团购管理：**
- 列表支持 `status` 参数筛选
- 更新：PATCH 部分更新，含订单价格保护（`BUSINESS_RULE_VIOLATION`）
- 结束：只有 `published` 可结束为 `ended`

**权限与归属：**
- `CurrentStoreHelper` 统一解析 user -> leader -> store
- 无 leader/store 返回 `LEADER_REQUIRED`
- 跨店铺资源返回 `STORE_FORBIDDEN`
- 资源不存在返回 `RESOURCE_NOT_FOUND`

## 4. 统一响应结构

未变更，同 Batch 0。新增 `EmptySuccessResponse`（无 data 的成功响应，用于 DELETE）。

## 5. 错误码枚举

未新增 ErrorCode。使用已有：
- `LEADER_REQUIRED` (403)
- `STORE_FORBIDDEN` (403)
- `RESOURCE_NOT_FOUND` (404)
- `BUSINESS_RULE_VIOLATION` (422) — 用于团购结束和价格保护
- `VALIDATION_ERROR` (400)

## 6. 测试命令

```bash
cd backend
mvn test                                    # 全部 116 个测试
mvn test -Dtest=ProductControllerTest       # 商品 Controller 测试（13 个）
mvn test -Dtest=ProductServiceTest          # 商品 Service 测试（11 个）
mvn test -Dtest=GroupBuyControllerTest      # 团购 Controller 测试（12 个）
mvn test -Dtest=GroupBuyServiceTest         # 团购 Service 测试（13 个）
```

## 7. 测试结果

```
Tests run: 116, Failures: 0, Errors: 0, Skipped: 0
```

## 8. 联调文档更新

已在 `docs/前后端联调文档.md` 中更新：
- §4A 商品管理链路：创建、列表、详情、更新、删除
- §5 发布与管理系统团购：内联商品/复用商品发布、列表、详情、更新、结束
- 补充 `STORE_FORBIDDEN`、`BUSINESS_RULE_VIOLATION` 等错误码

## 9. OpenAPI 契约

已更新 `docs/openapi/groupshop-api.yaml` 至版本 0.3.0：
- 新增 Product tag + 5 个路径 + 请求/响应 schema
- 新增 GroupBuy tag + 6 个路径 + 请求/响应 schema
- 新增 `NotFoundError`、`BusinessRuleViolationError` 错误响应
- 所有成功请求的 Controller 测试均通过 `contractResult()` 验证

## 10. Flyway 迁移

无新增迁移文件。V1 已包含所需全部表结构。

## 11. Files created or modified

### New files

| File | Description |
|---|---|
| `common/util/CurrentStoreHelper.java` | 当前店铺解析辅助类（user -> leader -> store） |
| `model/entity/Product.java` | 商品实体 |
| `model/entity/GroupBuy.java` | 团购活动实体 |
| `model/entity/GroupBuyItem.java` | 团购商品关系实体 |
| `model/entity/Order.java` | 最小订单实体（用于价格保护校验） |
| `model/entity/OrderItem.java` | 最小订单明细实体（用于价格保护校验） |
| `model/mapper/ProductMapper.java` | 商品 Mapper |
| `model/mapper/GroupBuyMapper.java` | 团购 Mapper |
| `model/mapper/GroupBuyItemMapper.java` | 团购商品关系 Mapper |
| `model/mapper/OrderMapper.java` | 订单 Mapper（最小） |
| `model/mapper/OrderItemMapper.java` | 订单明细 Mapper（最小） |
| `product/dto/CreateProductRequest.java` | 创建商品请求 DTO |
| `product/dto/UpdateProductRequest.java` | 更新商品请求 DTO |
| `product/dto/ProductResponse.java` | 商品响应 DTO |
| `product/service/ProductService.java` | 商品 Service |
| `product/controller/ProductController.java` | 商品 Controller |
| `groupbuy/dto/CreateGroupBuyRequest.java` | 创建团购请求 DTO（含 InlineProduct/ItemEntry） |
| `groupbuy/dto/UpdateGroupBuyRequest.java` | 更新团购请求 DTO（含 UpdateItemEntry） |
| `groupbuy/dto/GroupBuyResponse.java` | 团购响应 DTO（含 GroupBuyData/GroupBuyItemData） |
| `groupbuy/service/GroupBuyService.java` | 团购 Service |
| `groupbuy/controller/GroupBuyController.java` | 团购 Controller |
| `product/ProductControllerTest.java` | 商品 Controller 测试（13 个） |
| `product/ProductServiceTest.java` | 商品 Service 测试（11 个） |
| `groupbuy/GroupBuyControllerTest.java` | 团购 Controller 测试（12 个） |
| `groupbuy/GroupBuyServiceTest.java` | 团购 Service 测试（13 个） |

### Modified files

| File | Change |
|---|---|
| `docs/openapi/groupshop-api.yaml` | 版本 0.3.0，新增 Product/GroupBuy 标签和路径，请求/响应 schema，错误响应 |
| `docs/前后端联调文档.md` | 新增 §4A 商品管理链路，更新 §5 发布与管理系统团购（含列表/详情/更新/结束） |
| `docs/dev/context/latest-context.md` | 本次更新 |

### Flyway 迁移

无新增迁移文件。

## 12. Problems found

None

## 13. Problems fixed

None

## 14. Remaining issues

None

## 15. Suggestions for next batch

- Batch 04 将实现公共浏览团购（首页团购流、团购详情、团长主页）
- `CurrentStoreHelper` 可在后续 batch 中继续复用
- 最小 Order/OrderItem 实体可在后续 batch 中扩展为完整订单模型
- 注意团购结束后的 `group_buys.status=ended` 状态在公共浏览接口中需作为不可购买条件
