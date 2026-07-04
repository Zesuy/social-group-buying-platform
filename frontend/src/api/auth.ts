/**
 * 认证相关 API
 *
 * 本批只实现模拟登录和获取当前用户。
 */

import request from './request'
import type {
  ApiResponse,
  MockLoginRequest,
  MockLoginData,
  CurrentUserData,
  SendAuthCodeRequest,
  SendAuthCodeData,
  PhoneCodeLoginRequest,
  PhoneCodeRegisterRequest,
  UpdateCurrentUserRequest,
} from '@/types'

/** 发送登录 / 注册验证码 */
export async function sendAuthCode(params: SendAuthCodeRequest): Promise<SendAuthCodeData> {
  const res = await request.post('/auth/codes', params) as ApiResponse<SendAuthCodeData>
  return res.data
}

/** 手机号验证码登录 */
export async function loginWithCode(params: PhoneCodeLoginRequest): Promise<MockLoginData> {
  const res = await request.post('/auth/login', params) as ApiResponse<MockLoginData>
  return res.data
}

/** 手机号验证码注册 */
export async function registerWithCode(params: PhoneCodeRegisterRequest): Promise<MockLoginData> {
  const res = await request.post('/auth/register', params) as ApiResponse<MockLoginData>
  return res.data
}

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

/** 更新当前用户资料 */
export async function updateMe(params: UpdateCurrentUserRequest): Promise<CurrentUserData> {
  const res = await request.patch('/me', params) as ApiResponse<CurrentUserData>
  return res.data
}
