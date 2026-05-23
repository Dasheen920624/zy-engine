import { Table, Tag, Button, Space, Typography, Card } from "antd";
import { SafetyCertificateOutlined, ExportOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "1", time: "10:23:45.123", user: "张三", action: "发布规则 R-AB-024", trace: "tr-83a9...", sig: "✓ 已验签" },
  { id: "2", time: "10:21:08.502", user: "李四", action: "查询患者 MPI-000123456", trace: "tr-72c1...", sig: "✓ 已验签" },
  { id: "3", time: "10:18:55.991", user: "王医生", action: "采纳 CDSS 提醒 #4521", trace: "tr-99ab...", sig: "✓ 已验签" },
  { id: "4", time: "10:15:32.001", user: "system", action: "国密密钥年度轮换", trace: "tr-001f...", sig: "✓ 已验签 + TSA" },
];

export default function AdminAudit() {
  return (
    <PageShell
      title="审计日志"
      description="所有用户操作 + 系统事件，TSA 时间戳验签，6 个月内随时可查"
      primary={<Button icon={<ExportOutlined />}>导出审计</Button>}
    >
      <Card style={{ background: "#e6f7ff" }}>
        <Space>
          <SafetyCertificateOutlined style={{ color: "#1565c0", fontSize: 18 }} />
          <Typography.Text>
            等保 2.0 三级 + 个保法审计留痕 + 国密 SM2/SM3 验签 + TSA 国家可信时间戳
          </Typography.Text>
        </Space>
      </Card>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "时间", dataIndex: "time" },
          { title: "用户", dataIndex: "user" },
          { title: "操作", dataIndex: "action" },
          { title: "Trace ID", dataIndex: "trace", render: (v) => <Tag>{v}</Tag> },
          { title: "签名", dataIndex: "sig", render: (v) => <span style={{ color: "#52c41a" }}>{v}</span> },
        ]}
      />
    </PageShell>
  );
}
