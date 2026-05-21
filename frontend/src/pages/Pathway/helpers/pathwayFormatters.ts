/**
 * 路径模块共享纯函数格式化器（PATHWAY-ENGINE-COMPLETE）。
 */

import type {
  InstanceStatus,
  NodeStatus,
  TaskStatus,
  VariationType,
} from "../../../api/pathway";

export const INSTANCE_STATUS_LABELS: Record<InstanceStatus, string> = {
  ACTIVE: "进行中",
  COMPLETED: "已完成",
  EXITED: "已退出",
  TERMINATED: "已终止",
};

export const NODE_STATUS_LABELS: Record<NodeStatus, string> = {
  PENDING: "待进入",
  ACTIVE: "进行中",
  COMPLETED: "已完成",
  SKIPPED: "已跳过",
  BLOCKED: "阻塞",
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "待办",
  COMPLETED: "已完成",
  SKIPPED: "已跳过",
  FAILED: "失败",
};

export const VARIATION_TYPE_LABELS: Record<VariationType, string> = {
  SKIP: "跳过节点",
  DEFER: "延迟",
  EXTEND_TIME: "延长时限",
  SUBSTITUTE: "替代方案",
  EXIT: "退出路径",
  ROLLBACK: "回退",
  MANUAL_OVERRIDE: "人工干预",
};

export const INSTANCE_STATUS_COLOR: Record<InstanceStatus, "success" | "processing" | "default" | "error"> = {
  ACTIVE: "processing",
  COMPLETED: "success",
  EXITED: "default",
  TERMINATED: "error",
};

export const VARIATION_TYPE_COLOR: Record<VariationType, "default" | "warning" | "error" | "processing"> = {
  SKIP: "warning",
  DEFER: "warning",
  EXTEND_TIME: "warning",
  SUBSTITUTE: "processing",
  EXIT: "error",
  ROLLBACK: "error",
  MANUAL_OVERRIDE: "default",
};

export function formatTimestamp(time?: string | null): string {
  if (!time) return "—";
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return time;
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDurationHours(hours: number | null | undefined): string {
  if (hours === null || hours === undefined) return "—";
  if (hours < 1) return `${Math.round(hours * 60)} 分`;
  if (hours < 24) return `${hours.toFixed(1)} 时`;
  return `${(hours / 24).toFixed(1)} 天`;
}

export function describeInstanceStatus(status?: InstanceStatus | string): string {
  if (!status) return "—";
  return INSTANCE_STATUS_LABELS[status as InstanceStatus] ?? status;
}

export function describeNodeStatus(status?: NodeStatus | string): string {
  if (!status) return "—";
  return NODE_STATUS_LABELS[status as NodeStatus] ?? status;
}

export function describeTaskStatus(status?: TaskStatus | string): string {
  if (!status) return "—";
  return TASK_STATUS_LABELS[status as TaskStatus] ?? status;
}

export function describeVariationType(type?: VariationType | string): string {
  if (!type) return "—";
  return VARIATION_TYPE_LABELS[type as VariationType] ?? type;
}

/**
 * 患者 ID 脱敏：保留前 4 + 后 4，中间打码。
 * 国情合规（PR-FINAL-07 卡片提示）：身份证 4+4 / 手机号 3+4。
 */
export function maskPatientId(patientId: string | null | undefined): string {
  if (!patientId) return "—";
  if (patientId.length <= 8) return patientId;
  return `${patientId.slice(0, 4)}****${patientId.slice(-4)}`;
}

/** 数字百分比展示（输入 0~1 范围） */
export function formatPercent(ratio: number | null | undefined, fractionDigits = 1): string {
  if (ratio === null || ratio === undefined || Number.isNaN(ratio)) return "—";
  return `${(ratio * 100).toFixed(fractionDigits)}%`;
}

/** 把 0~1 confidence 描述为「高 / 中 / 低」 */
export function describeConfidence(score: number | null | undefined): string {
  if (score === null || score === undefined || Number.isNaN(score)) return "—";
  if (score >= 0.8) return "高";
  if (score >= 0.5) return "中";
  return "低";
}

export function stringifyJson(value: unknown): string {
  if (value === null || value === undefined) return "{}";
  return JSON.stringify(value, null, 2);
}
