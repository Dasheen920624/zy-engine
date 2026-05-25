import { Table, Tag, Space, Button } from "antd";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type TodoStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  {
    id: "t1",
    title: "规则发布审核：DRG 8 月新政草案",
    from: "医保办",
    priority: "high",
    status: "unread" as TodoStatus,
    sla: "今天 17:00",
  },
  {
    id: "t2",
    title: "路径变异登记：李** 卒中转 ICU",
    from: "神经内科",
    priority: "high",
    status: "in_progress" as TodoStatus,
    sla: "已超 30 分钟",
  },
  {
    id: "t3",
    title: "整改派单：合理用药 H 类抗菌药",
    from: "医务处",
    priority: "medium",
    status: "in_progress" as TodoStatus,
    sla: "本周内",
  },
  {
    id: "t4",
    title: "AI 知识审核：胸痛指南 v3 5 条新规则",
    from: "医务处",
    priority: "medium",
    status: "unread" as TodoStatus,
    sla: "本周内",
  },
];

const PRIORITY: Record<string, string> = { high: "red", medium: "orange", low: "blue" };

export default function WorkflowTodos() {
  return (
    <PageShell title="待办中心" description="审批 / 整改 / 发布 / 回滚 4 类待办，按 SLA 倒序">
      <Table
        rowKey="id"
        dataSource={MOCK}
        scroll={{ x: "max-content" }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        columns={[
          { title: "标题", dataIndex: "title" },
          { title: "来源", dataIndex: "from", render: (v) => <Tag>{v}</Tag> },
          {
            title: "优先级",
            dataIndex: "priority",
            render: (v: string) => <Tag color={PRIORITY[v]}>{v.toUpperCase()}</Tag>,
          },
          { title: "SLA", dataIndex: "sla" },
          {
            title: "状态",
            dataIndex: "status",
            render: (s: TodoStatus) => <StatusBadge machine="todo" status={s} />,
          },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">
                  处理
                </Button>
                <Button type="link" size="small">
                  转派
                </Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
