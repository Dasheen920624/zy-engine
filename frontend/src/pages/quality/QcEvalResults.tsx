import { Table, Tag, Button, Card, Space, Progress } from "antd";
import { ExportOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "r1", indicator: "DRG 入组率", target: 95, actual: 96.8, trend: "↑ +1.2", status: "达标" },
  { id: "r2", indicator: "CDSS 提醒采纳率", target: 75, actual: 78, trend: "↑ +3", status: "达标" },
  { id: "r3", indicator: "抗菌药使用率（急诊）", target: 60, actual: 67, trend: "→ 持平", status: "超阈值" },
  { id: "r4", indicator: "VTE 预防执行率", target: 90, actual: 88, trend: "↓ -2", status: "未达标" },
];

const STATUS: Record<string, string> = { 达标: "green", 超阈值: "orange", 未达标: "red" };

export default function QcEvalResults() {
  return (
    <PageShell
      title="评估结果"
      description="月度 / 季度 / 年度评估结果，可导出 PDF/Excel 院级报告"
      primary={<Button type="primary" icon={<ExportOutlined />}>导出本月评估报告</Button>}
    >
      <Card title="本月综合得分">
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          <Progress percent={86} strokeColor={{ from: "#1565c0", to: "#52c41a" }} />
          <Space>综合得分 <strong>86 / 100</strong> · 在 6 家同级三甲对标中排第 <strong>2 名</strong></Space>
        </Space>
      </Card>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "指标", dataIndex: "indicator" },
          { title: "目标", dataIndex: "target", align: "right" as const, render: (v) => `${v}%` },
          { title: "本月实际", dataIndex: "actual", align: "right" as const, render: (v) => `${v}%` },
          { title: "趋势", dataIndex: "trend" },
          { title: "状态", dataIndex: "status", render: (v) => <Tag color={STATUS[v]}>{v}</Tag> },
        ]}
      />
    </PageShell>
  );
}
