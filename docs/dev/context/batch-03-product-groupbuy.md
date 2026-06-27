# Batch Context — Latest

> 记录最近完成的 batch 成果，供后续 batch 参考。
> 当前最新：**Batch 2 — 创建店铺与激活团长身份**

---

## 1. 已完成批次

| Batch | 状态 | 说明 |
|---|---|---|
| 0 | ✅ 完成 | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |
| 1 | ✅ 完成 | 模拟登录、当前用户、token 认证拦截 |
| 2 | ✅ 完成 | 创建店铺、获取/更新当前用户店铺、激活团长身份 |

## 2. 项目结构

```
backend/
├── pom.xml                               # Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Flyway
├── docker-compose.yml                    # MySQL 8.0
├── src/main/java/com/example/groupshop/
│   ├── GroupshopApplication.java
│   ├── config/
│   │   ├── WebMvcConfig.java             # 注册 AuthInterceptor，排除 /health、/auth/mock-login、/_test/**
│   │   └── MyBatisPlusConfig.java        # MyBatis-Plus 分页插件
│   ├── common/
│   │   ├── response/
│   │   │   ├── ApiResponse.java          # 统一响应 {success, data/traceId, error/traceId}
│   │   │   └── PageResponse.java         # 分页 {items, page, pageSize, total, hasMore}
│   │   ├── enums/
│   │   │   ├── ErrorCode.java            # 18+ 个错误码（通用 + 业务）
│   │   │   └── DeliveryType.java         # 配送方式枚举（express/pickup/local_delivery），@JsonCreator/@JsonValue
│   │   ├── exception/
│   │   │   ├── BusinessException.java    # 携带 ErrorCode
│   │   │   └── GlobalExceptionHandler.java # 处理 Validation/Business/404/405/500，BusinessException 动态设置 HTTP 状态
│   │   └── trace/
│   │       ├── TraceIdFilter.java        # @Component Filter 生成 traceId
│   │       └── TraceIdAdvice.java        # ResponseBodyAdvice 注入 traceId
│   ├── model/
│   │   ├── entity/
│   │   │   ├── User.java                 # users 表实体
│   │   │   ├── Leader.java               # leaders 表实体
│   │   │   └── Store.java                # stores 表实体
│   │   └── mapper/
│   │       ├── UserMapper.java           # MyBatis-Plus BaseMapper
│   │       ├── LeaderMapper.java
│   │       └── StoreMapper.java
│   ├── auth/
│   │   ├── TokenStore.java               # MVP 内存级 token 存储（ConcurrentHashMap）
│   │   ├── AuthInterceptor.java           # Bearer token 拦截 /api/v1/**，排除公开路径
│   │   ├── controller/
│   │   │   └── AuthController.java       # POST /api/v1/auth/mock-login, GET /api/v1/me
│   │   ├── dto/
│   │   │   ├── MockLoginRequest.java     # {phone, nickname, avatarUrl}
│   │   │   ├── MockLoginResponse.java    # {accessToken, user: {id, nickname, avatarUrl, phone, hasLeader, leaderId, storeId}}
│   │   │   └── CurrentUserResponse.java  # {user, leader?, store?} — StoreSummary 含 status 字段
│   │   └── service/
│   │       └── AuthService.java          # 模拟登录、当前用户身份组装，返回 status
│   ├── store/
│   │   ├── controller/
│   │   │   └── StoreController.java      # POST /api/v1/stores, GET /api/v1/my/store, PATCH /api/v1/my/store
│   │   ├── service/
│   │   │   └── StoreService.java         # 创建/获取/更新店铺，事务内创建或复用 leader
│   │   └── dto/
│   │       ├── CreateStoreRequest.java   # {name, logoUrl?, description?, defaultDeliveryType}
│   │       ├── UpdateStoreRequest.java   # {name?, logoUrl?, description?, defaultDeliveryType?}
│   │       └── StoreResponse.java        # {leader: {id, displayName, avatarUrl}, store: {id, leaderId, ...}}
│   ├── controller/
│   │   └── HealthController.java         # GET /api/v1/health
│   └── testconfig/
│       └── TestValidationController.java # @Profile("test") 测试用校验控制器
├── src/main/resources/
│   ├── application.yml                   # 主配置（MySQL、Flyway、MVC、Jackson non_null）
│   ├── application-dev.yml               # dev profile（groupshop_dev 库）
│   └── db/migration/
│       └── V1__create_mvp_tables.sql      # 12 张 MVP 核心表
└── src/test/
    ├── resources/application-test.yml     # H2 内存数据库、MySQL 兼容模式
    └── java/com/example/groupshop/
        ├── base/
        │   ├── MockMvcTestBase.java       # @SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test")，含 extractToken 辅助方法
        │   └── ServiceTestBase.java       # @SpringBootTest + @ActiveProfiles("test")
        ├── GroupshopApplicationTests.java # 上下文加载测试
        ├── auth/
        │   ├── AuthControllerTest.java    # 10 个测试
        │   └── AuthServiceTest.java       # 6 个测试
        ├── store/
        │   ├── StoreControllerTest.java   # 12 个测试：创建/获取/更新店铺接口完整覆盖
        │   └── StoreServiceTest.java      # 9 个测试：创建/复用/获取/更新 leader+store 逻辑
        ├── common/
        │   ├── ErrorResponseTest.java     # 成功响应结构测试
        │   └── ValidationErrorTest.java   # @Valid 校验/非法JSON/未知路由测试
        └── migration/
            └── FlywayMigrationTest.java   # 18 个测试：12 表存在性 + 字段 + 唯一约束
```

