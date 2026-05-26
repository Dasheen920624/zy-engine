import { Card, Row, Col, Tag, Space, Typography } from "antd";
import { PageShell } from "@/shared/ui/PageShell";

type ProvStatus = "ok" | "warn" | "degraded";

const PROVIDERS: { category: string; name: string; status: ProvStatus; detail: string }[] = [
  {
    category: "数据库",
    name: "Oracle 23ai · 主库",
    status: "ok",
    detail: "QPS 1.2k · P95 18ms · 连接池 12/40",
  },
  {
    category: "数据库",
    name: "Oracle 23ai · DG 备库",
    status: "ok",
    detail: "RPO < 5s · 应用滞后 3s",
  },
  { category: "知识图谱", name: "Neo4j 5.23", status: "ok", detail: "1.2 亿节点 · 8.4 亿关系" },
  {
    category: "大模型",
    name: "Ollama · 通义 Q2.5-7b（本地）",
    status: "ok",
    detail: "P95 1.2s · 平均 token/s 142",
  },
  {
    category: "大模型",
    name: "OpenAI 兼容 · 文心 4.0（外部）",
    status: "warn",
    detail: "限流 80% · 启用降级到 Ollama",
  },
  {
    category: "外部系统",
    name: "总院 PACS（东软 7）",
    status: "degraded",
    detail: "心跳失败 5 分钟 · 已派单",
  },
  { category: "缓存", name: "Redis 7.2 · 主从", status: "ok", detail: "命中率 96.4%" },
  { category: "可信时间戳", name: "TSA · BJCA", status: "ok", detail: "今日签发 12834 次" },
];

const LABEL: Record<ProvStatus, string> = { ok: "正常", warn: "限流中", degraded: "降级" };
const TAG_COLOR: Record<ProvStatus, string> = { ok: "success", warn: "warning", degraded: "error" };

export default function SystemProviders() {
  return (
    <PageShell
      title="Provider 状态"
      description="数据库 / 模型 / 图谱 / 外部系统 / 缓存 / 时间戳 健康一屏看完"
    >
      <Row gutter={[16, 16]}>
        {PROVIDERS.map((p) => (
          <Col span={8} key={p.name}>
            <Card>
              <Space direction="vertical" size="small" className="mk-full-width">
                <Space className="mk-flex-between">
                  <Tag>{p.category}</Tag>
                  <Tag color={TAG_COLOR[p.status]}>{LABEL[p.status]}</Tag>
                </Space>
                <strong>{p.name}</strong>
                <Typography.Text type="secondary">{p.detail}</Typography.Text>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </PageShell>
  );
}
