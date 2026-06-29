/**
 * API 层入口
 *
 * 本批只暴露基础 Axios 实例和请求工具，不实现业务接口。
 * 后续 batch 在此目录按模块拆分，如 auth.ts、stores.ts、orders.ts 等。
 */
export { default as request } from './request'
