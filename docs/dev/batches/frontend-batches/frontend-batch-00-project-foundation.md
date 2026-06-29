# Frontend Batch 00：前端工程骨架、设计系统与测试基础

## 1. 目标

搭建可运行、可测试、可构建的 H5 前端基础工程，为后续页面和联调提供统一技术底座。

## 2. 前置条件

后端 Batch 10 已完成，MVP 接口冻结；已阅读 `AGENTS.md`、`docs/dev/frontend-mvp-plan-revised.md`、`docs/dev/h5-style-guide.md`、`docs/API风格规范.md`、`docs/API设计.md` 和 `docs/前后端联调文档.md`。

## 3. 涉及接口

本批不联调业务接口；仅预留 `VITE_API_BASE_URL` 和 `/api/v1` 请求前缀配置。

## 4. 页面范围

暂无完整业务页面；只完成应用入口、空白路由占位和全局基础样式。

## 5. 实现任务

在 `frontend/` 初始化 Vue 3 + Vite + TypeScript 工程；接入 Vue Router、Pinia、Vant、Axios、Vitest、Playwright、ESLint、Prettier；建立 `src/api`、`src/components`、`src/composables`、`src/constants`、`src/router`、`src/stores`、`src/styles`、`src/types`、`src/utils`、`src/views` 目录；配置移动端 viewport、safe-area、浅灰背景、绿色主色、价格色和基础 CSS 变量；封装 `formatAmount`、状态文案映射、非 MVP 入口灰态判断等基础工具；建立 `ApiResponse<T>`、`PageResponse<T>`、`ApiError` 等通用类型；准备空页面路由占位，确保应用可启动。

## 6. 测试要求

补充金额格式化、状态映射、非 MVP 入口灰态判断的单元测试；补充应用启动 smoke test；确保 `npm run typecheck`、`npm run lint`、`npm run test:unit`、`npm run build` 可执行。

## 7. 验收标准

`frontend/` 可独立安装依赖、启动开发服务、运行测试和构建；基础样式符合 H5 移动端口径；金额不使用浮点数展示；后续 batch 可以直接复用目录、类型、工具和测试命令。

## 8. 联调文档更新项

在 `docs/前后端联调文档.md` 补充前端本地启动方式、环境变量、测试命令和 H5 页面通用约定。

## 9. 禁止事项

不实现业务闭环页面；不新增后端接口；不引入真实微信支付、购物车、优惠券、售后退款、帮卖分销、积分商城、公众号推送、平台后台；不把金额当浮点数处理；不跳过测试基础配置。

## 10. 本批完成后必须输出的结果

- 已实现页面 / 能力清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
