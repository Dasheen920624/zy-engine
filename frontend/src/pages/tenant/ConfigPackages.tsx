import { Table, Button, Space, Tag } from "antd";
import { ImportOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { StatusBadge, type ConfigStatus } from "@/shared/ui/StatusBadge";

interface ConfigPack {
  id: string;
  name: string;
  scope: string;
  version: string;
  status: ConfigStatus;
  updatedAt: string;
}

const MOCK: ConfigPack[] = [
  { id: "p1", name: "胸痛 AMI 标准包", scope: "心内科", version: "v2.3", status: "active", updatedAt: "2026-05-20" },
  { id: "p2", name: "卒中绿色通道", scope: "神经内科", version: "v1.5", status: "published", updatedAt: "2026-05-18" },
  { id: "p3", name: "高血压基层管理", scope: "全院", version: "v1.0", status: "pending_review", updatedAt: "2026-05-22" },
  { id: "p4", name: "DRG 8 月规则更新", scope: "医保办", version: "v3.1", status: "draft", updatedAt: "2026-05-23" },
];

export default function ConfigPackages() {
  return (
    <PageShell
      title="配置包中心"
      description="路径 / 规则 / 字典 / 适配器配置打包发布，跨院区一键复制"
      primary={
        <Button type="primary" icon={<ImportOutlined />}>
          导入配置包
        </Button>
      }
    >
      <Table
        rowKey="id"
        dataSource={MOCK}
        pagination={false}
        columns={[
          { title: "名称", dataIndex: "name" },
          { title: "范围", dataIndex: "scope", render: (v) => <Tag>{v}</Tag> },
          { title: "版本", dataIndex: "version" },
          {
            title: "状态",
            dataIndex: "status",
            render: (s: ConfigStatus) => <StatusBadge machine="config" status={s} />,
          },
          { title: "更新时间", dataIndex: "updatedAt" },
          {
            title: "操作",
            render: () => (
              <Space>
                <Button type="link" size="small">查看 7 步流</Button>
                <Button type="link" size="small">发布</Button>
              </Space>
            ),
          },
        ]}
      />
    </PageShell>
  );
}
