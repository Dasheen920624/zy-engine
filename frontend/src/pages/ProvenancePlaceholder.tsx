import { Alert, Card, Typography } from "antd";
import styles from "./provenancePlaceholder.module.css";

const { Paragraph } = Typography;

export default function ProvenancePlaceholder() {
  return (
    <div>
      <div className="page-header">
        <h1>来源追溯</h1>
        <div className="subtitle">FE-008 任务 · 待实现</div>
      </div>
      <Alert
        type="warning"
        showIcon
        message="本页面是占位页"
        description="FE-008 将在此实现来源文档库 / 引用片段 / 资产绑定 / 审核记录 / 影响分析。依赖 PROV-001 ~ PROV-006 后端能力。"
      />
      <Card title="将对接的后端接口（PROV-xxx 落地后）" className={styles.apiCard}>
        <Paragraph><code>GET /api/provenance/documents</code></Paragraph>
        <Paragraph><code>GET /api/provenance/citations</code></Paragraph>
        <Paragraph><code>GET /api/provenance/bindings</code></Paragraph>
        <Paragraph><code>GET /api/provenance/impact</code></Paragraph>
      </Card>
    </div>
  );
}
