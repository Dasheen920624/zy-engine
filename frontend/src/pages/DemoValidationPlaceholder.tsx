import { Alert, Card, Typography } from "antd";

const { Paragraph } = Typography;

export default function DemoValidationPlaceholder() {
  return (
    <div>
      <div className="page-header">
        <h1>演示与校验工作台</h1>
        <div className="subtitle">FE-003 任务 · 待实现</div>
      </div>
      <Alert
        type="warning"
        showIcon
        message="本页面是占位页"
        description="FE-003 将在此实现 AMI 推荐 / EMR_QC / INSURANCE_QC / ORDER_SAFETY 四类剧本的 dry-run。"
      />
      <Card title="将对接的后端接口" style={{ marginTop: 16 }}>
        <Paragraph>
          <code>POST /api/rule-engine/evaluate</code> — 按 scenario_code 路由评估
        </Paragraph>
        <Paragraph>
          <code>POST /api/rule-engine/batch-evaluate</code> — 批量评估
        </Paragraph>
        <Paragraph>
          <code>GET /api/rule-engine/results/&#123;resultId&#125;</code> — 评估结果回查
        </Paragraph>
        <Paragraph>
          <code>POST /api/patient-pathways/candidates</code> — 候选路径推荐
        </Paragraph>
      </Card>
    </div>
  );
}
