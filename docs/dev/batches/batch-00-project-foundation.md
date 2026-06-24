# Batch 0：项目骨架、通用响应、错误码、数据库迁移、测试基础

## 1. 目标

搭建可运行、可测试、可迁移的后端基础工程，并一次性创建 MVP 核心表结构。

## 2. 前置条件

已阅读 `AGENTS.md`、`docs/MVP开发指南.md`、`docs/数据模型设计.md`、`docs/API风格规范.md` 和 `docs/dev/backend-tech-stack.md`。本批为第一个后端 batch，无业务接口前置依赖。

## 3. 涉及接口

暂无业务接口；可提供健康检查接口用于启动验证。

## 4. 涉及数据表

`users`、`leaders`、`stores`、`products`、`group_buys`、`group_buy_items`、`addresses`、`orders`、`order_items`、`shipments`、`subscriptions`、`member_relations`。

## 5. 实现任务

初始化 Spring Boot + Maven；配置 Java 17、MySQL、MyBatis-Plus、Flyway；定义统一响应、错误响应、错误码枚举、全局异常处理、traceId；配置 Validation；建立 MockMvc 和 Service 测试基类；按 `docs/数据模型设计.md` 创建全部 MVP 核心表、主键、必要唯一约束和基础索引；后续 batch 只在确有必要时追加迁移。

## 6. 测试要求

骨架启动测试；统一错误响应测试；参数校验错误测试；Flyway 空库迁移测试；核心表存在性、关键字段、唯一约束和基础索引校验。

## 7. 验收标准

应用可启动；测试可运行；错误响应符合 `docs/API风格规范.md`；空库执行 Flyway 后一次性具备全部 MVP 核心表；后续 batch 原则上不再为已知 MVP 表反复补基础结构。

## 8. 联调文档更新项

在 `docs/前后端联调文档.md` 补充通用响应格式、认证请求头、错误响应格式、本地联调基础约定和空库初始化说明。

## 9. 禁止事项

不实现非本批业务接口；不扩大 MVP 范围；不跳过测试；不把已知问题留到下一批；不私自修改 API 字段、状态枚举、错误码、金额口径；需要修改上游文档时，必须明确说明原因。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
