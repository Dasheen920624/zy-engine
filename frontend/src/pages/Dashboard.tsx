import { Card, Col, Row, Statistic, Tag, Typography, Space } from "antd";
import { Link } from "react-router-dom";
import styles from "./Dashboard.module.css";
import {
  ApartmentOutlined,
  AuditOutlined,
  CheckCircleOutlined,
  ClusterOutlined,
  DesktopOutlined,
  FileSearchOutlined,
  IdcardOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  ReadOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  ShareAltOutlined,
} from "@ant-design/icons";

const { Title, Paragraph, Text } = Typography;

interface CapabilityCard {
  group: "知识工厂" | "质控驾驶舱" | "用户与身份" | "系统";
  icon: React.ReactNode;
  title: string;
  description: string;
  status: "READY" | "BETA" | "PENDING";
  path?: string;
}

/**
 * 工作台首页（重构版，反映 2026-05 全功能版本）。
 *
 * - 顶部 4 个核心指标卡（演示用 hardcode，PR-V2-12 后接 /api/quality/summary）
 * - "知识工厂" 8 卡 + "质控驾驶舱" 8 卡 + "用户与身份" 3 卡 + "系统" 5 卡
 * - 每张卡含状态徽标：READY 可用 / BETA 体验 / PENDING 占位
 * - 设计意图：医院内网管理台首屏即可俯瞰平台 4 大治理域的运行情况
 */
