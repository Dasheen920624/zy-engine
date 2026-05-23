import { Table, Tag, Button, Space } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type ConfigStatus } from "@/shared/ui/StatusBadge";

const MOCK = [
  { id: "e1", name: "三甲复评 2024", category: "评级", count: 86, status: "active" as ConfigStatus },
  { id: "e2", name: "NCIS 国家平台月报", category: "上报", count: 124, status: "active" as ConfigStatus },
  { id: "e3", name: "DRG 入组分析", category: "医保", count: 32, status: "published" as ConfigStatus },
  { id: "e4", name: "VTE 预防执行率", category: "公共卫生", count: 18, status: "draft" as ConfigStatus },
];

export default function QcEvalSets() {
  return (
    <PageShell
      title="评估指标库"
      description="按评级 / 上报 / 医保 / 公共卫生 分类的指标定义，支持版本化"
      primary={<Button type="primary" icon={<PlusOutlined />}>新增指标集</Button>}
    >
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "指标集", dataIndex: "name" },
          { title: "分类", dataIndex: "category", render: (v) => <Tag color="cyan">{v}</Tag> },
          { title: "指标数", dataIndex: "count", align: "right" as const },
          { title: "状态", dataIndex: "status", render: (s: ConfigStatus) => <StatusBadge machine="config" status={s} /> },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">编辑</Button>
                <Button type="link" size="small">查看结果</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
