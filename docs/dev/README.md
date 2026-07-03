# 开发执行文档索引

> 定位：本文是 AI 逐 batch 开发时的入口索引。MVP 总览见 `docs/MVP开发指南.md`，实际执行以 `docs/dev/batches/` 下的单 batch 文件为准。

## 1. 推荐阅读顺序

每次开始开发一个 batch 前，优先阅读：

1. `AGENTS.md`
2. `docs/功能需求定义.md`
3. `docs/数据模型设计.md`
4. `docs/API风格规范.md`
5. `docs/API设计.md`
6. `docs/页面与交互文档.md`
7. `docs/前后端联调文档.md`
8. 当前 batch 文件

通用规则文件按需阅读：

| 文件 | 用途 |
|---|---|
| `docs/dev/backend-tech-stack.md` | 后端技术栈、包结构、响应、错误码、认证、金额、状态、迁移约定 |
| `docs/dev/h5-style-guide.md` | H5 移动端风格和 Vant 使用口径 |
| `docs/dev/frontend-ai-workflow.md` | 前端页面级 AI 工作流：需求澄清、技术方案、实现验收 |
| `docs/dev/testing-rules.md` | MockMvc、Service、事务、异常、权限、快照、库存等测试规则 |
| `docs/dev/integration-maintenance.md` | 每个 batch 完成后如何更新联调文档 |

## 2. 执行顺序

必须按以下顺序执行，不得跳 batch：

| 顺序 | 文件 | 目标 |
|---:|---|---|
| 0 | `docs/dev/batches/batch-00-project-foundation.md` | 项目骨架、通用响应、错误码、数据库迁移、测试基础 |
| 1 | `docs/dev/batches/batch-01-auth-current-user.md` | 模拟登录与当前用户 |
| 2 | `docs/dev/batches/batch-02-store-leader.md` | 创建店铺与激活团长身份 |
| 3A | `docs/dev/batches/batch-03a-product-management.md` | 商品管理 |
| 3B | `docs/dev/batches/batch-03b-groupbuy-publish.md` | 发布普通团购 |
| 3C | `docs/dev/batches/batch-03c-groupbuy-management.md` | 团购管理 |
| 4 | `docs/dev/batches/batch-04-public-browsing.md` | 公共浏览，首页团购列表、团购详情、团长主页 |
| 5 | `docs/dev/batches/batch-05-address-order-preview.md` | 地址管理与订单预览 |
| 6 | `docs/dev/batches/batch-06-order-create.md` | 创建订单，地址快照、商品快照、订单明细 |
| 7 | `docs/dev/batches/batch-07-simulate-payment-member.md` | 模拟支付，状态校验、库存扣减、销量增加、会员关系创建 / 更新 |
| 8 | `docs/dev/batches/batch-08-leader-orders-shipping.md` | 团长订单管理与发货 |
| 9 | `docs/dev/batches/batch-09-subscription-member-cards.md` | 订阅团长与会员卡展示 |
| 10 | `docs/dev/batches/batch-10-regression-freeze.md` | 全链路回归、接口文档冻结、联调准备 |

## 3. 执行原则

| 原则 | 要求 |
|---|---|
| 当前批次优先 | 只实现当前 batch 的接口和业务规则 |
| 不扩大范围 | 不实现真实微信支付、优惠券、售后退款、购物车、帮卖分销、积分商城、公众号推送、平台后台 |
| 测试同步 | 不把测试留到下一批 |
| 联调同步 | 每个 batch 完成后更新 `docs/前后端联调文档.md` |
| 文档一致 | 如需修改上游文档，必须说明原因 |
