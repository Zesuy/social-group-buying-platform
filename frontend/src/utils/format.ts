/**
 * 格式化展示工具
 *
 * 覆盖金额、日期、日期时间和数量格式化。
 * 后端金额以"整数分"传输，前端展示为 ¥xx.xx。
 * 不允许在前端直接使用浮点数金额作为业务值。
 */

/**
 * 将整数分格式化为人民币展示字符串
 *
 * @param amount 金额（整数分），如 2990 表示 ¥29.90
 * @returns 格式化后的字符串，如 "¥29.90"
 *
 * @example
 * formatAmount(2990)    // "¥29.90"
 * formatAmount(0)       // "¥0.00"
 * formatAmount(100)     // "¥1.00"
 * formatAmount(null)    // "¥0.00"
 * formatAmount(undefined) // "¥0.00"
 * formatAmount(-100)    // "¥-1.00"
 */
export function formatAmount(amount: number | null | undefined): string {
  if (amount == null) {
    return '¥0.00'
  }
  const yuan = Math.abs(amount) / 100
  const sign = amount < 0 ? '-' : ''
  return `${sign}¥${yuan.toFixed(2)}`
}

/**
 * 将整数分转换为浮点数（仅用于展示计算，不作为业务值传递）
 *
 * @param amount 金额（整数分）
 * @returns 元为单位的值，如 2990 -> 29.9
 */
export function amountToYuan(amount: number | null | undefined): number {
  if (amount == null) return 0
  return amount / 100
}

/**
 * 格式化日期为短日期字符串
 *
 * @param dateStr ISO 日期字符串或 Date
 * @returns "2024-01-15" 格式
 */
export function formatDate(dateStr: string | Date | null | undefined): string {
  if (!dateStr) return '—'
  const d = typeof dateStr === 'string' ? new Date(dateStr) : dateStr
  if (isNaN(d.getTime())) return '—'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 格式化日期时间为可读字符串
 *
 * @param dateStr ISO 日期字符串或 Date
 * @returns "2024-01-15 14:30" 格式
 */
export function formatDateTime(dateStr: string | Date | null | undefined): string {
  if (!dateStr) return '—'
  const d = typeof dateStr === 'string' ? new Date(dateStr) : dateStr
  if (isNaN(d.getTime())) return '—'
  const date = formatDate(d)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${date} ${h}:${m}`
}

/**
 * 格式化数量展示
 *
 * @param count 数量值
 * @param unit 单位（可选）
 * @returns "x99" 或 "99 件"
 */
export function formatQuantity(count: number | null | undefined, unit?: string): string {
  if (count == null) return '—'
  const prefix = unit ? `x${count}` : String(count)
  return unit ? `${count} ${unit}` : prefix
}

/**
 * 格式化手机号（隐藏中间四位）
 *
 * @param phone 手机号
 * @returns "138****1234"
 */
export function formatPhone(phone: string | null | undefined): string {
  if (!phone) return '—'
  if (phone.length !== 11) return phone
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}
