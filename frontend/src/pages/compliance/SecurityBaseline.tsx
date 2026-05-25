import { Card, Table, Tag, Progress, Space, Typography, theme as antdTheme } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

const ITEMS = [
  {
    id: "1",
    category: "身份与认证",
    item: "MFA 启用",
    status: "满足",
    evidence: "已启用国密 CA + 手机短信",
  },
  {
    id: "2",
    category: "访问控制",
    item: "RBAC 最小权限",
    status: "满足",
    evidence: "5 角色 × 8 资源类型",
  },
  {
    id: "3",
    category: "数据加密",
    item: "敏感字段字段级加密",
    status: "满足",
    evidence: "SM4 + KMS 密钥轮换",
  },
  {
    id: "4",
    category: "数据加密",
    item: "传输 TLS + 国密",
    status: "满足",
    evidence: "TLS 1.3 + SM2 证书",
  },
  { id: "5", category: "审计", item: "审计链验签", status: "满足", evidence: "SM3 哈希链 + TSA" },
  {
    id: "6",
    category: "等保 2.0",
    item: "等保三级控制点 156 项",
    status: "缺证据",
    evidence: "外部测评中（W9 完成）",
  },
  {
    id: "7",
    category: "商密评测",
    item: "GM/T 0054 商密评测",
    status: "缺证据",
    evidence: "外部预审中（W9 完成）",
  },
];

const ST: Record<string, string> = { 满足: "green", 缺证据: "orange", 需整改: "red" };

export default function SecurityBaseline() {
  const { token } = antdTheme.useToken();

  return (
    <PageShell
      title="安全基线"
      description="等保 2.0 三级 + 商密评测 + 个保法 自查清单，状态一目了然"
    >
      <Card title="本院当前安全得分">
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          <Progress percent={88} strokeColor={token.colorSuccess} />
          <Typography.Text>
            5/7 项已满足证据；2 项等待外部评测（等保 + 商密，预计 W9 完成）。
          </Typography.Text>
        </Space>
      </Card>
      <Table
        rowKey="id"
        dataSource={ITEMS}
        scroll={{ x: "max-content" }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        columns={[
          { title: "分类", dataIndex: "category", render: (v) => <Tag>{v}</Tag> },
          { title: "控制点", dataIndex: "item" },
          { title: "状态", dataIndex: "status", render: (v) => <Tag color={ST[v]}>{v}</Tag> },
          { title: "证据", dataIndex: "evidence" },
        ]}
      />
    </PageShell>
  );
}
