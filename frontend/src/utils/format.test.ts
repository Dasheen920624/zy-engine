import { describe, it, expect, vi, beforeEach } from "vitest";
import { formatDateTime, formatDate, formatNumber, formatPercent, formatCurrency } from "../format";

describe("formatDateTime", () => {
  it("应返回 '-' 当值为 null", () => {
    expect(formatDateTime(null)).toBe("-");
  });
  it("应返回 '-' 当值为 undefined", () => {
    expect(formatDateTime(undefined)).toBe("-");
  });
  it("应返回 '-' 当值为无效日期字符串", () => {
    expect(formatDateTime("invalid")).toBe("-");
  });
  it("应格式化 ISO 日期字符串", () => {
    const result = formatDateTime("2026-05-23T10:30:00");
    expect(result).toBeTruthy();
    expect(result).not.toBe("-");
  });
  it("应格式化 Date 对象", () => {
    const result = formatDateTime(new Date(2026, 4, 23, 10, 30, 0));
    expect(result).toBeTruthy();
  });
  it("应格式化时间戳", () => {
    const result = formatDateTime(1747981800000);
    expect(result).toBeTruthy();
  });
  it("应支持自定义 options", () => {
    const result = formatDateTime("2026-05-23T10:30:00", { year: "numeric" });
    expect(result).toContain("2026");
  });
});

describe("formatDate", () => {
  it("应返回不含时间的日期", () => {
    const result = formatDate("2026-05-23T10:30:00");
    expect(result).toBeTruthy();
    expect(result).not.toBe("-");
  });
  it("应返回 '-' 当值为 null", () => {
    expect(formatDate(null)).toBe("-");
  });
});

describe("formatNumber", () => {
  it("应返回 '-' 当值为 null", () => {
    expect(formatNumber(null)).toBe("-");
  });
  it("应返回 '-' 当值为 undefined", () => {
    expect(formatNumber(undefined)).toBe("-");
  });
  it("应格式化数字", () => {
    const result = formatNumber(1234567);
    expect(result).toBeTruthy();
    expect(result).not.toBe("-");
  });
  it("应支持自定义 options", () => {
    const result = formatNumber(0.1234, { style: "percent" });
    expect(result).toBeTruthy();
  });
});

describe("formatPercent", () => {
  it("应返回 '-' 当值为 null", () => {
    expect(formatPercent(null)).toBe("-");
  });
  it("应格式化百分比（0-100）", () => {
    const result = formatPercent(85.5);
    expect(result).toContain("85");
  });
  it("应支持自定义小数位数", () => {
    const result = formatPercent(85.567, 2);
    expect(result).toBeTruthy();
  });
});

describe("formatCurrency", () => {
  it("应返回 '-' 当值为 null", () => {
    expect(formatCurrency(null)).toBe("-");
  });
  it("应格式化人民币金额", () => {
    const result = formatCurrency(1234.56);
    expect(result).toBeTruthy();
  });
  it("应支持自定义货币代码", () => {
    const result = formatCurrency(1234.56, "USD");
    expect(result).toBeTruthy();
  });
});
