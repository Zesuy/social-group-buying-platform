# Backend

`backend/` 是邻鲜团的 Spring Boot REST API 服务，负责用户、店铺、商品、团购、订单、支付、售后、通知、聊天、上传资产等业务能力。

## 技术栈

- Java 17
- Spring Boot 3.2
- Spring Web + Validation
- MyBatis-Plus
- MySQL 8
- Flyway
- JUnit 5 + MockMvc + H2
- 支付宝沙箱 SDK

## 目录说明

```text
src/main/java/com/example/groupshop/
  auth/          登录与认证
  store/         店铺资料与经营信息
  leader/        团长身份与团长主页
  product/       商品库
  category/      商品分类
  groupbuy/      团购发布、管理、公开浏览
  cart/          购物车
  order/         订单、支付完成、发货
  payment/       统一支付入口、支付宝沙箱通知
  coupon/        优惠券和红包抵扣
  member*/       会员卡、会员成长和等级
  aftersale/     售后与模拟退款
  notification/  站内通知
  chat/          订单上下文轻量聊天
  upload/        图片上传和资产治理
  model/         MyBatis-Plus Entity 与 Mapper
src/main/resources/
  application.yml
  application-dev.yml
  db/migration/  Flyway 迁移
src/test/java/   MockMvc、Service、合同和迁移测试
scripts/         本地演示数据脚本
```

## 常用命令

```bash
cd backend
mvn spring-boot:run
mvn test
```

健康检查：

```text
GET http://localhost:8080/api/v1/health
```

## 数据库

默认开发库为 MySQL `groupshop`。启动后 Flyway 自动执行：

```text
src/main/resources/db/migration/
```

演示数据脚本在 `scripts/`，说明见 [scripts/README.md](scripts/README.md)。

## 支付和 AI 配置

默认支付为本地模拟支付：

```env
SANDBOX_PAYMENT_ENABLED=false
```

启用支付宝沙箱需要设置 `ALIPAY_SANDBOX_APP_ID`、`ALIPAY_SANDBOX_APP_PRIVATE_KEY`、`ALIPAY_SANDBOX_PUBLIC_KEY`、`BACKEND_PUBLIC_BASE_URL` 和 `FRONTEND_PUBLIC_BASE_URL`。

发布团购 AI 润色默认使用本地规则兜底；设置 `GROUPSHOP_AI_POLISH_PROVIDER=openai` 且提供 `OPENAI_API_KEY` 后可走 OpenAI Chat Completions。

## 测试要求

- Controller 接口使用 MockMvc 覆盖。
- 关键 Service 覆盖权限、状态流转、库存、重复动作和异常路径。
- Flyway 迁移、OpenAPI 合同和长 ID JSON 序列化有独立测试。
