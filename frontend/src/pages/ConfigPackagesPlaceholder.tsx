import { Alert, Card, Typography } from "antd";

const { Paragraph } = Typography;

export default function ConfigPackagesPlaceholder() {
  return (
    <div>
      <div className="page-header">
        <h1>配置包中心</h1>
        <div className="subtitle">FE-004 任务 · 待实现</div>
      </div>
      <Alert
        type="warning"
        showIcon
        message="本页面是占位页"
        description="FE-004 将在此实现配置包列表 / 详情 / Review / diff / hash 校验 / publish / rollback / export。视觉与交互参考 frontend-prototype/config-package-center.html。"
      />
      <Card title="将对接的后端接口" style={{ marginTop: 16 }}>
        <Paragraph><code>GET /api/config-packages</code></Paragraph>
        <Paragraph><code>POST /api/config-packages</code></Paragraph>
        <Paragraph><code>GET /api/config-packages/&#123;code&#125;/&#123;version&#125;</code></Paragraph>
        <Paragraph><code>POST /api/config-packages/&#123;code&#125;/&#123;version&#125;/review</code></Paragraph>
        <Paragraph><code>POST /api/config-packages/&#123;code&#125;/&#123;version&#125;/publish</code></Paragraph>
        <Paragraph><code>POST /api/config-packages/&#123;code&#125;/&#123;version&#125;/export</code></Paragraph>
      </Card>
    </div>
  );
}
