# Frontend Batch 01：应用壳、5 Tab、登录态与模拟登录

## 1. 目标

完成 H5 应用主框架、底部 5 Tab、登录态恢复和模拟登录，使后续所有页面具备统一进入、守卫和登录拦截能力。

## 2. 前置条件

Frontend Batch 00 已完成；后端认证与当前用户接口可用；已确认 `docs/API设计.md` 中认证响应结构和 `docs/前后端联调文档.md` 中登录链路一致。

## 3. 涉及接口

- `POST /api/v1/auth/mock-login`
- `GET /api/v1/me`

## 4. 页面范围

- `/`
- `/orders`
- `/open-group`
- `/messages`
- `/profile`
- `/login`

本批只完成 5 Tab 壳、消息空态、登录页和登录态展示；业务列表和交易链路在后续 batch 实现。

## 5. 实现任务

封装 Axios 实例，完成 `baseURL`、`Authorization` 注入、响应解包、错误标准化和错误码中文映射；实现 `authStore`，维护 `accessToken`、`user`、`leader`、`store`、`login`、`logout`、`fetchMe`、`ensureLogin`；实现应用启动时 token 恢复并调用 `GET /api/v1/me`；实现路由守卫 `requiresAuth`、`requiresLeader` 和登录后重定向；实现 `PageLayout`、`BottomTabBar`、`NavBar`、`FixedActionBar`、`LoadingView`、`ErrorView`、`EmptyState`、`ReminderBanner` 等基础组件；完成底部固定 5 Tab：首页、订单、一键开团、消息、个人中心；完成消息页空态，不调用任何消息接口；完成模拟登录页，支持买家测试用户和团长测试用户快捷填充。

## 6. 测试要求

单元测试覆盖 API 错误解析、token 保存与恢复、`authStore` 登录和退出；组件测试覆盖底部 Tab 高亮、消息空态、登录表单校验；路由测试覆盖未登录访问需登录页面时跳转登录、登录后恢复目标路由。

## 7. 验收标准

应用打开后有稳定的 5 Tab 移动端框架；未登录进入受保护页面会被拦截到 `/login`；模拟登录成功后保存 token 并刷新当前用户；刷新页面可恢复登录态；消息页为空态且不请求不存在的接口。

## 8. 联调文档更新项

更新 `docs/前后端联调文档.md` 中“模拟登录与获取当前用户”链路，补充前端页面入口、调用顺序、登录后恢复行为、测试用户快捷填充说明和常见错误展示方式。

## 9. 禁止事项

不实现真实微信登录；不接公众号消息；不新增消息接口；不把未登录用户静默当作已登录；不绕过后端 `GET /api/v1/me` 判断团长和店铺身份；不实现非 MVP Tab。

## 10. 本批完成后必须输出的结果

- 已实现页面 / 能力清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
