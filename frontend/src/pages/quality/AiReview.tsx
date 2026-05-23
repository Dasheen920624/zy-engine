import { useState } from "react";
import { Table, Tag, Button, Space, Card, Typography, Modal, Progress, Descriptions, List } from "antd";
import { RobotOutlined, CheckOutlined, CloseOutlined, AuditOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { useLlmExplain } from "@/shared/api/hooks";

const MOCK = [
  { id: "1", text: "胸痛 AMI 患者建议 90 分钟内 PCI", confidence: 0.96, sources: 3, status: "待审核" },
  { id: "2", text: "卒中 4.5h 内 rt-PA 溶栓适应症", confidence: 0.93, sources: 5, status: "待审核" },
  { id: "3", text: "DRG MS-30 入组分组逻辑（草案）", confidence: 0.81, sources: 2, status: "已采纳" },
  { id: "4", text: "高血压随访间隔建议（≥ 80 岁）", confidence: 0.62, sources: 1, status: "已拒绝" },
];

const STATUS: Record<string, string> = { 待审核: "orange", 已采纳: "green", 已拒绝: "red" };
const BAND: Record<string, string> = { 高: "green", 中: "blue", 低: "orange" };
const SOURCE_COLOR: Record<string, string> = { guideline: "purple", paper: "blue", kb: "cyan", rule: "magenta" };

export default function AiReview() {
  const [explainId, setExplainId] = useState<string | undefined>();
  const explain = useLlmExplain(explainId);

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
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" icon={<AuditOutlined />} onClick={() => setExplainId(row.id)}>
                  可解释性
                </Button>
                <Button type="link" size="small" icon={<CheckOutlined />}>采纳</Button>
                <Button type="link" size="small" icon={<CloseOutlined />} danger>拒绝</Button>
              </Space>
            ),
          },
        ]}
      />

      {/* GA-EXT-14 · AI 决策可解释性看板 */}
      <Modal
        open={!!explainId}
        onCancel={() => setExplainId(undefined)}
        footer={null}
        title="AI 决策可解释性"
        width={720}
      >
        {explain.isLoading && <Typography.Text>加载中...</Typography.Text>}
        {explain.data && (
          <Space direction="vertical" size="middle" style={{ width: "100%" }}>
            <Card size="small">
              <Typography.Title level={5} style={{ margin: 0 }}>{explain.data.shortAnswer}</Typography.Title>
              <Space style={{ marginTop: 8 }}>
                <span>AI 置信度</span>
                <Tag color={BAND[explain.data.confidenceBand]}>
                  {explain.data.confidenceBand} · {(explain.data.confidence * 100).toFixed(0)}%
                </Tag>
              </Space>
              <Progress percent={Math.round(explain.data.confidence * 100)} strokeColor="#1565c0" />
            </Card>

            <Card size="small" title="证据来源">
              <List
                size="small"
                dataSource={explain.data.sources}
                renderItem={(s) => (
                  <List.Item>
                    <Space>
                      <Tag color={SOURCE_COLOR[s.type] ?? "default"}>{s.type}</Tag>
                      <strong>{s.title}</strong>
                      <Typography.Text type="secondary">· {s.anchor} · {s.publishedAt}</Typography.Text>
                    </Space>
                  </List.Item>
                )}
              />
            </Card>

            <Descriptions size="small" column={1} bordered>
              <Descriptions.Item label="AI 模型">{explain.data.aiModel}</Descriptions.Item>
              <Descriptions.Item label="训练数据范围">{explain.data.trainingDataRange}</Descriptions.Item>
              <Descriptions.Item label="合规警示">
                <Typography.Text type="warning">{explain.data.warning}</Typography.Text>
              </Descriptions.Item>
            </Descriptions>
          </Space>
        )}
      </Modal>
    </PageShell>
  );
}
