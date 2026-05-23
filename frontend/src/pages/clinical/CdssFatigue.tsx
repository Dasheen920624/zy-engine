import { Card, Table, Tag, Button, Statistic, Row, Col, Space } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type AlertStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  { id: "1", text: "张** · 氯吡格雷 + 阿司匹林联用警告", source: "ACS 指南 2024", adoption: 0.82, status: "closed" as AlertStatus, doctor: "心内 · 王医生" },
  { id: "2", text: "李** · 头孢曲松皮试缺失", source: "医嘱安全规则 R-AB-024", adoption: 0.45, status: "remediating" as AlertStatus, doctor: "急诊 · 刘医生" },
  { id: "3", text: "王** · 高血压未按时随访", source: "质控规则 Q-HBP-008", adoption: 0.91, status: "closed" as AlertStatus, doctor: "全科 · 张医生" },
];

export default function CdssFatigue() {
  return (
    <PageShell
      title="临床提醒治理"
      description="医嘱提醒疲劳治理 · 医生只看到与自己患者相关的，主操作只有“采纳 / 不采纳”"
      primary={<Button type="primary">治理疲劳规则</Button>}
    >
      <Row gutter={12}>
        <Col span={6}><Card><Statistic title="今日提醒" value={1283} /></Card></Col>
        <Col span={6}><Card><Statistic title="采纳率" value={0.78} precision={2} suffix="%" valueStyle={{ color: "#52c41a" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="覆盖科室" value={18} /></Card></Col>
        <Col span={6}><Card><Statistic title="疲劳风险规则" value={3} valueStyle={{ color: "#faad14" }} /></Card></Col>
      </Row>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "提醒内容", dataIndex: "text" },
          { title: "来源", dataIndex: "source", render: (v) => <Tag color="blue">{v}</Tag> },
          {
            title: "采纳率",
            dataIndex: "adoption",
            render: (v: number) => `${(v * 100).toFixed(0)}%`,
          },
          { title: "医生", dataIndex: "doctor" },
          { title: "状态", dataIndex: "status", render: (s: AlertStatus) => <StatusBadge machine="alert" status={s} /> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">采纳</Button>
                <Button type="link" size="small">不采纳并说明</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
