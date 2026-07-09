# Frontend

`frontend/` 是邻鲜团的 H5 前端和 Capacitor Android 壳。主要提供买家端、团长端和商家管理端页面。

## 技术栈

- Vue 3 + Vite + TypeScript
- Vue Router + Pinia
- Vant + 自定义业务组件
- Axios API 封装
- Vitest + Vue Test Utils
- Playwright E2E
- Capacitor Android

## 目录说明

```text
src/api/         后端 API 封装
src/components/  通用 UI 和业务组件
src/composables/ 组合式逻辑
src/router/      路由和权限入口
src/stores/      Pinia 状态
src/styles/      全局样式、主题变量、Vant 覆盖
src/types/       前端类型定义
src/utils/       格式化、图片、状态等工具
src/views/       页面，含买家端、leader 和 merchant 子目录
tests/unit/      Vitest 单测
tests/e2e/       Playwright E2E
android/         Capacitor Android 工程
```

## 常用命令

本项目在 WSL/Linux 环境开发，建议通过 zsh 加载本机 Node 环境：

```bash
zsh -ic 'cd frontend && npm install'
zsh -ic 'cd frontend && npm run dev'
zsh -ic 'cd frontend && npm run typecheck'
zsh -ic 'cd frontend && npm run lint'
zsh -ic 'cd frontend && npm run test:unit'
zsh -ic 'cd frontend && npm run build'
zsh -ic 'cd frontend && npm run test:e2e'
```

## 环境变量

开发环境默认通过 Vite proxy 访问后端。需要指定 API 地址时设置：

```env
VITE_API_BASE_URL=http://localhost:8080
```

Android 构建可参考 `.env.android` 和 `capacitor.config.ts`，当前 APK 壳指向 `https://shop.zesuy.top`。

## APK

```bash
zsh -ic 'cd frontend && npm run apk:debug'
```

调试包默认输出到：

```text
frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

## 开发注意

- 页面改动优先复用 `src/api/`、`src/types/`、`src/components/` 和现有组合式函数。
- H5 体验按移动端优先处理，固定底栏和弹层需要兼顾 safe-area 与桌面 H5 容器宽度。
- 页面 / 链路级改动至少运行 typecheck、lint、相关 unit/E2E；完整前端验收运行全量 E2E。
