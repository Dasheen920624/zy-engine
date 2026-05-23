import { Card, Input, Space, Tag, Empty, Alert } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

export default function GraphExplore() {
  return (
    <PageShell
      title="图谱查询"
      description="医学知识图谱（1.2 亿节点 · 8.4 亿关系）· 专家调试与可视化"
    >
      <Alert
        type="info"
        showIcon
        message="高级工具 · 仅架构师 / 实施工程师 / 路径专家可见"
        description="客户主路径不展示节点 / 边 / Cypher 等技术细节。图谱降级由 Provider 状态自动管理。"
      />
      <Card style={{ marginTop: 16 }}>
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          <Input.Search placeholder="搜实体（如：胸痛 / 阿司匹林 / I21.0）" defaultValue="胸痛" />
          <Space>
            <Tag color="blue">实体</Tag>
            <Tag color="purple">关系</Tag>
            <Tag color="cyan">证据</Tag>
          </Space>
          <Empty
            description={
              <Space direction="vertical">
                <span>图谱可视化由 GA-ADVANCED-01 W4 业务深化时接 X6 + Neo4j 5.23</span>
                <span style={{ color: "#999" }}>当前为高级工具骨架</span>
              </Space>
            }
          />
        </Space>
      </Card>
    </PageShell>
  );
}
