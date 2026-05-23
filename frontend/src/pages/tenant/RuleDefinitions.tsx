import { Table, Button, Tag, Space } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type ConfigStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  { id: "r1", name: "DRG MS-30 入组校验", category: "医保审核", severity: "blocker", hits: 12834, status: "active" as ConfigStatus },
  { id: "r2", name: "抗菌药 24h 限制使用", category: "医嘱安全", severity: "warning", hits: 4521, status: "active" as ConfigStatus },
  { id: "r3", name: "高血压三月内未随访", category: "质控规则", severity: "info", hits: 287, status: "published" as ConfigStatus },
  { id: "r4", name: "DRG 8 月新政（DIP 草案）", category: "医保审核", severity: "blocker", hits: 0, status: "pending_review" as ConfigStatus },
];

const SEVERITY_COLOR: Record<string, string> = { blocker: "red", warning: "orange", info: "blue" };

export default function RuleDefinitions() {
  return (
    <PageShell
      title="规则库"
      description="医保审核 / 医嘱安全 / 质控 3 大类规则，从模板创建，DSL 仅专家模式可见"
      primary={<Button type="primary" icon={<PlusOutlined />}>从模板创建</Button>}
    >
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "规则名", dataIndex: "name" },
          { title: "分类", dataIndex: "category", render: (v) => <Tag color="purple">{v}</Tag> },
          {
            title: "严重度",
            dataIndex: "severity",
            render: (v: string) => <Tag color={SEVERITY_COLOR[v]}>{v.toUpperCase()}</Tag>,
          },
          { title: "近 30 日命中", dataIndex: "hits", align: "right" as const },
          { title: "状态", dataIndex: "status", render: (s: ConfigStatus) => <StatusBadge machine="config" status={s} /> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">试运行</Button>
                <Button type="link" size="small">编辑</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
