import { Table, Tag, Button, Space, message } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type AlertStatus } from "@/shared/ui/StatusBadge";
import { useCdssAlerts, useCdssDecide } from "@/shared/api/hooks";
import { MetricGrid } from "@/shared/ui/MetricGrid";
import { PageState } from "@/shared/ui/PageState";

export default function CdssFatigue() {
  const alerts = useCdssAlerts();
  const decide = useCdssDecide();

  function handleDecide(id: string, decision: "adopt" | "reject") {
    decide.mutate(
      { id, decision, reason: decision === "reject" ? "医生说明：不适用此患者" : undefined },
      {
        onSuccess: () => message.success(`已${decision === "adopt" ? "采纳" : "拒绝"}并写入审计`),
      },
    );
  }

  const totalAdoption = alerts.data?.length
    ? alerts.data.reduce((s, a) => s + a.adoptionRate, 0) / alerts.data.length
    : 0;

  return (
    <PageShell
      title="临床提醒治理"
      description={'医嘱提醒疲劳治理 · 医生只看到与自己患者相关的，主操作只有"采纳 / 不采纳"'}
      primary={<Button type="primary">治理疲劳规则</Button>}
    >
      <MetricGrid
        items={[
          { key: "today", title: "今日提醒", value: alerts.data?.length ?? 0 },
          {
            key: "adoption",
            title: "平均采纳率",
            value: (totalAdoption * 100).toFixed(0),
            suffix: "%",
            tone: "success",
          },
          { key: "dept", title: "覆盖科室", value: 18 },
          { key: "risk", title: "疲劳风险规则", value: 3, tone: "warning" },
        ]}
      />
      <PageState
        state={
          alerts.isLoading
            ? "loading"
            : alerts.isError
              ? "error"
              : (alerts.data?.length ?? 0) === 0
                ? "empty"
                : "ready"
        }
        title="暂无临床提醒"
        onRetry={() => void alerts.refetch()}
      >
        <Table
          rowKey="id"
          dataSource={alerts.data ?? []}
          scroll={{ x: "max-content" }}
          pagination={{ pageSize: 20, showSizeChanger: true }}
          columns={[
            { title: "提醒内容", dataIndex: "text" },
            {
              title: "来源",
              dataIndex: "source",
              render: (v: string) => <Tag color="blue">{v}</Tag>,
            },
            {
              title: "采纳率",
              dataIndex: "adoptionRate",
              render: (v: number) => `${(v * 100).toFixed(0)}%`,
            },
            { title: "医生", dataIndex: "doctor" },
            {
              title: "状态",
              dataIndex: "status",
              render: (s: AlertStatus) => <StatusBadge machine="alert" status={s} />,
            },
            {
              title: "操作",
              render: (_, row) => (
                <Space>
                  <Button
                    type="link"
                    size="small"
                    onClick={() => handleDecide(row.id, "adopt")}
                    loading={decide.isPending}
                  >
                    采纳
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    onClick={() => handleDecide(row.id, "reject")}
                    loading={decide.isPending}
                  >
                    不采纳并说明
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </PageState>
    </PageShell>
  );
}
