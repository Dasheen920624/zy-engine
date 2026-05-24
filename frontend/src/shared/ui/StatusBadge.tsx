import { Tag } from "antd";
import type { CSSProperties } from "react";

/**
 * 4 套统一状态机（与 docs/CONSTITUTION.md §3 对齐）。
 *
 * 资产类、变更类、待办类、告警类全平台统一这 4 套，禁止自创。
 */
export type ConfigStatus = "draft" | "pending_review" | "published" | "active" | "deprecated" | "archived";
export type ChangeStatus = "pending" | "canary" | "rolled_out" | "rolled_back";
export type TodoStatus = "unread" | "in_progress" | "done" | "escalated";
export type AlertStatus = "new" | "assigned" | "remediating" | "closed" | "waived";

export type AnyStatus = ConfigStatus | ChangeStatus | TodoStatus | AlertStatus;

interface StatusMeta {
  label: string;
  color: string; // antd Tag color name
}

const CONFIG_MAP: Record<ConfigStatus, StatusMeta> = {
  draft: { label: "草稿", color: "default" },
  pending_review: { label: "待审核", color: "warning" },
  published: { label: "已发布", color: "processing" },
  active: { label: "生效中", color: "success" },
  deprecated: { label: "已下线", color: "default" },
  archived: { label: "已归档", color: "default" },
};

const CHANGE_MAP: Record<ChangeStatus, StatusMeta> = {
  pending: { label: "待发布", color: "default" },
  canary: { label: "灰度中", color: "warning" },
  rolled_out: { label: "全量", color: "success" },
  rolled_back: { label: "已回滚", color: "error" },
};

const TODO_MAP: Record<TodoStatus, StatusMeta> = {
  unread: { label: "未读", color: "blue" },
  in_progress: { label: "处理中", color: "processing" },
  done: { label: "已完成", color: "success" },
  escalated: { label: "已升级", color: "error" },
};

const ALERT_MAP: Record<AlertStatus, StatusMeta> = {
  new: { label: "新建", color: "error" },
  assigned: { label: "已派单", color: "warning" },
  remediating: { label: "整改中", color: "processing" },
  closed: { label: "已闭环", color: "success" },
  waived: { label: "已豁免", color: "default" },
};

const ALL_MAPS = { config: CONFIG_MAP, change: CHANGE_MAP, todo: TODO_MAP, alert: ALERT_MAP } as const;

export type StatusMachine = keyof typeof ALL_MAPS;

interface StatusBadgeProps {
  machine: StatusMachine;
  status: AnyStatus;
  style?: CSSProperties;
}

/**
 * 通用状态徽标。任何 PR 提交新页面，必须用 StatusBadge 显示状态。
 *
 * @example
 *   <StatusBadge machine="config" status="published" />
 *   <StatusBadge machine="change" status="canary" />
 *   <StatusBadge machine="todo" status="in_progress" />
 *   <StatusBadge machine="alert" status="new" />
 */
export function StatusBadge({ machine, status, style }: StatusBadgeProps) {
  const map = ALL_MAPS[machine] as Record<string, StatusMeta>;
  const meta = map[status];
  if (!meta) {
    return <Tag color="default">未知状态: {String(status)}</Tag>;
  }
  return (
    <Tag color={meta.color} style={style}>
      {meta.label}
    </Tag>
  );
}

/**
 * 导出状态机字典给业务层使用（如下拉筛选）。
 */
export const STATUS_MACHINES = {
  config: Object.keys(CONFIG_MAP) as ConfigStatus[],
  change: Object.keys(CHANGE_MAP) as ChangeStatus[],
  todo: Object.keys(TODO_MAP) as TodoStatus[],
  alert: Object.keys(ALERT_MAP) as AlertStatus[],
} as const;
