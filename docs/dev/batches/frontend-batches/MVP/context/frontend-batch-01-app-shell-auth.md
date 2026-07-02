# Frontend Batch 01 Handoff

> 本批实现时间：2026-06-29

## 已实现页面 / 能力清单

| 页面 / 能力 | 状态 | 说明 |
|---|---|---|
| 底部 5 Tab 导航 | ✅ 完成 | 首页、订单、一键开团、消息、我的 |
| 路由守卫（requiresAuth） | ✅ 完成 | 未登录重定向到 `/login` |
| 路由守卫（requiresLeader） | ✅ 完成 | 非团长重定向到 `/store/create` |
| 登录后重定向 | ✅ 完成 | 支持 `redirect` 查询参数 |
| 启动恢复登录态 | ✅ 完成 | 读取 token -> `GET /api/v1/me` |
| 模拟登录页 | ✅ 完成 | 手机号/昵称/头像表单 + 测试用户快捷填充 |
| 消息页空态 | ✅ 完成 | 空状态 + 提示横幅，不请求消息 API |
| API 错误标准化 | ✅ 完成 | 后端/网络/超时错误统一为 ApiError |
| 错误码中文映射 | ✅ 完成 | MVP 常用错误码映射 |
| 基础组件库 | ✅ 完成 | PageLayout、BottomTabBar、NavBar、FixedActionBar、LoadingView、ErrorView、EmptyState、ReminderBanner |

## 已新增 / 修改文件清单

### 新增文件

| 文件 | 说明 |
|---|---|
| `src/api/auth.ts` | 认证 API（mockLogin、fetchMe） |
| `src/api/errors.ts` | 错误标准化和错误码中文映射 |
| `src/stores/auth.ts` | 认证状态管理（Pinia） |
| `src/components/PageLayout.vue` | 页面布局容器 |
| `src/components/BottomTabBar.vue` | 底部 5 Tab 导航 |
| `src/components/NavBar.vue` | 顶部导航栏 |
| `src/components/FixedActionBar.vue` | 底部固定操作栏 |
| `src/components/LoadingView.vue` | 加载中状态 |
| `src/components/ErrorView.vue` | 错误状态 + 重试 |
| `src/components/EmptyState.vue` | 空状态 |
| `src/components/ReminderBanner.vue` | 提示横幅 |
| `tests/unit/errors.test.ts` | 错误处理单元测试 |
| `tests/unit/auth-store.test.ts` | authStore 单元测试 |
| `tests/unit/bottom-tab-bar.test.ts` | BottomTabBar 组件测试 |
| `tests/unit/login-view.test.ts` | LoginView 组件测试 |
| `tests/unit/messages-view.test.ts` | MessagesView 组件测试 |
| `tests/unit/router-guards.test.ts` | 路由守卫测试 |
| `tests/unit/setup.ts` | Vitest 测试设置（Vant 组件注册） |

### 修改文件

| 文件 | 说明 |
|---|---|
| `src/types/api.ts` | 添加认证/用户类型定义 |
| `src/types/index.ts` | 导出新类型 |
| `src/api/index.ts` | 导出 authApi、errors |
| `src/api/request.ts` | 解包响应数据，使用标准化错误处理 |
| `src/stores/index.ts` | 导出 useAuthStore |
| `src/router/index.ts` | 完整路由守卫实现 |
| `src/main.ts` | 添加启动恢复登录态 |
| `src/App.vue` | 添加 BottomTabBar |
| `src/views/LoginView.vue` | 完整模拟登录表单 |
| `src/views/MessagesView.vue` | 空态 + 提示横幅 |
| `src/views/IndexView.vue` | 使用 PageLayout |
| `src/views/OrdersView.vue` | 使用 PageLayout |
| `src/views/OpenGroupView.vue` | 使用 PageLayout |
| `src/views/ProfileView.vue` | 使用 PageLayout |
| `vitest.config.ts` | 添加 setupFiles |
| `docs/前后端联调文档.md` | 添加前端页面入口、启动恢复、路由守卫、错误展示说明 |

## 已新增 / 修改测试清单

| 测试文件 | 用例数 | 说明 |
|---|---|---|
| `tests/unit/errors.test.ts` | 10 | ApiError 创建、错误码映射、标准化 |
| `tests/unit/auth-store.test.ts` | 13 | 登录/退出/fetchMe/restoreSession + fetchMe 失败回滚 |
| `tests/unit/bottom-tab-bar.test.ts` | 2 | 5 Tab 渲染和标签 |
| `tests/unit/login-view.test.ts` | 4 | 表单渲染、快捷填充 |
| `tests/unit/messages-view.test.ts` | 3 | 空态、横幅、无 API 请求 |
| `tests/unit/router-guards.test.ts` | 8 | 未登录拦截、非团长拦截、已登录跳过登录页 |
| `tests/e2e/smoke.spec.ts` | 6 | 应用加载、5 Tab 壳、未登录拦截、登录页、登录流程、消息页空态 |

## 已更新联调文档的位置

- `docs/前后端联调文档.md` 第 3 节 "模拟登录与获取当前用户"
  - 更新页面入口说明
  - 添加启动恢复描述（3.2 下方）
  - 添加新增子节 3.6 "前端页面与交互说明"

## 测试运行命令和结果

```bash
npm run typecheck        # ✅ 通过
npm run lint             # ✅ 通过
npm run test:unit        # ✅ 71 tests passed (10 files)
npm run build            # ✅ 构建成功
npm run test:e2e         # ✅ 6 tests passed
```

## 本批未解决问题

无。
