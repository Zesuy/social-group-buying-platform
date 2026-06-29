import { describe, it, expect } from 'vitest'
import { formatAmount, amountToYuan } from '@/utils/format'

describe('formatAmount', () => {
  it('should format 0 as ¥0.00', () => {
    expect(formatAmount(0)).toBe('¥0.00')
  })

  it('should format positive integer correctly', () => {
    expect(formatAmount(2990)).toBe('¥29.90')
  })

  it('should pad cents with zero', () => {
    expect(formatAmount(100)).toBe('¥1.00')
    expect(formatAmount(1)).toBe('¥0.01')
  })

  it('should handle large amounts', () => {
    expect(formatAmount(100000)).toBe('¥1000.00')
    expect(formatAmount(999999)).toBe('¥9999.99')
  })

  it('should return ¥0.00 for null', () => {
    expect(formatAmount(null)).toBe('¥0.00')
  })

  it('should return ¥0.00 for undefined', () => {
    expect(formatAmount(undefined)).toBe('¥0.00')
  })

  it('should handle negative amounts', () => {
    expect(formatAmount(-2990)).toBe('-¥29.90')
    expect(formatAmount(-100)).toBe('-¥1.00')
  })
})

describe('amountToYuan', () => {
  it('should convert fen to yuan', () => {
    expect(amountToYuan(2990)).toBe(29.9)
    expect(amountToYuan(100)).toBe(1)
  })

  it('should return 0 for null/undefined', () => {
    expect(amountToYuan(null)).toBe(0)
    expect(amountToYuan(undefined)).toBe(0)
  })
})
