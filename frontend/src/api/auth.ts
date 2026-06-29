/**
 * 认证相关 API
 *
 * 本批只实现模拟登录和获取当前用户。
 */

import request from './request'
import type { ApiResponse, MockLoginRequest, MockLoginData, CurrentUserData } from '@/types'

/**
 * 模拟登录
 *
 * MVP 阶段使用模拟登录，后续替换为微信登录。
 *
 * @param params 登录参数（昵称、头像、手机号）
 * @returns accessToken 和用户摘要
 */
export async function mockLogin(params: MockLoginRequest): Promise<MockLoginData> {
  const res = await request.post('/auth/mock-login', params) as ApiResponse<MockLoginData>
  return res.data
}

/**
 * 获取当前用户信息
 *
 * 需要 Authorization header。
 * 返回当前用户、团长身份和店铺摘要。
 */
export async function fetchMe(): Promise<CurrentUserData> {
  const res = await request.get('/me') as ApiResponse<CurrentUserData>
  return res.data
}
