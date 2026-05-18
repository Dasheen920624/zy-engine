import { Alert, Card, Col, Row, Statistic, Tag, Typography } from "antd";
import { Link } from "react-router-dom";

const { Paragraph } = Typography;

export default function Dashboard() {
  return (
    <div>
      <div className="page-header">
        <h1>工作台</h1>
        <div className="subtitle">FE-002 脚手架已就绪 · 业务页面将由 FE-003～FE-010 逐步落地</div>
      </div>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
        message="这是 FE-002 前端工程脚手架"
        description={
          <>
            <Paragraph style={{ marginBottom: 8 }}>
              本工程已具备：路由、Layout、AntD 主题、组织上下文（自动随 Header 发送）、统一 API client（含 traceId + ApiError）、TanStack Query 缓存、MSW mock、vitest 测试。
            </Paragraph>
            <Paragraph style={{ marginBottom: 0 }}>
              可前往 <Link to="/system/providers">Provider 状态页</Link> 查看接入真实后端 <code>/api/system/providers</code> 的效果；其它页面（演示与校验 / 配置包中心 / 来源追溯）当前为占位页，等待 FE-003 等任务交付。
            </Paragraph>
          </>
        }
      />

      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="脚手架版本" value="0.1.0" />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="技术栈" value="React 18 + TS + Vite" />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="UI 库" value="Ant Design 5" />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="目标后端"
              valueRender={() => <Tag color="blue">/medkernel/api</Tag>}
            />
          </Card>
        </Col>
      </Row>

      <Card title="下一步交付（任务台账）" style={{ marginTop: 24 }}>
        <ul style={{ paddingLeft: 20, margin: 0, lineHeight: 2 }}>
          <li>
            <strong>FE-003</strong> — 演示与规则校验工作台（AMI / EMR_QC / INSURANCE_QC / ORDER_SAFETY 4 类剧本 dry-run）
          </li>
          <li>
            <strong>FE-004</strong> — 配置包中心（列表 / Review / diff / publish / rollback）
          </li>
          <li>
            <strong>FE-005</strong> — 规则配置器（条件树 + 来源卡片 + dry-run）
          </li>
          <li>
            <strong>FE-007</strong> — 质控看板（ECharts 报表 + 多组织下钻）
          </li>
          <li>
            <strong>FE-008</strong> — 来源追溯前端（来源库 / 引用 / 资产绑定 / 影响分析）
          </li>
          <li>
            <strong>FE-010</strong> — Playwright E2E + axe-core 可访问性扫描
          </li>
        </ul>
      </Card>
    </div>
  );
}
