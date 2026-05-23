import { Card, Steps, Table, Tag, Button, Space } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

const MOCK = [
  { id: "p1", patient: "张** (M58)", pathway: "胸痛 AMI 急诊路径", currentNode: "急诊评估", started: "2026-05-23 10:32", risk: "high" },
  { id: "p2", patient: "李** (F42)", pathway: "卒中绿色通道", currentNode: "DSA 中", started: "2026-05-23 09:15", risk: "high" },
  { id: "p3", patient: "王** (M65)", pathway: "高血压管理", currentNode: "随访 - 3 月", started: "2026-02-12", risk: "low" },
];

const RISK_COLOR: Record<string, string> = { high: "red", medium: "orange", low: "blue" };

export default function PatientPathways() {
  return (
    <PageShell
      title="患者路径"
      description="患者入径、节点流转、变异登记；医生只看与自己科室相关的患者"
      primary={<Button type="primary">入径检索</Button>}
    >
      <Card title="当前患者节点示例 · 张** 胸痛路径">
        <Steps
          current={2}
          items={[
            { title: "院前 120" },
            { title: "急诊接诊" },
            { title: "急诊评估", description: "ECG / 肌钙蛋白" },
            { title: "导管室准备" },
            { title: "PCI 手术" },
            { title: "CCU 监护" },
            { title: "出院随访" },
          ]}
        />
      </Card>
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "患者", dataIndex: "patient" },
          { title: "路径", dataIndex: "pathway" },
          { title: "当前节点", dataIndex: "currentNode", render: (v) => <Tag color="processing">{v}</Tag> },
          { title: "入径时间", dataIndex: "started" },
          { title: "风险", dataIndex: "risk", render: (v) => <Tag color={RISK_COLOR[v]}>{v.toUpperCase()}</Tag> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">推进节点</Button>
                <Button type="link" size="small">登记变异</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
