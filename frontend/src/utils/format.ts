/**
 * 时间和数字格式化工具函数。
 * 使用 Intl API 实现本地化格式化，默认 zh-CN。
 */

/** 获取当前 locale */
function getLocale(): string {
  try {
    return localStorage.getItem("medkernel_locale") || "zh-CN";
  } catch {
    return "zh-CN";
  }
}

/**
 * 格式化日期时间。
 * @param value 日期字符串、Date 对象或时间戳
 * @param options Intl.DateTimeFormat 选项
 * @returns 格式化后的日期时间字符串
 */
export function formatDateTime(
  value: string | Date | number | null | undefined,
  options?: Intl.DateTimeFormatOptions,
): string {
  if (value === null || value === undefined) return "-";
  const date = value instanceof Date ? value : new Date(value);
  if (isNaN(date.getTime())) return "-";
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  };
  return new Intl.DateTimeFormat(getLocale(), { ...defaultOptions, ...options }).format(date);
}

/**
 * 格式化日期（不含时间）。
 */
export function formatDate(
  value: string | Date | number | null | undefined,
): string {
  return formatDateTime(value, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: undefined,
    minute: undefined,
    second: undefined,
  });
}

/**
 * 格式化数字。
 * @param value 数值
 * @param options Intl.NumberFormat 选项
 */
export function formatNumber(
  value: number | null | undefined,
  options?: Intl.NumberFormatOptions,
): string {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat(getLocale(), options).format(value);
}

/**
 * 格式化百分比。
 * @param value 0-100 的数值
 * @param maximumFractionDigits 最大小数位数
 */
export function formatPercent(
  value: number | null | undefined,
  maximumFractionDigits: number = 1,
): string {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat(getLocale(), {
    style: "percent",
    maximumFractionDigits,
  }).format(value / 100);
}

/**
 * 格式化金额。
 * @param value 金额数值
 * @param currency 货币代码，默认 CNY
 */
export function formatCurrency(
  value: number | null | undefined,
  currency: string = "CNY",
): string {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat(getLocale(), {
    style: "currency",
    currency,
  }).format(value);
}
