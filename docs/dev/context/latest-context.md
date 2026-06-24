# Batch Context — Latest

> 记录最近完成的 batch 成果，供后续 batch 参考。
> 当前最新：**Batch 0 — 项目骨架、通用响应、错误码、数据库迁移、测试基础**

---

## 1. 已完成批次

| Batch | 状态 | 说明 |
|---|---|---|
| 0 | ✅ 完成 | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |

## 2. 项目结构

```
backend/
├── pom.xml                               # Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Flyway
├── docker-compose.yml                    # MySQL 8.0
├── src/main/java/com/example/groupshop/
│   ├── GroupshopApplication.java
│   ├── config/
│   │   ├── WebMvcConfig.java             # 空 WebMvcConfigurer
│   │   └── MyBatisPlusConfig.java        # MyBatis-Plus 分页插件
│   ├── common/
│   │   ├── response/
│   │   │   ├── ApiResponse.java          # 统一响应 {success, data/traceId, error/traceId}
│   │   │   └── PageResponse.java         # 分页 {items, page, pageSize, total, hasMore}
│   │   ├── enums/
│   │   │   └── ErrorCode.java            # 18 个错误码（通用 + 业务）
│   │   ├── exception/
│   │   │   ├── BusinessException.java    # 携带 ErrorCode
│   │   │   └── GlobalExceptionHandler.java # 处理 Validation/Business/404/405/500
│   │   └── trace/
│   │       ├── TraceIdFilter.java        # @Component Filter 生成 traceId
│   │       └── TraceIdAdvice.java        # ResponseBodyAdvice 注入 traceId
│   ├── controller/
│   │   └── HealthController.java         # GET /api/v1/health
│   └── testconfig/
│       └── TestValidationController.java # @Profile("test") 测试用校验控制器
├── src/main/resources/
│   ├── application.yml                   # 主配置（MySQL、Flyway、MVC）
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
        ├── common/
        │   ├── ErrorResponseTest.java     # 成功响应结构测试
        │   └── ValidationErrorTest.java   # @Valid 校验/非法JSON/未知路由测试
        └── migration/
            └── FlywayMigrationTest.java   # 18 个测试：12 表存在性 + 字段 + 唯一约束
```

## 3. 统一响应结构

### 成功响应
```json
{
  "success": true,
  "data": {},
  "traceId": "req_e9b818b0a8e044ae"
}
```

### 分页响应
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
  "traceId": "req_..."
}
```

### 错误响应
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "参数校验失败",
    "details": {}
  },
  "traceId": "req_..."
}
```

## 4. 错误码枚举

| HTTP | 错误码 | 说明 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 参数校验失败 |
| 401 | `UNAUTHORIZED` | 未登录或令牌无效 |
| 403 | `FORBIDDEN` | 无权限操作 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在 |
| 409 | `RESOURCE_CONFLICT` | 状态冲突或重复创建 |
| 422 | `BUSINESS_RULE_VIOLATION` | 不满足业务规则 |
| 500 | `INTERNAL_ERROR` | 服务端异常 |
| 409 | `STORE_ALREADY_EXISTS` | 已创建店铺 |
| 403 | `LEADER_REQUIRED` | 需要团长身份 |
| 403 | `STORE_FORBIDDEN` | 不能操作他人店铺 |
| 422 | `GROUP_BUY_NOT_PURCHASABLE` | 团购不可购买 |
| 422 | `GROUP_BUY_ENDED` | 团购已结束 |
| 422 | `ITEM_NOT_IN_GROUP_BUY` | 商品不属于该团购 |
| 403 | `ADDRESS_FORBIDDEN` | 地址不属于当前用户 |
| 422 | `ORDER_NOT_PAYABLE` | 订单不可支付 |
| 409 | `ORDER_ALREADY_PAID` | 订单已支付 |
| 422 | `ORDER_NOT_CANCELABLE` | 订单不可取消 |
| 422 | `ORDER_NOT_SHIPPABLE` | 订单不可发货 |
| 409 | `ORDER_ALREADY_SHIPPED` | 订单已发货 |
| 422 | `INSUFFICIENT_STOCK` | 库存不足 |
| 409 | `SUBSCRIPTION_EXISTS` | 已订阅该团长 |

