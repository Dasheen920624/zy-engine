import { Table, Tag, Button, Space } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { MetricGrid } from "@/shared/ui/MetricGrid";

const MOCK = [
  {
    id: "w1",
    name: "AMI 急诊一键分析",
    provider: "Ollama · 通义 Q2.5-7b",
    invocations: 1283,
    avgLatency: 1.2,
    status: "active",
  },
  {
    id: "w2",
    name: "DRG 编码助手",
    provider: "OpenAI 兼容 · 文心 4.0",
    invocations: 832,
    avgLatency: 0.8,
    status: "active",
  },
  {
    id: "w3",
    name: "病案首页质控",
    provider: "Ollama · 通义 Q2.5-7b",
    invocations: 528,
    avgLatency: 3.5,
    status: "draft",
  },
];

export default function AiWorkflows() {
  return (
    <PageShell
      title="AI 工作流"
      description="模型网关 + 降级链 + 工作流模板（专家调试）"
      primary={<Button type="primary">新建工作流</Button>}
    >
      <MetricGrid
        items={[
          { key: "calls", title: "今日调用", value: 2643 },
          { key: "latency", title: "P95 延迟（秒）", value: 1.4, precision: 1 },
          { key: "fallback", title: "降级次数", value: 23, tone: "warning" },
          { key: "cost", title: "本月成本（¥）", value: 4283 },
        ]}
      />
      <Table
        rowKey="id"
        dataSource={MOCK}
        scroll={{ x: "max-content" }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        columns={[
          { title: "工作流", dataIndex: "name" },
          { title: "Provider", dataIndex: "provider", render: (v) => <Tag color="cyan">{v}</Tag> },
          { title: "今日调用", dataIndex: "invocations", align: "right" as const },
          {
            title: "P95（秒）",
            dataIndex: "avgLatency",
            align: "right" as const,
            render: (v) => v.toFixed(1),
          },
          {
            title: "状态",
            dataIndex: "status",
            render: (v) => <Tag color={v === "active" ? "green" : "default"}>{v}</Tag>,
          },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">
                  查看链路
                </Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
