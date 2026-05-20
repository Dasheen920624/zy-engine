import { Alert, Card, Col, Row, Statistic, Tag, Typography } from "antd";
import { Link } from "react-router-dom";

const { Paragraph } = Typography;

export default function Dashboard() {
  return (
    <div>
      <div className="page-header">
        <h1>工作台</h1>
        <div className="subtitle">前端治理控制台 · 配置包中心和 Provider 状态已可联调</div>
      </div>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
        message="当前前端能力核对"
        description={
          <>
            <Paragraph style={{ marginBottom: 8 }}>
              本工程已具备：路由、Layout、AntD 主题、组织上下文（自动随 Header 发送）、统一 API client（含 traceId + ApiError）、TanStack Query 缓存、MSW mock、vitest 测试。
            </Paragraph>
            <Paragraph style={{ marginBottom: 0 }}>
              已可用页面：<Link to="/system/providers">Provider 状态页</Link>、<Link to="/config/packages">配置包中心</Link>、<Link to="/demo-validation">演示与校验工作台</Link>。来源追溯以及更多治理页面按任务台账分阶段交付。
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

      <Card title="后续交付（任务台账）" style={{ marginTop: 24 }}>
        <ul style={{ paddingLeft: 20, margin: 0, lineHeight: 2 }}>
          <li>
            <strong>FE-003</strong> — 演示与规则校验工作台已补齐，后续接入真实 Oracle 生产库联调回归
          </li>
          <li>
            <strong>FE-004</strong> — 配置包中心深化（rollback / 跨环境导入导出 / 权限态 / 生产联调）
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
