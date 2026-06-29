/**
 * 金额展示工具
 *
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
