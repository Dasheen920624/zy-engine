import { describe, expect, it } from "vitest";
import {
  describeRuleType,
  describeSeverity,
  describeStatus,
  formatElapsedMs,
  formatPublishedTime,
  formatRuleScope,
  stringifyDsl,
} from "../ruleFormatters";
import type { RuleDefinition } from "../../../../api/rule";

const baseRule: RuleDefinition = {
  rule_code: "R1",
  rule_name: "示例规则",
  rule_type: "TIME_LIMIT_QC",
  version_no: "1.0",
  status: "PUBLISHED",
  severity: "HIGH",
  enabled: true,
  rule_json: {},
};

describe("ruleFormatters.describeRuleType", () => {
  it("returns Chinese label for known types", () => {
    expect(describeRuleType("TIME_LIMIT_QC")).toBe("时限质控");
    expect(describeRuleType("SAFETY")).toBe("安全规则");
  });
  it("falls back to raw string for unknown types", () => {
    expect(describeRuleType("WEIRD")).toBe("WEIRD");
  });
  it("returns 未分类 when undefined", () => {
    expect(describeRuleType(undefined)).toBe("未分类");
  });
});

describe("ruleFormatters.describeSeverity & describeStatus", () => {
  it("describes severity", () => {
    expect(describeSeverity("HIGH")).toBe("高");
    expect(describeSeverity(undefined)).toBe("—");
  });
  it("describes status", () => {
    expect(describeStatus("DRAFT")).toBe("草稿");
    expect(describeStatus("PUBLISHED")).toBe("已发布");
    expect(describeStatus("UNKNOWN")).toBe("UNKNOWN");
  });
});

describe("ruleFormatters.formatRuleScope", () => {
  it("returns 全局 when no scope hints", () => {
    expect(formatRuleScope(baseRule)).toBe("全局");
  });
  it("composes scope segments", () => {
    const r: RuleDefinition = {
      ...baseRule,
      scope_level: "HOSPITAL",
      tenant_id: "T1",
      hospital_code: "H1",
      department_code: "D1",
    };
    expect(formatRuleScope(r)).toBe("HOSPITAL / 租户 T1 / 院 H1 / 科 D1");
  });
});

describe("ruleFormatters.formatPublishedTime", () => {
  it("formats valid ISO time", () => {
    expect(formatPublishedTime("2026-05-21T10:30:00+08:00")).toMatch(/2026-05-21/);
  });
  it("returns dash for empty", () => {
    expect(formatPublishedTime(undefined)).toBe("—");
    expect(formatPublishedTime(null)).toBe("—");
  });
  it("returns raw when not parseable", () => {
    expect(formatPublishedTime("not-a-date")).toBe("not-a-date");
  });
});

describe("ruleFormatters.formatElapsedMs", () => {
  it("formats <1ms / ms / s ranges", () => {
    expect(formatElapsedMs(0.4)).toBe("<1 ms");
    expect(formatElapsedMs(120)).toBe("120 ms");
    expect(formatElapsedMs(1500)).toBe("1.50 s");
  });
  it("returns dash for null/undefined", () => {
    expect(formatElapsedMs(undefined)).toBe("—");
    expect(formatElapsedMs(null)).toBe("—");
  });
});

describe("ruleFormatters.stringifyDsl", () => {
  it("formats with 2-space indent", () => {
    expect(stringifyDsl({ a: 1 })).toBe(`{\n  "a": 1\n}`);
  });
  it("handles null/undefined", () => {
    expect(stringifyDsl(null)).toBe("{}");
    expect(stringifyDsl(undefined)).toBe("{}");
  });
});
