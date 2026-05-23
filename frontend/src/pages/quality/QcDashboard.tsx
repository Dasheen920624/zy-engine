import { Card, Row, Col, Statistic, Progress, Space, Typography } from "antd";
import { ArrowDownOutlined, ArrowUpOutlined } from "@ant-design/icons";
import { PageShell } from "@/shared/ui/PageShell";

const DEPTS = [
  { name: "心内科", score: 92, trend: "up" },
  { name: "神经内科", score: 88, trend: "up" },
  { name: "急诊科", score: 75, trend: "down" },
  { name: "ICU", score: 90, trend: "up" },
  { name: "全科医学", score: 82, trend: "up" },
  { name: "外科", score: 79, trend: "down" },
];

export default function QcDashboard() {
  return (
    <PageShell
      title="院级质控驾驶舱"
      description="院长 / 医务处 / 质控办 1 屏看院级 + 科室级指标，0 技术名词"
    >
      <Row gutter={12}>
        <Col span={6}><Card><Statistic title="本月整改闭环率" value={84.2} precision={1} suffix="%" valueStyle={{ color: "#52c41a" }} prefix={<ArrowUpOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="DRG 入组率" value={96.8} precision={1} suffix="%" valueStyle={{ color: "#52c41a" }} /></Card></Col>
        <Col span={6}><Card><Statistic title="CDSS 提醒采纳率" value={78} suffix="%" /></Card></Col>
        <Col span={6}><Card><Statistic title="医保拒付率" value={2.3} precision={1} suffix="%" valueStyle={{ color: "#ff4d4f" }} prefix={<ArrowDownOutlined />} /></Card></Col>
      </Row>
      <Card title="科室质控得分（满分 100）">
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          {DEPTS.map((d) => (
            <Row key={d.name} align="middle">
              <Col span={4}><Typography.Text strong>{d.name}</Typography.Text></Col>
              <Col span={18}>
                <Progress
                  percent={d.score}
                  strokeColor={d.score >= 90 ? "#52c41a" : d.score >= 80 ? "#1565c0" : "#faad14"}
                />
              </Col>
              <Col span={2} style={{ textAlign: "right" }}>
                {d.trend === "up" ? (
                  <ArrowUpOutlined style={{ color: "#52c41a" }} />
                ) : (
                  <ArrowDownOutlined style={{ color: "#ff4d4f" }} />
                )}
              </Col>
            </Row>
          ))}
        </Space>
      </Card>
    </PageShell>
  );
}