export default function Dashboard() {
  return (
    <div>
      <div className="mk-page-header">
        <Title level={3} className={styles.pageTitle}>
          工作台
        </Title>
        <Paragraph type="secondary" className={styles.pageDescription}>
          集团医疗智能中枢 MedKernel · 知识工厂 / 质控驾驶舱 / 用户与身份 / 平台监控 四大模块
        </Paragraph>
      </div>

      {/* 核心指标 */}
      <Row gutter={[16, 16]} className={styles.metricsRow}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已落地后端模块"
              value={28}
              valueStyle={{ color: "var(--mk-brand-primary-active)" }}
              suffix={<Text type="secondary" className={styles.statSuffix}>个 Java 包</Text>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已落地前端页面"
              value={20}
              valueStyle={{ color: "var(--mk-brand-primary-active)" }}
              suffix={<Text type="secondary" className={styles.statSuffix}>个模块</Text>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="契约测试"
              value="248 / 248"
              valueStyle={{ color: "var(--mk-success)" }}
              suffix={<Text type="secondary" className={styles.statSuffix}>0 错</Text>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="后端编译状态"
              value="GREEN"
              valueStyle={{ color: "var(--mk-success)" }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <CapabilitySection
        title="知识工厂"
        description="配置包、路径、规则、图谱、字典、适配器、AI 工作流引擎与来源——一切对医院 / 集团运营产生影响的资产都在此版本化"
        cards={CONFIG_GOVERNANCE_CARDS}
      />

      <CapabilitySection
        title="质控驾驶舱"
        description="质控大盘、智能评估、CDSS 警报疲劳、AI 知识审核、待办与通知——把 AI 决策可监管化"
        cards={OPERATIONS_GOVERNANCE_CARDS}
      />

      <CapabilitySection
        title="用户与身份"
        description="患者主索引、多身份源绑定、外网租户开通——支持院内 + 外网双轨身份治理"
        cards={TENANT_IDENTITY_CARDS}
      />

      <CapabilitySection
        title="平台监控"
        description="安全基线、Provider 状态、审计日志、用户/通知设置——平台可观测可维护"
        cards={SYSTEM_CARDS}
      />
    </div>
  );
}

function CapabilitySection({
  title,
  description,
  cards,
}: {
  title: string;
  description: string;
  cards: CapabilityCard[];
}) {
  return (
    <div className={styles.section}>
      <div className={styles.sectionHead}>
        <Title level={4} className={styles.pageTitle}>
          {title}
        </Title>
        <Paragraph type="secondary" className={styles.sectionDescription}>
          {description}
        </Paragraph>
      </div>
      <Row gutter={[16, 16]}>
        {cards.map((card) => (
          <Col key={card.title} xs={24} sm={12} md={8} lg={6}>
            <CardItem card={card} />
          </Col>
        ))}
      </Row>
    </div>
  );
}

function CardItem({ card }: { card: CapabilityCard }) {
  const tag = STATUS_TAG[card.status];
  const body = (
    <Card hoverable className={styles.card}>
      <Space direction="vertical" size={8} className={styles.cardSpace}>
        <div className={styles.cardHeader}>
          <span className={styles.cardIcon}>
            {card.icon}
          </span>
          <Text strong>{card.title}</Text>
          <Tag color={tag.color} className={styles.cardStatusTag}>
            {tag.label}
          </Tag>
        </div>
        <Paragraph
          type="secondary"
          className={styles.cardDescription}
        >
          {card.description}
        </Paragraph>
      </Space>
    </Card>
  );

  if (card.path && card.status !== "PENDING") {
    return <Link to={card.path}>{body}</Link>;
  }

  return body;
}

const STATUS_TAG: Record<CapabilityCard["status"], { color: string; label: string }> = {
  READY: { color: "success", label: "已上线" },
  BETA: { color: "processing", label: "体验中" },
  PENDING: { color: "default", label: "占位" },
};

const CONFIG_GOVERNANCE_CARDS: CapabilityCard[] = [
  {
    group: "知识工厂",
    icon: <ApartmentOutlined />,
    title: "配置包中心",
    description: "PKG-001..004 / PROV-004：配置包版本化、Review、Diff、发布与回滚",
    status: "READY",
    path: "/config/packages",
  },
  {
    group: "知识工厂",
    icon: <NodeIndexOutlined />,
    title: "路径配置",
    description: "PATH-001..008：临床路径模板列表、版本对比、X6 画布编辑器",
    status: "READY",
    path: "/pathway/templates",
  },
  {
    group: "知识工厂",
    icon: <SafetyCertificateOutlined />,
    title: "规则配置",
    description: "RULE-001..008：规则 DSL、命中证据、来源卡片、dry-run 模拟",
    status: "BETA",
    path: "/rule/definitions",
  },
  {
    group: "知识工厂",
    icon: <ShareAltOutlined />,
    title: "图谱配置",
    description: "GRAPH-001..005：Neo4j 投影、Dry-run 同步、版本管理",
    status: "BETA",
    path: "/graph/explore",
  },
  {
    group: "知识工厂",
    icon: <MedicineBoxOutlined />,
    title: "字典映射",
    description: "TERM-001/002：本地术语 ↔ ICD/RxNorm/LOINC 映射工作台",
    status: "READY",
    path: "/terminology/mapping",
  },
  {
    group: "知识工厂",
    icon: <ClusterOutlined />,
    title: "适配器中心",
    description: "ADAPT-001 / INTEROP-001：HIS/EMR/LIS 适配器、组织绑定、CDS Hooks",
    status: "READY",
    path: "/adapter/hub",
  },
  {
    group: "知识工厂",
    icon: <RobotOutlined />,
    title: "AI 工作流引擎",
    description: "ADR-0013 LLM Gateway：国产大模型直连为主，Dify 退化为可选 WORKFLOW Provider",
    status: "READY",
    path: "/ai-workflows",
  },
  {
    group: "知识工厂",
    icon: <FileSearchOutlined />,
    title: "来源追溯",
    description: "PROV-001..003 / REFIT-003：医学/医保来源、引用、资产绑定、发布门禁",
    status: "BETA",
    path: "/provenance",
  },
];

const OPERATIONS_GOVERNANCE_CARDS: CapabilityCard[] = [
  {
    group: "质控驾驶舱",
    icon: <LineChartOutlined />,
    title: "院级质控驾驶舱",
    description: "PR-V2-12：医院 / 集团多组织下钻的质控指标看板",
    status: "READY",
    path: "/qc/dashboard",
  },
  {
    group: "质控驾驶舱",
    icon: <AuditOutlined />,
    title: "质控预警",
    description: "PR-V2-11：质控告警工单列表 + 派单",
    status: "READY",
    path: "/qc/alerts",
  },
  {
    group: "质控驾驶舱",
    icon: <LineChartOutlined />,
    title: "评估指标库",
    description: "EVAL-001/002：智能评估指标模型与评分引擎",
    status: "READY",
    path: "/qc/eval/sets",
  },
  {
    group: "质控驾驶舱",
    icon: <LineChartOutlined />,
    title: "评估结果",
    description: "EVAL-002：评估结果与历史对比报告",
    status: "READY",
    path: "/qc/eval/results",
  },
  {
    group: "质控驾驶舱",
    icon: <SafetyCertificateOutlined />,
    title: "CDSS 提醒疲劳",
    description: "CDSS-001..003：医生覆盖记录与提醒疲劳治理",
    status: "READY",
    path: "/cdss/fatigue",
  },
  {
    group: "质控驾驶舱",
    icon: <ReadOutlined />,
    title: "AI 知识审核",
    description: "AIK-001..006：医疗知识订阅、AI 生产任务、模型调用日志、版权治理",
    status: "READY",
    path: "/aik/sources",
  },
  {
    group: "质控驾驶舱",
    icon: <AuditOutlined />,
    title: "待办中心",
    description: "WF-001：审核 / 发布 / 回滚 / 整改 / 合规等统一待办与审批",
    status: "READY",
    path: "/workflow/todos",
  },
  {
    group: "质控驾驶舱",
    icon: <AuditOutlined />,
    title: "通知中心",
    description: "NOTIFY-001：业务通知、订阅设置、已读未读管理",
    status: "READY",
    path: "/notifications",
  },
];

const TENANT_IDENTITY_CARDS: CapabilityCard[] = [
  {
    group: "用户与身份",
    icon: <IdcardOutlined />,
    title: "患者主索引",
    description: "MPI-001：跨院区患者主索引、就诊标识、合并冲突治理",
    status: "READY",
    path: "/mpi/patients",
  },
  {
    group: "用户与身份",
    icon: <IdcardOutlined />,
    title: "身份绑定管理",
    description: "SEC-012：多身份源绑定、合并、解绑、冲突检测",
    status: "READY",
    path: "/security/identity-binding",
  },
  {
    group: "用户与身份",
    icon: <IdcardOutlined />,
    title: "租户开通",
    description: "SEC-011：外网客户租户开通、本地密码 + MFA、服务账号",
    status: "READY",
    path: "/tenant/onboarding",
  },
];

const SYSTEM_CARDS: CapabilityCard[] = [
  {
    group: "系统",
    icon: <SafetyCertificateOutlined />,
    title: "安全基线",
    description: "SEC-010：审计防篡改、密钥轮换、安全基线规则",
    status: "READY",
    path: "/security/baseline",
  },
  {
    group: "系统",
    icon: <DesktopOutlined />,
    title: "Provider 状态",
    description: "OPS-001：DB / Graph / Dify Provider 实时状态与降级原因",
    status: "READY",
    path: "/system/providers",
  },
  {
    group: "系统",
    icon: <IdcardOutlined />,
    title: "用户管理",
    description: "SEC-001 / SEC-006/007：用户、角色、SSO（CAS/OIDC/SAML/LDAP）",
    status: "READY",
    path: "/admin/users",
  },
  {
    group: "系统",
    icon: <AuditOutlined />,
    title: "审计日志",
    description: "AUDIT-001 / SEC-010：ENGINE_AUDIT_LOG 防篡改链、查询、导出",
    status: "READY",
    path: "/admin/audit",
  },
  {
    group: "系统",
    icon: <DesktopOutlined />,
    title: "通知设置",
    description: "NOTIFY-001：订阅类型、推送渠道、免打扰窗口设置",
    status: "READY",
    path: "/notifications/settings",
  },
];
