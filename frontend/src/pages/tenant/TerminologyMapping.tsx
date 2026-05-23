import { Table, Button, Tag, Space, Statistic, Row, Col, Card } from "antd";
import { ImportOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "t1", local: "原发性高血压", localCode: "I10.x01", standard: "原发性高血压", standardCode: "I10", system: "ICD-10", confidence: "已确认" },
  { id: "t2", local: "AMI（前壁）", localCode: "I21.001", standard: "ST 段抬高型心梗（前壁）", standardCode: "I21.0", system: "ICD-10", confidence: "已确认" },
  { id: "t3", local: "脑梗塞", localCode: "ZX-001", standard: "脑梗死", standardCode: "I63.9", system: "ICD-10", confidence: "待确认" },
  { id: "t4", local: "甲流", localCode: "X-FLU-A", standard: "流行性感冒甲型", standardCode: "J10", system: "ICD-10", confidence: "未匹配" },
];

const STATUS_COLOR: Record<string, string> = { 已确认: "green", 待确认: "orange", 未匹配: "red" };

export default function TerminologyMapping() {
  return (
    <PageShell
      title="字典映射"
      description="把医院码映射到 ICD-10 / ICD-11 / LOINC / SNOMED CT，AI 推荐 + 人工确认"
      primary={<Button type="primary" icon={<ImportOutlined />}>导入医院字典</Button>}
    >
      <Row gutter={12}>
        <Col span={6}><Card><Statistic title="总条目" value={12345} /></Card></Col>
        <Col span={6}><Card><Statistic title="已确认" value={11203} valueStyle={{ color: "#52c41a" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="待确认" value={1078} valueStyle={{ color: "#faad14" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="未匹配" value={64} valueStyle={{ color: "#ff4d4f" }} /></Card></Col>
      </Row>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "医院术语", dataIndex: "local" },
          { title: "医院码", dataIndex: "localCode" },
          { title: "标准术语", dataIndex: "standard" },
          { title: "标准码", dataIndex: "standardCode" },
          { title: "标准库", dataIndex: "system", render: (v) => <Tag>{v}</Tag> },
          {
            title: "状态",
            dataIndex: "confidence",
            render: (v: string) => <Tag color={STATUS_COLOR[v]}>{v}</Tag>,
          },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">确认</Button>
                <Button type="link" size="small">修改</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
