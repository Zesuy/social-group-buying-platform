import { describe, it, expect } from 'vitest'
import { getErrorMessage, createApiError, normalizeAxiosError } from '@/api/errors'
import type { ErrorResponse } from '@/types'
import type { AxiosError } from 'axios'

/** 模拟 Axios 错误，仅包含测试所需字段 */
interface MockAxiosError {
  code?: string
  response?: {
    data?: ErrorResponse
  } | null
  isAxiosError?: boolean
  message?: string
}

describe('getErrorMessage', () => {
  it('should return Chinese message for KNOWN error codes', () => {
    expect(getErrorMessage('UNAUTHORIZED')).toBe('未登录或令牌已过期，请重新登录')
    expect(getErrorMessage('VALIDATION_ERROR')).toBe('请检查输入信息是否正确')
    expect(getErrorMessage('LEADER_REQUIRED')).toBe('当前操作需要团长身份')
    expect(getErrorMessage('STORE_ALREADY_EXISTS')).toBe('您已创建过店铺')
    expect(getErrorMessage('INSUFFICIENT_STOCK')).toBe('库存不足')
    expect(getErrorMessage('NETWORK_ERROR')).toBe('网络异常，请检查网络后重试')
    expect(getErrorMessage('TIMEOUT')).toBe('请求超时，请稍后重试')
    expect(getErrorMessage('RESOURCE_NOT_FOUND')).toBe('请求的资源不存在')
  })

  it('should return fallback for UNKNOWN error codes', () => {
    const msg = getErrorMessage('SOME_UNKNOWN_CODE')
    expect(msg).toBe('系统异常（SOME_UNKNOWN_CODE）')
  })
})

describe('createApiError', () => {
  it('should create ApiError with Chinese message for KNOWN code', () => {
    const err = createApiError('UNAUTHORIZED')
    expect(err).toEqual({
      code: 'UNAUTHORIZED',
      message: '未登录或令牌已过期，请重新登录',
    })
  })

  it('should use custom message when provided', () => {
    const err = createApiError('UNAUTHORIZED', '自定义错误消息')
    expect(err.message).toBe('自定义错误消息')
  })

  it('should create fallback for UNKNOWN code', () => {
    const err = createApiError('UNKNOWN')
    expect(err.message).toBe('系统异常（UNKNOWN）')
  })
})

describe('normalizeAxiosError', () => {
  it('should extract ApiError from backend response', () => {
    // Simulate an Axios error with a backend error response
    const axiosError = {
      response: {
        data: {
          success: false,
          error: {
            code: 'VALIDATION_ERROR',
            message: '手机号格式不正确',
          },
          traceId: 'req_001',
        } satisfies ErrorResponse,
      },
      isAxiosError: true,
    }

    const result = normalizeAxiosError(axiosError as MockAxiosError as AxiosError)
    expect(result.code).toBe('VALIDATION_ERROR')
    expect(result.message).toBe('手机号格式不正确')
  })

  it('should generate NETWORK_ERROR when no response', () => {
    const axiosError = {
      code: 'ERR_NETWORK',
      response: undefined,
      isAxiosError: true,
      message: 'Network Error',
    }

    const result = normalizeAxiosError(axiosError as MockAxiosError as AxiosError)
    expect(result.code).toBe('NETWORK_ERROR')
    expect(result.message).toBe('网络异常，请检查网络后重试')
  })

  it('should generate TIMEOUT for timeout errors', () => {
    const axiosError = {
      code: 'ECONNABORTED',
      message: 'timeout of 15000ms exceeded',
      response: undefined,
      isAxiosError: true,
    }

    const result = normalizeAxiosError(axiosError as MockAxiosError as AxiosError)
    expect(result.code).toBe('TIMEOUT')
  })

  it('should generate NETWORK_ERROR for other unknown errors without response', () => {
    const axiosError = {
      isAxiosError: true,
      response: undefined,
      message: 'Something went wrong',
    }

    const result = normalizeAxiosError(axiosError as MockAxiosError as AxiosError)
    // 无 response 时归为网络错误
    expect(result.code).toBe('NETWORK_ERROR')
    expect(result.message).toBe('网络异常，请检查网络后重试')
  })

  it('should fill Chinese message when backend message is empty', () => {
    const axiosError = {
      response: {
        data: {
          success: false,
          error: {
            code: 'UNAUTHORIZED',
            message: '',
          },
          traceId: 'req_001',
        } satisfies ErrorResponse,
      },
      isAxiosError: true,
    }

    const result = normalizeAxiosError(axiosError as MockAxiosError as AxiosError)
    expect(result.code).toBe('UNAUTHORIZED')
    expect(result.message).toBe('未登录或令牌已过期，请重新登录')
  })
})
