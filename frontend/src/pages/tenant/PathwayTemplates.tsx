import { Table, Button, Tag, Spin, message } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type ConfigStatus } from "@/shared/ui/StatusBadge";
import { useColumnManager } from "@/shared/ui/ColumnManager";
import { usePathwayTemplates, usePublishPathway } from "@/shared/api/hooks";

interface PathwayCol {
  key: string;
  title: string;
  always?: boolean;
}

const ALL_COLUMNS: PathwayCol[] = [
  { key: "name", title: "路径名称", always: true },
  { key: "disease", title: "病种" },
  { key: "department", title: "归属科室" },
  { key: "nodes", title: "节点数" },
  { key: "status", title: "状态" },
  { key: "action", title: "操作", always: true },
];

export default function PathwayTemplates() {
  const list = usePathwayTemplates();
  const publish = usePublishPathway();
  const { visibleColumns, columnManager } = useColumnManager<PathwayCol>("pathway-templates", ALL_COLUMNS);

  const allRender: Record<string, (v: unknown, r: { id: string; name: string; disease: string; department: string; nodes: number; status: string }) => React.ReactNode> = {
    name: (_, r) => r.name,
    disease: (_, r) => <Tag color="blue">{r.disease}</Tag>,
    department: (_, r) => r.department,
    nodes: (_, r) => r.nodes,
    status: (_, r) => <StatusBadge machine="config" status={r.status as ConfigStatus} />,
    action: (_, r) => (
      <Button
        type="link"
        size="small"
        loading={publish.isPending}
        onClick={() =>
          publish.mutate(r.id, {
            onSuccess: (d) => message.success(`已进入${d.stage === "canary" ? "灰度" : "全量"}（${d.rollout}）`),
          })
        }
      >
        发布
      </Button>
    ),
  };

  return (
    <PageShell
      title="路径配置"
      description="按专病维度的路径模板。普通模式表单+时间线，专家模式才打开 X6 节点画布"
      primary={<Button type="primary" icon={<PlusOutlined />}>新建路径</Button>}
      extras={columnManager}
    >
      {list.isLoading ? (
        <Spin />
      ) : (
        <Table
          rowKey="id"
          dataSource={list.data ?? []}
          pagination={false}
          columns={visibleColumns.map((c) => ({
            title: c.title,
            key: c.key,
            render: allRender[c.key],
          }))}
        />
      )}
    </PageShell>
  );
}
