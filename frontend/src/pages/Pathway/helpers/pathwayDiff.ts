/**
 * 客户端 diff 辅助（PATHWAY-ENGINE-COMPLETE）。
 *
 * 后端 /api/pathways/{code}/diff 已实现完整 diff，但其 JSON 结构未约束
 * 详细字段。本文件提供两个工具：
 *
 * 1. extractDiffSummary：从后端 diff 结果计算「N 新增 / M 移除 / K 修改」
 * 2. categorizeDiffItem：把 diff 行归类为 added / removed / modified（供 UI 着色）
 */

import type { PathwayDiffResult } from "../../../api/pathway";

export interface DiffSection {
  added: Array<Record<string, unknown>>;
  removed: Array<Record<string, unknown>>;
  modified: Array<Record<string, unknown>>;
}

export function pickDiffSection(
  diff: PathwayDiffResult | undefined,
  kind: "nodes" | "edges" | "tasks",
): DiffSection {
  if (!diff) return { added: [], removed: [], modified: [] };
  return {
    added: diff[`${kind}_added`] ?? [],
    removed: diff[`${kind}_removed`] ?? [],
    modified: diff[`${kind}_modified`] ?? [],
  };
}

export function diffTotals(diff: PathwayDiffResult | undefined): {
  nodes: number;
  edges: number;
  tasks: number;
} {
  if (!diff) return { nodes: 0, edges: 0, tasks: 0 };
  const s = diff.summary;
  if (s) {
    return {
      nodes: s.nodes_added + s.nodes_removed + s.nodes_modified,
      edges: s.edges_added + s.edges_removed + s.edges_modified,
      tasks: s.tasks_added + s.tasks_removed + s.tasks_modified,
    };
  }
  return {
    nodes:
      (diff.nodes_added?.length ?? 0) +
      (diff.nodes_removed?.length ?? 0) +
      (diff.nodes_modified?.length ?? 0),
    edges:
      (diff.edges_added?.length ?? 0) +
      (diff.edges_removed?.length ?? 0) +
      (diff.edges_modified?.length ?? 0),
    tasks:
      (diff.tasks_added?.length ?? 0) +
      (diff.tasks_removed?.length ?? 0) +
      (diff.tasks_modified?.length ?? 0),
  };
}

export function describeDiffItem(item: Record<string, unknown>): string {
  const code =
    item.node_code ||
    item.edge_code ||
    item.task_code ||
    item.code ||
    item.id ||
    "(unknown)";
  const name = item.node_name || item.edge_name || item.task_name || item.name;
  return name ? `${String(code)} · ${String(name)}` : String(code);
}
