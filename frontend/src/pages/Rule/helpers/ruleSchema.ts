/**
 * 规则 DSL 轻量校验器（PR-FINAL-11）。
 *
 * 不引入完整 JSON Schema 运行时（ajv）以避免 bundle 增大。
 * 与 ai-dev-input/03_data_models/rule_dsl.schema.json 保持手工对齐 —— 当后端 schema
 * 演进时，本文件应同 PR 更新。
 *
 * 校验粒度：
 *   - 必填字段：rule_code / rule_name / rule_type / version / trigger / condition / result
 *   - rule_type 枚举值
 *   - condition 至少有一个谓词（all / any / not / field+op / exists / ...）
 */

import type { RuleType } from "../../../api/rule";

export interface DslValidationError {
  path: string;
  message: string;
}

export interface DslValidationResult {
  valid: boolean;
  errors: DslValidationError[];
}

const RULE_TYPES: RuleType[] = [
  "TIME_LIMIT_QC",
  "CONTENT_QC",
  "PATHWAY_NODE",
  "SAFETY",
  "FOLLOWUP",
  "OPERATION",
];

const CONDITION_KEYS = new Set([
  "all",
  "any",
  "not",
  "exists",
  "not_exists",
  "field",
  "eq",
  "ne",
  "gt",
  "gte",
  "lt",
  "lte",
  "in",
  "contains_any",
  "duration_minutes_between",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function validateCondition(condition: unknown, path: string, errors: DslValidationError[]): void {
  if (!isRecord(condition)) {
    errors.push({ path, message: "condition 必须是对象" });
    return;
  }
  const keys = Object.keys(condition);
  if (keys.length === 0) {
    errors.push({ path, message: "condition 至少需要一个谓词" });
    return;
  }
  const unknownKeys = keys.filter((k) => !CONDITION_KEYS.has(k));
  if (unknownKeys.length > 0) {
    errors.push({
      path,
      message: `未知谓词：${unknownKeys.join(", ")}（合法谓词：${[...CONDITION_KEYS].join(" / ")}）`,
    });
  }
  if (Array.isArray(condition.all)) {
    condition.all.forEach((c, i) => validateCondition(c, `${path}.all[${i}]`, errors));
  }
  if (Array.isArray(condition.any)) {
    condition.any.forEach((c, i) => validateCondition(c, `${path}.any[${i}]`, errors));
  }
  if (condition.not !== undefined) {
    validateCondition(condition.not, `${path}.not`, errors);
  }
}

export function validateRuleDsl(dsl: unknown): DslValidationResult {
  const errors: DslValidationError[] = [];

  if (!isRecord(dsl)) {
    return { valid: false, errors: [{ path: "$", message: "DSL 必须是对象" }] };
  }

  const required = ["rule_code", "rule_name", "rule_type", "version", "trigger", "condition", "result"];
  required.forEach((key) => {
    if (dsl[key] === undefined || dsl[key] === null || dsl[key] === "") {
      errors.push({ path: `$.${key}`, message: `必填字段缺失：${key}` });
    }
  });

  if (typeof dsl.rule_type === "string" && !RULE_TYPES.includes(dsl.rule_type as RuleType)) {
    errors.push({
      path: "$.rule_type",
      message: `rule_type 取值非法（合法：${RULE_TYPES.join(" / ")}）`,
    });
  }

  if (dsl.trigger !== undefined && !isRecord(dsl.trigger)) {
    errors.push({ path: "$.trigger", message: "trigger 必须是对象" });
  }

  if (dsl.condition !== undefined) {
    validateCondition(dsl.condition, "$.condition", errors);
  }

  if (dsl.result !== undefined && !isRecord(dsl.result)) {
    errors.push({ path: "$.result", message: "result 必须是对象" });
  }

  return { valid: errors.length === 0, errors };
}

/** 尝试解析 JSON 字符串；返回 { ok, value, error }，避免 throw 干扰编辑器 UX */
export function safeParseJson(text: string): { ok: true; value: unknown } | { ok: false; error: string } {
  try {
    return { ok: true, value: JSON.parse(text) };
  } catch (e) {
    return { ok: false, error: (e as Error).message };
  }
}
