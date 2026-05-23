import { Table, Button, Tag } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type ConfigStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  { id: "p1", name: "胸痛 AMI 急诊路径", disease: "AMI / 急性心肌梗死", department: "心内 + 急诊", nodes: 12, status: "active" as ConfigStatus },
  { id: "p2", name: "急性脑卒中绿色通道", disease: "卒中", department: "神经 + 急诊", nodes: 15, status: "published" as ConfigStatus },
  { id: "p3", name: "原发性高血压管理", disease: "高血压", department: "全科 / 心内", nodes: 8, status: "pending_review" as ConfigStatus },
  { id: "p4", name: "2 型糖尿病规范化管理", disease: "T2DM", department: "内分泌", nodes: 10, status: "draft" as ConfigStatus },
];

export default function PathwayTemplates() {
  return (
    <PageShell
      title="路径配置"
      description="按专病维度的路径模板。普通模式表单+时间线，专家模式才打开 X6 节点画布"
      primary={<Button type="primary" icon={<PlusOutlined />}>新建路径</Button>}
    >
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "路径名称", dataIndex: "name" },
          { title: "病种", dataIndex: "disease", render: (v) => <Tag color="blue">{v}</Tag> },
          { title: "归属科室", dataIndex: "department" },
          { title: "节点数", dataIndex: "nodes" },
          { title: "状态", dataIndex: "status", render: (s: ConfigStatus) => <StatusBadge machine="config" status={s} /> },
          { title: "操作", render: () => <Button type="link" size="small">编辑</Button> },
        ]}
      />
    </PageShell>
  );
}
