# 开发执行文档索引

> 定位：本文是 AI 开发和维护时的入口索引。`docs/dev/batches/` 保留为历史批次归档；当前新功能、修复和文档校准以 `AGENTS.md`、核心产品 / API 文档、实际代码契约和用户明确需求为准。

## 1. 推荐阅读顺序

每次开始开发或文档校准前，优先阅读：

1. `AGENTS.md`
2. `docs/功能需求定义.md`
3. `docs/数据模型设计.md`
4. `docs/API风格规范.md`
5. `docs/API设计.md`
6. `docs/页面与交互文档.md`
7. `docs/前后端联调文档.md`
8. 与当前任务相关的历史 batch 文件（如需要）

通用规则文件按需阅读：

| 文件 | 用途 |
|---|---|
| `docs/dev/backend-tech-stack.md` | 后端技术栈、包结构、响应、错误码、认证、金额、状态、迁移约定 |
| `docs/dev/h5-style-guide.md` | H5 移动端风格和 Vant 使用口径 |
| `docs/dev/frontend-ai-workflow.md` | 前端页面级 AI 工作流：需求澄清、技术方案、实现验收 |
| `docs/dev/testing-rules.md` | MockMvc、Service、事务、异常、权限、快照、库存等测试规则 |
| `docs/dev/integration-maintenance.md` | 页面、链路或功能闭环完成后如何更新联调文档 |

## 2. 当前执行方式

MVP 和 P1 后端批次已完成，项目不再按下表强制顺序推进。后续工作按用户明确指定的页面、链路、接口或功能闭环推进；如果任务涉及旧能力状态、边界或验收口径，再查阅对应历史批次文件。

历史批次索引：

| 顺序 | 文件 | 目标 |
|---:|---|---|
| MVP | `docs/dev/batches/backend-batches/MVP/` | 后端核心交易闭环 |
| MVP | `docs/dev/batches/frontend-batches/MVP/` | 前端核心页面、联调和视觉基线 |
| P1 | `docs/dev/batches/backend-batches/P1/` | 商品库、购物车、优惠券、售后、通知、上传资产等增强能力 |
| P1 | `docs/dev/batches/frontend-batches/P1/` | 前端消息、上传和 P1 页面接入口径 |

## 3. 执行原则

| 原则 | 要求 |
|---|---|
| 当前范围优先 | 只实现用户明确指定的页面、链路、接口或修复范围 |
| 以事实为准 | 先核对代码、API 文档和联调文档，不把已实现能力误判为占位或越界 |
| 不扩大范围 | 不实现真实微信支付、帮卖分销、积分商城、公众号推送、平台后台、复杂客服中心等未落地能力 |
| 测试同步 | 不把测试留到下一批 |
| 联调同步 | 每个页面、链路或功能闭环完成后更新 `docs/前后端联调文档.md` |
| 文档一致 | 如需修改上游文档，必须说明原因 |