## 5. API 约定

| 约定 | 规则 |
|---|---|
| 前缀 | `/api/v1` |
| 认证头 | `Authorization: Bearer <accessToken>` |
| 数据格式 | JSON，UTF-8 |
| 字段命名 | `camelCase` |
| 时间格式 | ISO 8601，如 `2026-06-24T12:00:00+08:00` |
| 金额格式 | 整数分，字段名以 `Amount` 结尾 |
| 列表路径 | `/my/store` 表示当前团长自己的店铺（服务端查 token 确定店铺） |
| HTTP 方法 | GET / POST / PATCH / DELETE，不使用 PUT |

## 6. 数据库：12 张 MVP 核心表

| 表 | 关键约束 | 说明 |
|---|---|---|
| `users` | — | 用户基础信息 |
| `leaders` | `UNIQUE(user_id)` | 团长身份 |
| `stores` | `UNIQUE(leader_id)` | 店铺 |
| `products` | — | 商品 |
| `group_buys` | — | 团购活动 |
| `group_buy_items` | — | 团购商品关系 |
| `addresses` | — | 收货地址 |
| `orders` | `UNIQUE(order_no)` | 订单，含地址快照 |
| `order_items` | — | 订单明细（商品快照） |
| `shipments` | — | 发货记录 |
| `subscriptions` | `UNIQUE(user_id, leader_id)` | 订阅关系 |
| `member_relations` | `UNIQUE(user_id, store_id)` | 会员关系 |

金额字段使用 `BIGINT` 保存分。所有表含 `created_at` / `updated_at`。

## 7. 测试基类使用

### MockMvcTestBase
```java
// 所有 Controller 测试继承此类
class MyControllerTest extends MockMvcTestBase {
    @Test
    void testSomething() {
        mockMvc.perform(get("/api/v1/..."))
               .andExpect(status().isOk())
               .andExpectAll(successResult());  // 验证 success=true + traceId
    }
}
```

### ServiceTestBase
```java
// 所有 Service 测试继承此类（加载全上下文 + H2 + Flyway）
class MyServiceTest extends ServiceTestBase {
    @Autowired private MyService myService;
    // ...
}
```

## 8. 测试命令

```bash
cd backend
mvn test                                    # 全部 25 个测试
mvn test --no-transfer-progress             # 减少下载日志
mvn test -Dtest=FlywayMigrationTest         # 单类测试
```

## 9. 本地开发数据库

```bash
cd backend
docker compose up -d                         # 启动 MySQL 8.0
docker compose down                          # 停止
docker compose down -v                       # 停止并清除数据
```

- 默认数据库：`groupshop`（主配置）/ `groupshop_dev`（dev profile）
- 账号：`root` / `root`
- 端口：`3306`

## 10. 联调文档更新

已在 `docs/前后端联调文档.md` 中维护：
- §1.1 基础信息
- §1.2 成功响应格式
- §1.3 错误响应格式
- §1.4 常用错误码
- §1.5 空库初始化说明（新增）

## 11. 下一批注意事项

- Batch 1 将实现 `POST /api/v1/auth/mock-login` 和 `GET /api/v1/me`。
- 涉及表：`users`、`leaders`、`stores`（已有 Flyway V1 创建）。
- 注意：Flyway V1 已创建所有 12 张表，后续 batch **不再** 为已知 MVP 表追加迁移，只在确有必要时追加新文件。
- 需要实现 MVP 模拟登录 token 生成和解析，当前无此能力。
- 测试基类 `MockMvcTestBase` 和 `ServiceTestBase` 可直接复用。
