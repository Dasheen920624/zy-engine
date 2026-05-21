import { describe, expect, it } from "vitest";
import { safeParseJson, validateRuleDsl } from "../ruleSchema";

describe("ruleSchema.safeParseJson", () => {
  it("parses valid JSON", () => {
    const result = safeParseJson('{"a":1}');
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.value).toEqual({ a: 1 });
  });

  it("returns error for invalid JSON", () => {
    const result = safeParseJson("{not-json");
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error.length).toBeGreaterThan(0);
  });
});

describe("ruleSchema.validateRuleDsl", () => {
  it("passes a minimal valid DSL", () => {
    const result = validateRuleDsl({
      rule_code: "DEMO",
      rule_name: "示例",
      rule_type: "TIME_LIMIT_QC",
      version: "1.0.0",
      trigger: { events: ["admission"] },
      condition: { field: "x", exists: "x" },
      result: { hit: {} },
    });
    expect(result.valid).toBe(true);
    expect(result.errors).toEqual([]);
  });

  it("fails when DSL is not an object", () => {
    const result = validateRuleDsl("string");
    expect(result.valid).toBe(false);
    expect(result.errors[0].path).toBe("$");
  });

  it("reports missing required fields", () => {
    const result = validateRuleDsl({ rule_code: "X" });
    expect(result.valid).toBe(false);
    const missing = result.errors.map((e) => e.path);
    expect(missing).toContain("$.rule_name");
    expect(missing).toContain("$.rule_type");
    expect(missing).toContain("$.trigger");
    expect(missing).toContain("$.condition");
    expect(missing).toContain("$.result");
  });

  it("rejects unknown rule_type", () => {
    const result = validateRuleDsl({
      rule_code: "X",
      rule_name: "X",
      rule_type: "UNKNOWN_KIND",
      version: "1",
      trigger: {},
      condition: { field: "x", exists: "x" },
      result: {},
    });
    expect(result.valid).toBe(false);
    expect(result.errors.some((e) => e.path === "$.rule_type")).toBe(true);
  });

  it("rejects empty condition", () => {
    const result = validateRuleDsl({
      rule_code: "X",
      rule_name: "X",
      rule_type: "SAFETY",
      version: "1",
      trigger: {},
      condition: {},
      result: {},
    });
    expect(result.valid).toBe(false);
    expect(result.errors.some((e) => /谓词/.test(e.message))).toBe(true);
  });

  it("flags unknown predicate keys", () => {
    const result = validateRuleDsl({
      rule_code: "X",
      rule_name: "X",
      rule_type: "SAFETY",
      version: "1",
      trigger: {},
      condition: { weird_key: 1 },
      result: {},
    });
    expect(result.valid).toBe(false);
    expect(result.errors.some((e) => /未知谓词/.test(e.message))).toBe(true);
  });

  it("recursively validates nested all/any/not", () => {
    const result = validateRuleDsl({
      rule_code: "X",
      rule_name: "X",
      rule_type: "SAFETY",
      version: "1",
      trigger: {},
      condition: { all: [{ any: [{ bogus: 1 }] }] },
      result: {},
    });
    expect(result.valid).toBe(false);
    expect(result.errors[0].path).toContain("$.condition.all[0].any[0]");
  });
});
