import { Card, Row, Col, Space, Typography, Button } from "antd";
import { LinkOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const LINKS = [
  {
    name: "Swagger UI · OpenAPI 3.1",
    url: "/medkernel/swagger-ui.html",
    desc: "全部 Controller + DTO 文档",
  },
  {
    name: "Actuator · 健康端点",
    url: "/medkernel/actuator/health",
    desc: "JVM + 数据源 + Provider 健康",
  },
  {
    name: "Actuator · Prometheus",
    url: "/medkernel/actuator/prometheus",
    desc: "Micrometer 全量指标导出",
  },
  { name: "Grafana 看板", url: "http://grafana.local/d/medkernel", desc: "5 大业务看板" },
  { name: "OTel Tempo 追踪", url: "http://tempo.local", desc: "分布式 trace" },
  { name: "Loki 日志", url: "http://loki.local", desc: "结构化日志检索" },
];

export default function DevConsole() {
  return (
    <PageShell title="开发者控制台" description="技术快捷入口 · 仅架构师 / 信息科主任 / SRE 可见">
      <Row gutter={[16, 16]}>
        {LINKS.map((l) => (
          <Col span={8} key={l.name}>
            <Card>
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                <strong>{l.name}</strong>
                <Typography.Text type="secondary">{l.desc}</Typography.Text>
                <Button type="link" icon={<LinkOutlined />} href={l.url} target="_blank">
                  打开
                </Button>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>
    </PageShell>
  );
}
