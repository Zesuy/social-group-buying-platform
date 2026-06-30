import { defineConfig } from '@playwright/test'

/**
 * Playwright Live E2E 配置
 *
 * 运行真实前后端联调测试。
 * 测试前需要手动启动后端（localhost:8080）和前端（localhost:5173）。
 *
 * 使用方式：
 *   npm run test:e2e:live
 *   或
 *   npx playwright test --config playwright.live.config.ts
 */
export default defineConfig({
  testDir: './tests/e2e',
  testMatch: '*.live.spec.ts',
  fullyParallel: false, // Live E2E 共享测试数据，不能并行
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1, // 串行执行，避免数据冲突
  reporter: 'html',
  timeout: 60000, // 真实 API 响应时间较长
  expect: {
    timeout: 10000,
  },
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  // Live E2E 要求后端已启动；前端 dev server 由 Playwright 自动启动
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 30000,
  },
})
