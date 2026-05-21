/**
 * 规则模块的纯函数格式化器（PR-FINAL-11）。无外部依赖，便于单测。
 */

import type { RuleDefinition, RuleType } from "../../../api/rule";
import type { Severity } from "../../../api/types";

export const RULE_TYPE_LABELS: Record<RuleType, string> = {
  TIME_LIMIT_QC: "时限质控",
  CONTENT_QC: "内容质控",
  PATHWAY_NODE: "路径节点",
  SAFETY: "安全规则",
  FOLLOWUP: "随访提醒",
  OPERATION: "运营规则",
};

export const SEVERITY_LABELS: Record<Severity, string> = {
  HIGH: "高",
  MEDIUM: "中",
  LOW: "低",
  INFO: "提示",
};

export const SEVERITY_COLOR: Record<Severity, "error" | "warning" | "default" | "processing"> = {
  HIGH: "error",
  MEDIUM: "warning",
  LOW: "default",
  INFO: "processing",
};

export const STATUS_LABELS: Record<string, string> = {
  DRAFT: "草稿",
  REVIEWED: "已审核",
  PUBLISHED: "已发布",
  RETIRED: "已下线",
};

export function formatRuleScope(rule: RuleDefinition): string {
  const segs: string[] = [];
  if (rule.scope_level) segs.push(rule.scope_level);
  if (rule.tenant_id) segs.push(`租户 ${rule.tenant_id}`);
  if (rule.hospital_code) segs.push(`院 ${rule.hospital_code}`);
  if (rule.campus_code) segs.push(`院区 ${rule.campus_code}`);
  if (rule.department_code) segs.push(`科 ${rule.department_code}`);
  return segs.length > 0 ? segs.join(" / ") : "全局";
}

export function formatPublishedTime(time?: string | null): string {
  if (!time) return "—";
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return time;
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatElapsedMs(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return "—";
  if (ms < 1) return "<1 ms";
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

export function describeRuleType(ruleType: RuleType | string | undefined): string {
  if (!ruleType) return "未分类";
  return RULE_TYPE_LABELS[ruleType as RuleType] ?? ruleType;
}

export function describeSeverity(severity: Severity | string | undefined): string {
  if (!severity) return "—";
  return SEVERITY_LABELS[severity as Severity] ?? severity;
}

export function describeStatus(status: string | undefined): string {
  if (!status) return "—";
  return STATUS_LABELS[status] ?? status;
}

/** 把 DSL 对象转为可读的 JSON 字符串（2 空格缩进） */
export function stringifyDsl(dsl: Record<string, unknown> | unknown): string {
  if (dsl === null || dsl === undefined) return "{}";
  return JSON.stringify(dsl, null, 2);
}
