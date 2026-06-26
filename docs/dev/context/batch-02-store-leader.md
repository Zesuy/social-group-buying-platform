# Batch Context — Latest

> 记录最近完成的 batch 成果，供后续 batch 参考。
> 当前最新：**Batch 1 — 模拟登录与当前用户**

---

## 1. 已完成批次

| Batch | 状态 | 说明 |
|---|---|---|
| 0 | ✅ 完成 | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |
| 1 | ✅ 完成 | 模拟登录、当前用户、token 认证拦截 |

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
│   │   │   └── ErrorCode.java            # 18+ 个错误码（通用 + 业务）
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
│   │   │   └── CurrentUserResponse.java  # {user, leader?, store?}
│   │   └── service/
│   │       └── AuthService.java          # 模拟登录（查找或创建用户）、当前用户身份组装
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
        │   ├── MockMvcTestBase.java       # @SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test")
        │   └── ServiceTestBase.java       # @SpringBootTest + @ActiveProfiles("test")
        ├── GroupshopApplicationTests.java # 上下文加载测试
        ├── auth/
        │   ├── AuthControllerTest.java    # 10 个测试：mock-login（创建/复用/默认昵称/校验）、/me（正常/无token/无效token/空token/格式错误）
        │   └── AuthServiceTest.java       # 6 个测试：用户创建/复用/默认昵称/token 存/取/解析/当前用户（没店铺/不存在）
        ├── common/
        │   ├── ErrorResponseTest.java     # 成功响应结构测试
        │   └── ValidationErrorTest.java   # @Valid 校验/非法JSON/未知路由测试
        └── migration/
            └── FlywayMigrationTest.java   # 18 个测试：12 表存在性 + 字段 + 唯一约束
```

## 3. Batch 1 实现的接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/auth/mock-login` | 模拟登录，按手机号查找或创建用户，返回 accessToken |
| GET | `/api/v1/me` | 获取当前用户上下文（user、leader、store 摘要） |

### 认证机制

- **token 格式**：`mock_token_{UUID}`，存储在内存 `ConcurrentHashMap` 中
- **token 传递**：`Authorization: Bearer <accessToken>`
- **拦截路径**：`/api/v1/**`（排除 `/api/v1/health`、`/api/v1/auth/mock-login`、`/api/v1/_test/**`）
- **用户身份**：拦截器解析 token 后将 `userId` 设置为 request attribute `currentUserId`
- **401 状态**：`AuthInterceptor` 抛出 `BusinessException(UNAUTHORIZED)`，`GlobalExceptionHandler` 设置 HTTP 401

### 模拟登录逻辑

1. 按 `phone` 查找用户
2. 不存在则创建新用户（`nickname` 可选，默认 `用户{手机号后4位}`）
3. 生成 token 并存储 `token → userId` 映射
4. 查询用户是否有 leader/store 身份，返回摘要信息

## 4. 统一响应结构

未变更，同 Batch 0。

## 5. 错误码枚举

`GlobalExceptionHandler.handleBusiness()` 修复为动态设置 HTTP 状态（原为无 `@ResponseStatus` 导致 BusinessException 总是返回 200）。

## 6. 测试命令

```bash
cd backend
mvn test                                    # 全部 41 个测试
mvn test -Dtest=AuthControllerTest          # 单类测试
mvn test -Dtest=AuthServiceTest             # 单类测试
```

## 7. 测试结果

```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
```

## 8. 联调文档更新

已在 `docs/前后端联调文档.md` 中更新：
- §3.2 响应样例 — 分开列出 `POST /api/v1/auth/mock-login` 和 `GET /api/v1/me` 的响应格式
- §3.2 增加已创建店铺用户的 `GET /api/v1/me` 响应样例
- 增加 `leader` 和 `store` 为 null 时的行为说明

## 9. 本地开发数据库

不变，同 Batch 0。

## 10. Files created or modified

### New files

| File | Description |
|---|---|
| `model/entity/User.java` | User 实体 |
| `model/entity/Leader.java` | Leader 实体 |
| `model/entity/Store.java` | Store 实体 |
| `model/mapper/UserMapper.java` | User Mapper |
| `model/mapper/LeaderMapper.java` | Leader Mapper |
| `model/mapper/StoreMapper.java` | Store Mapper |
| `auth/TokenStore.java` | MVP 内存 token 存储 |
| `auth/AuthInterceptor.java` | Bearer token 拦截器 |
| `auth/controller/AuthController.java` | 认证 Controller |
| `auth/dto/MockLoginRequest.java` | 登录请求 DTO |
| `auth/dto/MockLoginResponse.java` | 登录响应 DTO |
| `auth/dto/CurrentUserResponse.java` | 当前用户响应 DTO |
| `auth/service/AuthService.java` | 认证 Service |
| `auth/AuthControllerTest.java` | Controller 测试 |
| `auth/AuthServiceTest.java` | Service 测试 |

### Modified files

| File | Change |
|---|---|
| `config/WebMvcConfig.java` | 注册 AuthInterceptor，配置拦截/放行路径 |
| `common/exception/GlobalExceptionHandler.java` | `handleBusiness()` 注入 `HttpServletResponse` 动态设置 HTTP 状态 |
| `docs/前后端联调文档.md` | 更新 §3 响应样例 |

### Flyway 迁移

无新增迁移文件。

## 11. Problems found

1. `GlobalExceptionHandler.handleBusiness()` 缺少 `@ResponseStatus` 且未设置 HTTP 状态，导致 BusinessException 始终返回 200。已修复：注入 `HttpServletResponse` 并在 handler 中设置 `response.setStatus(ex.getHttpStatus())`。

## 12. Problems fixed

- GlobalExceptionHandler BusinessException HTTP 状态动态化
- WebMvcConfig 新增 `/_test/**` 排除路径，防止拦截器影响测试控制器

## 13. Remaining issues

None

## 14. Suggestions for next batch

- Batch 2 将实现 `POST /api/v1/stores` 创建店铺接口
- 修改了 `GlobalExceptionHandler`，后续 batch 注意所有 BusinessException 都会返回正确的 HTTP 状态码
- MockMvcTestBase 可直接复用，注意需要 token 的测试先调用模拟登录
