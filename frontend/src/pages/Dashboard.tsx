import { Card, Typography } from "antd";

const { Title, Paragraph } = Typography;

export default function Dashboard() {
  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Title level={3}>集团医疗智能中枢 · MedKernel</Title>
        <Paragraph type="secondary">v1.0 GA · 工作台骨架（GA-PROD-01 待实装）</Paragraph>
        <Paragraph>
          产品宪法见 <code>docs/CONSTITUTION.md</code>，12 周方案见 <code>docs/V1_GA_REWRITE_PLAN.md</code>。
        </Paragraph>
      </Card>
    </div>
  );
}