## 3. Batch 2 实现的接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/stores` | 创建店铺，事务内创建或复用 leader，再创建 store |
| GET | `/api/v1/my/store` | 获取当前用户店铺，未创建时返回 200 省略 data |
| PATCH | `/api/v1/my/store` | 局部更新当前用户店铺，同步 leader 展示字段 |

### 店铺创建逻辑

1. 检查当前用户是否已有 leader（通过 `userId` 查询 `leaders` 表）
2. 如果已有 leader 且已有 store，返回 409 `STORE_ALREADY_EXISTS`
3. 如果已有 leader 但无 store（orphan leader），复用该 leader 并更新其展示字段
4. 如果无 leader，创建新 leader（`displayName=name`、`avatarUrl=logoUrl`、`bio=description`、`serviceStatus=normal`、计数为 0）
5. 创建 store（`distributionEnabled=false`、`status=active`）

### 店铺更新逻辑

- PATCH 局部更新：只更新非 null 字段
- 店铺名称/logo/简介变更时同步到 leader 的 displayName/avatarUrl/bio
- 未创建店铺时返回 403 `LEADER_REQUIRED`

### 认证机制

未变更，同 Batch 1。

## 4. 统一响应结构

未变更，同 Batch 0。

## 5. 错误码枚举

`DeliveryType` 枚举新增在 `common/enums/` 包下，未新增 ErrorCode。

## 6. 测试命令

```bash
cd backend
mvn test                                    # 全部 67+ 个测试
mvn test -Dtest=StoreControllerTest          # 单类测试
mvn test -Dtest=StoreServiceTest             # 单类测试
```

## 7. 测试结果

```
Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
```

## 8. 联调文档更新

已在 `docs/前后端联调文档.md` 中更新：
- §4 创建店铺链路拆分：成功判定、获取当前用户店铺、更新当前用户店铺
- §4.4 获取当前用户店铺的请求/响应样例（含未创建场景省略 data，已有店铺返回 `{ leader, store }`）
- §4.6 更新当前用户店铺的请求/响应样例
- §4.7 错误码补充 `LEADER_REQUIRED`
- §4.8 测试数据准备方式

## 9. 本地开发数据库

不变，同 Batch 0。

## 10. Files created or modified

### New files

| File | Description |
|---|---|
| `common/enums/DeliveryType.java` | 配送方式枚举，@JsonCreator/@JsonValue |
| `store/controller/StoreController.java` | 店铺 Controller |
| `store/service/StoreService.java` | 店铺 Service |
| `store/dto/CreateStoreRequest.java` | 创建店铺请求 DTO |
| `store/dto/UpdateStoreRequest.java` | 更新店铺请求 DTO |
| `store/dto/StoreResponse.java` | 店铺响应 DTO（含 leader + store） |
| `store/StoreServiceTest.java` | StoreService 测试（9 个） |
| `store/StoreControllerTest.java` | StoreController 测试（16 个） |

### Modified files

| File | Change |
|---|---|
| `auth/dto/CurrentUserResponse.java` | StoreSummary 新增 `status` 字段 |
| `auth/service/AuthService.java` | getCurrentUser 返回店铺 `status` |
| `base/MockMvcTestBase.java` | 新增 `extractToken()` 辅助方法 |
| `docs/openapi/groupshop-api.yaml` | 新增 Store tag、3 个路径、请求/响应 schema、错误响应 |
| `docs/前后端联调文档.md` | 更新 §4 创建店铺链路，补充获取/更新店铺 |
| `docs/dev/context/latest-context.md` | 本次更新 |

### Flyway 迁移

无新增迁移文件。

## 11. Problems found

None

## 12. Problems fixed

- `CurrentUserResponse.StoreSummary` 缺少 `status` 字段，导致创建店铺后 `GET /api/v1/me` 响应中店铺缺少状态信息。已修复。
- `GET /api/v1/my/store` 最初返回纯 `StoreInfo` 对象，与上游 API 设计（返回 `{ leader, store }`）不符。已修复为返回 `StoreResponse`，同步更新 OpenAPI `MyStoreSuccessResponse.data` 指向 `StoreResponseData`，并更新联调文档。
- `UpdateStoreRequest.logoUrl` 和 `description` 最初缺少 `@Size(min = 1)` 校验，导致空字符串可通过校验并持久化。已补全。
- 更新店铺时 "leader 存在但 store 不存在" 的分支缺少测试覆盖。已新增 `updateMyStore_shouldThrowWhenNoStoreButHasOrphanLeader` 测试。

## 13. Remaining issues

None

## 14. Suggestions for next batch

- Batch 3 将实现发布普通团购接口 `POST /api/v1/my/store/group-buys`
- 店铺创建逻辑中的 orphan leader 复用机制可供后续 batch 参考
- 注意 PATCH 局部更新时，校验注解 `@Size(min = 1)` 只在校验路径被触发时生效（例如 `@Valid` 且有值传入）
