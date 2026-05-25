import { Card, Row, Col, Progress, Space, Typography, theme as antdTheme } from "antd";
import { ArrowDownOutlined, ArrowUpOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";
import { MetricGrid } from "@/shared/ui/MetricGrid";

const DEPTS = [
  { name: "心内科", score: 92, trend: "up" },
  { name: "神经内科", score: 88, trend: "up" },
  { name: "急诊科", score: 75, trend: "down" },
  { name: "ICU", score: 90, trend: "up" },
  { name: "全科医学", score: 82, trend: "up" },
  { name: "外科", score: 79, trend: "down" },
];

export default function QcDashboard() {
  const { token } = antdTheme.useToken();

  return (
    <PageShell
      title="院级质控驾驶舱"
      description="院长 / 医务处 / 质控办 1 屏看院级 + 科室级指标，0 技术名词"
    >
      <MetricGrid
        items={[
          {
            key: "closure",
            title: "本月整改闭环率",
            value: 84.2,
            precision: 1,
            suffix: "%",
            prefix: <ArrowUpOutlined />,
            tone: "success",
          },
          {
            key: "drg",
            title: "DRG 入组率",
            value: 96.8,
            precision: 1,
            suffix: "%",
            tone: "success",
          },
          { key: "cdss", title: "CDSS 提醒采纳率", value: 78, suffix: "%" },
          {
            key: "denial",
            title: "医保拒付率",
            value: 2.3,
            precision: 1,
            suffix: "%",
            prefix: <ArrowDownOutlined />,
            tone: "danger",
          },
        ]}
      />
      <Card title="科室质控得分（满分 100）">
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          {DEPTS.map((d) => (
            <Row key={d.name} align="middle">
              <Col span={4}>
                <Typography.Text strong>{d.name}</Typography.Text>
              </Col>
              <Col span={18}>
                <Progress
                  percent={d.score}
                  strokeColor={
                    d.score >= 90
                      ? token.colorSuccess
                      : d.score >= 80
                        ? token.colorPrimary
                        : token.colorWarning
                  }
                />
              </Col>
              <Col span={2} style={{ textAlign: "right" }}>
                {d.trend === "up" ? (
                  <ArrowUpOutlined style={{ color: token.colorSuccess }} />
                ) : (
                  <ArrowDownOutlined style={{ color: token.colorError }} />
                )}
              </Col>
            </Row>
          ))}
        </Space>
      </Card>
    </PageShell>
  );
}
