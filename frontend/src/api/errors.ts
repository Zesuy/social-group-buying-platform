/**
 * API 错误处理与错误码映射工具
 *
 * 将后端、网络、超时等异常统一为 ApiError 结构，
 * 并提供 MVP 常见错误码的中文展示文案。
 */

import type { ApiError, ErrorResponse } from '@/types'
import type { AxiosError } from 'axios'

/**
 * MVP 常见错误码 -> 中文文案映射
 */
const ERROR_MESSAGES: Record<string, string> = {
  UNAUTHORIZED: '未登录或令牌已过期，请重新登录',
  VALIDATION_ERROR: '请检查输入信息是否正确',
  LEADER_REQUIRED: '当前操作需要团长身份',
  STORE_ALREADY_EXISTS: '您已创建过店铺',
  STORE_FORBIDDEN: '不能操作他人店铺',
  GROUP_BUY_NOT_PURCHASABLE: '该团购暂不可购买',
  GROUP_BUY_ENDED: '团购已结束',
  ITEM_NOT_IN_GROUP_BUY: '商品不属于该团购',
  ADDRESS_FORBIDDEN: '地址不属于当前用户',
  INSUFFICIENT_STOCK: '库存不足',
  ORDER_NOT_PAYABLE: '订单不可支付',
  ORDER_ALREADY_PAID: '订单已支付',
  ORDER_NOT_SHIPPABLE: '订单不可发货',
  ORDER_ALREADY_SHIPPED: '订单已发货',
  ORDER_NOT_CANCELABLE: '订单不可取消',
  ORDER_NOT_COMPLETABLE: '订单不可确认收货',
  ORDER_ALREADY_COMPLETED: '订单已完成',
  SUBSCRIPTION_EXISTS: '已订阅该团长',
  RESOURCE_NOT_FOUND: '请求的资源不存在',
  BUSINESS_RULE_VIOLATION: '操作不满足业务规则',
  NETWORK_ERROR: '网络异常，请检查网络后重试',
  TIMEOUT: '请求超时，请稍后重试',
  STORE_NAME_EXISTS: '店铺名称已存在',
}

/**
 * 根据错误码获取中文展示文案
 *
 * @param code 后端返回或前端定义的错误码
 * @returns 中文提示文案，未识别的错误码返回通用文案+错误码
 */
export function getErrorMessage(code: string): string {
  return ERROR_MESSAGES[code] || `系统异常（${code}）`
}

/**
 * 创建标准 ApiError 对象
 */
export function createApiError(code: string, message?: string): ApiError {
  return {
    code,
    message: message || getErrorMessage(code),
  }
}

/**
 * 从 Axios 错误中标准化提取 ApiError
 *
 * 处理三种场景：
 * 1. 后端返回了错误响应（含 error 字段）
 * 2. 网络错误（无响应）
 * 3. 超时错误
 * 4. 其他未知异常
 */
export function normalizeAxiosError(error: AxiosError<ErrorResponse | undefined>): ApiError {
  // 后端返回了结构化错误
  if (error.response?.data?.error) {
    const apiError = error.response.data.error
    return {
      ...apiError,
      message: apiError.message || getErrorMessage(apiError.code),
    }
  }

  // 超时错误（优先于网络错误判断，因为超时也可能无 response）
  if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
    return createApiError('TIMEOUT')
  }

  // 网络错误（无响应）
  if (error.code === 'ERR_NETWORK' || !error.response) {
    return createApiError('NETWORK_ERROR')
  }

  // 其他未知异常
  return createApiError('UNKNOWN_ERROR', error.message || '未知异常')
}
