/**
 * 调用统计卡（PR-FINAL-13）：渲染 /api/dify/workflows/stats 聚合 + 最近调用表。
 */

import { Statistic, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { WorkflowInvocationStats } from "../../../api/aiWorkflows";
import { PROVIDER_LABELS } from "../../../api/aiWorkflows";
import type { ProviderType } from "../../../api/aiWorkflows";
import styles from "../styles.module.css";

const { Text } = Typography;

export interface InvocationStatsCardProps {
  stats?: WorkflowInvocationStats;
}

interface RecentRow {
  invocation_id: string;
  workflow_code: string;
  status: "SUCCESS" | "FAILED" | "DEGRADED";
  provider: string;
  elapsed_ms: number;
  created_time: string;
  workflow_version?: string;
  patient_id?: string;
  encounter_id?: string;
}

function statusTag(status: RecentRow["status"]) {
  if (status === "SUCCESS") return <Tag color="success">成功</Tag>;
  if (status === "DEGRADED") return <Tag color="warning">降级</Tag>;
  return <Tag color="error">失败</Tag>;
}

const COLUMNS: ColumnsType<RecentRow> = [
  {
    title: "调用 ID",
    dataIndex: "invocation_id",
    key: "invocation_id",
    width: 220,
    render: (id: string) => <Text code>{id}</Text>,
  },
  {
    title: "工作流",
    dataIndex: "workflow_code",
    key: "workflow_code",
    render: (code: string, row) => (
      <span>
        {code}
        {row.workflow_version ? ` @ v${row.workflow_version}` : ""}
      </span>
    ),
  },
  {
    title: "状态",
    dataIndex: "status",
    key: "status",
    width: 90,
    render: statusTag,
  },
  {
    title: "Provider",
    dataIndex: "provider",
    key: "provider",
    width: 160,
    render: (p: string) =>
      PROVIDER_LABELS[p as ProviderType] ? `${PROVIDER_LABELS[p as ProviderType]}` : p,
  },
  {
    title: "耗时",
    dataIndex: "elapsed_ms",
    key: "elapsed_ms",
    width: 100,
    render: (ms: number) => `${ms} ms`,
  },
  {
    title: "时间",
    dataIndex: "created_time",
    key: "created_time",
    width: 180,
  },
];

export default function InvocationStatsCard({ stats }: InvocationStatsCardProps) {
  if (!stats) {
    return <Text type="secondary">暂无调用统计</Text>;
  }
  return (
    <>
      <div className={styles.statsRow} role="status">
        <Statistic title="累计调用" value={stats.total ?? 0} />
        <Statistic title="成功" value={stats.success ?? 0} />
        <Statistic title="失败" value={stats.failed ?? 0} />
        <Statistic title="降级（走兜底）" value={stats.degraded ?? 0} />
        {stats.avg_elapsed_ms !== undefined && (
          <Statistic title="平均耗时" value={stats.avg_elapsed_ms} suffix="ms" />
        )}
      </div>
      <Table<RecentRow>
        className={styles.recentTable}
        size="small"
        rowKey="invocation_id"
        columns={COLUMNS}
        dataSource={stats.recent ?? []}
        pagination={{ pageSize: 10, showSizeChanger: false }}
        locale={{ emptyText: "最近调用记录为空" }}
        aria-label="recent-invocations"
      />
    </>
  );
}
