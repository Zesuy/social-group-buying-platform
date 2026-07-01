import { describe, it, expect } from 'vitest'
import { formatAmount, amountToYuan, formatDate, formatDateTime, formatQuantity, formatPhone } from '@/utils/format'

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

describe('formatDate', () => {
  it('should format ISO date string', () => {
    expect(formatDate('2024-01-15')).toBe('2024-01-15')
  })

  it('should handle Date object', () => {
    expect(formatDate(new Date('2024-06-01'))).toBe('2024-06-01')
  })

  it('should return "—" for null/undefined', () => {
    expect(formatDate(null)).toBe('—')
    expect(formatDate(undefined)).toBe('—')
  })

  it('should return "—" for invalid date', () => {
    expect(formatDate('not-a-date')).toBe('—')
  })
})

describe('formatDateTime', () => {
  it('should format date time string', () => {
    expect(formatDateTime('2024-01-15T14:30:00')).toBe('2024-01-15 14:30')
  })

  it('should return "—" for null', () => {
    expect(formatDateTime(null)).toBe('—')
  })
})

describe('formatQuantity', () => {
  it('should format with prefix', () => {
    expect(formatQuantity(3)).toBe('3')
  })

  it('should format with unit', () => {
    expect(formatQuantity(5, '件')).toBe('5 件')
  })

  it('should return "—" for null', () => {
    expect(formatQuantity(null)).toBe('—')
  })
})

describe('formatPhone', () => {
  it('should mask middle digits', () => {
    expect(formatPhone('13812345678')).toBe('138****5678')
  })

  it('should return "—" for null', () => {
    expect(formatPhone(null)).toBe('—')
  })

  it('should return original for non-11-digit', () => {
    expect(formatPhone('12345')).toBe('12345')
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
