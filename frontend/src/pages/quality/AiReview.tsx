import { Table, Tag, Button, Space, Card, Typography } from "antd";
import { RobotOutlined, CheckOutlined, CloseOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "1", text: "胸痛 AMI 患者建议 90 分钟内 PCI", confidence: 0.96, sources: 3, status: "待审核" },
  { id: "2", text: "卒中 4.5h 内 rt-PA 溶栓适应症", confidence: 0.93, sources: 5, status: "待审核" },
  { id: "3", text: "DRG MS-30 入组分组逻辑（草案）", confidence: 0.81, sources: 2, status: "已采纳" },
  { id: "4", text: "高血压随访间隔建议（≥ 80 岁）", confidence: 0.62, sources: 1, status: "已拒绝" },
];

const STATUS: Record<string, string> = { 待审核: "orange", 已采纳: "green", 已拒绝: "red" };

export default function AiReview() {
  return (
    <PageShell
      title="AI 知识审核"
      description={"AI 生成 / 抽取的新知识必须经医务处人工审核才能进入指南库（生成式 AI 管理办法 §7 硬约束）"}
    >
      <Card style={{ background: "#fff8e1" }}>
        <Space>
          <RobotOutlined style={{ color: "#faad14" }} />
          <Typography.Text>
            所有 AI 生成内容已强制标识"AI 生成"。医师确认前不进入病历也不指导真实医疗活动。
          </Typography.Text>
        </Space>
      </Card>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "知识点", dataIndex: "text" },
          {
            title: "AI 置信度",
            dataIndex: "confidence",
            render: (v: number) => (
              <Tag color={v >= 0.9 ? "green" : v >= 0.8 ? "blue" : "orange"}>
                {(v * 100).toFixed(0)}%
              </Tag>
            ),
          },
          { title: "证据来源数", dataIndex: "sources" },
          { title: "状态", dataIndex: "status", render: (v) => <Tag color={STATUS[v]}>{v}</Tag> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small" icon={<CheckOutlined />}>采纳</Button>
                <Button type="link" size="small" icon={<CloseOutlined />} danger>拒绝</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
