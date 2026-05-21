/**
 * 节点进度时间轴：渲染患者路径实例的节点序列与当前节点。
 */

import { Tag } from "antd";
import type { NodeStatus, PatientNodeState } from "../../../../api/pathway";
import { describeNodeStatus, formatTimestamp } from "../../helpers/pathwayFormatters";
import styles from "../../styles.module.css";

export interface NodeProgressTimelineProps {
  nodes: PatientNodeState[];
  currentNodeCode?: string;
}

const STATUS_COLOR: Record<NodeStatus, "success" | "processing" | "warning" | "error" | "default"> = {
  PENDING: "default",
  ACTIVE: "processing",
  COMPLETED: "success",
  SKIPPED: "warning",
  BLOCKED: "error",
};

function pickClass(node: PatientNodeState, currentNodeCode?: string): string {
  if (node.node_code === currentNodeCode) {
    return `${styles.nodeItem} ${styles.nodeItemActive}`;
  }
  if (node.status === "COMPLETED") return `${styles.nodeItem} ${styles.nodeItemCompleted}`;
  if (node.status === "BLOCKED") return `${styles.nodeItem} ${styles.nodeItemBlocked}`;
  return styles.nodeItem;
}

export default function NodeProgressTimeline({ nodes, currentNodeCode }: NodeProgressTimelineProps) {
  if (!nodes.length) {
    return <div className={styles.tableEmptyHint}>暂无节点状态</div>;
  }
  return (
    <div className={styles.nodeTimeline} role="list">
      {nodes.map((node) => (
        <div key={node.node_code} className={pickClass(node, currentNodeCode)} role="listitem">
          <span className={styles.nodeBadge}>
            <Tag color={STATUS_COLOR[node.status] ?? "default"}>
              {describeNodeStatus(node.status)}
            </Tag>
          </span>
          <div>
            <span className={styles.nodeName}>{node.node_name ?? node.node_code}</span>
            <span className={styles.nodeMeta}>
              {node.node_code}
              {node.enter_time ? ` · 进入 ${formatTimestamp(node.enter_time)}` : ""}
              {node.complete_time ? ` · 完成 ${formatTimestamp(node.complete_time)}` : ""}
              {node.timeout_flag ? " · ⚠ 超时" : ""}
            </span>
          </div>
          <div>
            {node.tasks?.length ? (
              <span className={styles.nodeMeta}>{node.tasks.length} 个任务</span>
            ) : null}
          </div>
        </div>
      ))}
    </div>
  );
}
