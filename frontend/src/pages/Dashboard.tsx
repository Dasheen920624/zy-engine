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
  RocketOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  ShareAltOutlined,
} from "@ant-design/icons";

const { Title, Paragraph, Text } = Typography;

interface CapabilityCard {
  group: "试点准备" | "临床运行" | "质控改进" | "合规运维" | "高级工具";
  icon: React.ReactNode;
  title: string;
  description: string;
  status: "READY" | "BETA" | "PENDING";
  path?: string;
}

/**
 * 工作台首页按客户试点旅程组织，而不是按内部技术模块堆功能。
 */
export default function Dashboard() {
  return (
    <div>
      <div className="mk-page-header">
        <Title level={3} className={styles.pageTitle}>
          工作台
        </Title>
        <Paragraph type="secondary" className={styles.pageDescription}>
          集团医疗智能中枢 MedKernel · 试点准备 → 临床运行 → 质控改进 → 合规运维
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
        title="1. 试点准备"
        description="先把医院、院区、院内系统、路径、规则和字典准备好，让试点能顺利开始。"
        cards={PILOT_SETUP_CARDS}
      />

      <CapabilitySection
        title="2. 临床运行"
        description="把患者入径、节点流转、医嘱提醒、医生确认和待办处理串成闭环。"
        cards={CLINICAL_RUN_CARDS}
      />

      <CapabilitySection
        title="3. 质控改进"
        description="让医务处和科室能看到问题、派发整改、复盘趋势，并形成可导出的证据。"
        cards={QUALITY_IMPROVE_CARDS}
      />

      <CapabilitySection
        title="4. 合规运维"
        description="把身份、审计、安全、监控和通知配置收口，支撑等保、国密和客户上线。"
        cards={COMPLIANCE_OPS_CARDS}
      />

      <CapabilitySection
        title="高级工具"
        description="给实施、架构和专家使用的深水区能力，不作为客户第一次演示的主路径。"
        cards={ADVANCED_TOOL_CARDS}
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

const PILOT_SETUP_CARDS: CapabilityCard[] = [
  {
    group: "试点准备",
    icon: <RocketOutlined />,
    title: "客户实施向导",
    description: "按医院试点顺序检查环境、账号、数据、培训和上线准备。",
    status: "READY",
    path: "/onboarding/implementation-guide",
  },
  {
    group: "试点准备",
    icon: <ShopOutlined />,
    title: "租户开通",
    description: "开通医院租户、院区、管理员和试点套餐。",
    status: "READY",
    path: "/tenant/onboarding",
  },
  {
    group: "试点准备",
    icon: <ApartmentOutlined />,
    title: "配置包中心",
    description: "把路径、规则、字典打包成可审核、可发布、可回滚的试点版本。",
    status: "READY",
    path: "/config/packages",
  },
  {
    group: "试点准备",
    icon: <NodeIndexOutlined />,
    title: "路径配置",
    description: "配置专病路径模板，支撑患者入径和节点流转。",
    status: "READY",
    path: "/pathway/templates",
  },
  {
    group: "试点准备",
    icon: <SafetyCertificateOutlined />,
    title: "规则库",
    description: "管理医嘱、医保、质控规则，并保留来源证据。",
    status: "BETA",
    path: "/rule/definitions",
  },
  {
    group: "试点准备",
    icon: <MedicineBoxOutlined />,
    title: "字典映射",
    description: "把院内诊断、医嘱、检验、药品编码映射到标准口径。",
    status: "READY",
    path: "/terminology/mapping",
  },
  {
    group: "试点准备",
    icon: <ClusterOutlined />,
    title: "适配器中心",
    description: "接入 HIS、EMR、LIS、PACS 等院内系统。",
    status: "READY",
    path: "/adapter/hub",
  },
];

const CLINICAL_RUN_CARDS: CapabilityCard[] = [
  {
    group: "临床运行",
    icon: <IdcardOutlined />,
    title: "患者主索引",
    description: "合并跨院区患者身份，处理就诊标识冲突。",
    status: "READY",
    path: "/mpi/patients",
  },
  {
    group: "临床运行",
    icon: <NodeIndexOutlined />,
    title: "患者路径",
    description: "患者入径、节点流转、变异登记和执行状态查看。",
    status: "READY",
    path: "/pathway/patients",
  },
  {
    group: "临床运行",
    icon: <SafetyCertificateOutlined />,
    title: "临床提醒治理",
    description: "治理医嘱提醒、覆盖记录和提醒疲劳。",
    status: "READY",
    path: "/cdss/fatigue",
  },
  {
    group: "临床运行",
    icon: <SafetyCertificateOutlined />,
    title: "规则校验",
    description: "对单患者或单医嘱即时试运行规则，验证提醒是否合理。",
    status: "READY",
    path: "/rule/validate",
  },
  {
    group: "临床运行",
    icon: <AuditOutlined />,
    title: "待办中心",
    description: "处理审批、整改、发布、回滚和合规待办。",
    status: "READY",
    path: "/workflow/todos",
  },
  {
    group: "临床运行",
    icon: <AuditOutlined />,
    title: "通知中心",
    description: "查看业务通知、整改提醒和处理状态。",
    status: "READY",
    path: "/notifications",
  },
];

const QUALITY_IMPROVE_CARDS: CapabilityCard[] = [
  {
    group: "质控改进",
    icon: <LineChartOutlined />,
    title: "院级质控驾驶舱",
    description: "查看全院、院区、科室的质控表现和趋势。",
    status: "READY",
    path: "/qc/dashboard",
  },
  {
    group: "质控改进",
    icon: <AuditOutlined />,
    title: "质控预警",
    description: "发现问题、派发整改、跟踪处理闭环。",
    status: "READY",
    path: "/qc/alerts",
  },
  {
    group: "质控改进",
    icon: <FileSearchOutlined />,
    title: "医保智能审核",
    description: "识别 DRG/DIP 和医保规则风险，辅助审核整改。",
    status: "READY",
    path: "/qc/insurance",
  },
  {
    group: "质控改进",
    icon: <LineChartOutlined />,
    title: "评估指标库",
    description: "维护质控指标、评分规则和适用范围。",
    status: "READY",
    path: "/qc/eval/sets",
  },
  {
    group: "质控改进",
    icon: <LineChartOutlined />,
    title: "评估结果",
    description: "查看评估结果、趋势变化和报告证据。",
    status: "READY",
    path: "/qc/eval/results",
  },
  {
    group: "质控改进",
    icon: <ReadOutlined />,
    title: "AI 知识审核",
    description: "人工审核 AI 生成的医学知识，确保可追溯、可发布。",
    status: "READY",
    path: "/aik/sources",
  },
  {
    group: "质控改进",
    icon: <ReadOutlined />,
    title: "知识审核台",
    description: "集中处理候选知识、来源证据和审核意见。",
    status: "READY",
    path: "/aik/review",
  },
];

const COMPLIANCE_OPS_CARDS: CapabilityCard[] = [
  {
    group: "合规运维",
    icon: <IdcardOutlined />,
    title: "用户管理",
    description: "管理用户、角色、权限和登录安全状态。",
    status: "READY",
    path: "/admin/users",
  },
  {
    group: "合规运维",
    icon: <IdcardOutlined />,
    title: "身份绑定",
    description: "绑定 HIS、LDAP、SSO 等外部身份，减少重复账号。",
    status: "READY",
    path: "/security/identity-binding",
  },
  {
    group: "合规运维",
    icon: <AuditOutlined />,
    title: "审计日志",
    description: "查询关键操作留痕，验证审计链完整性。",
    status: "READY",
    path: "/admin/audit",
  },
  {
    group: "合规运维",
    icon: <SafetyCertificateOutlined />,
    title: "安全基线",
    description: "跟踪等保、国密、弱口令、审计和整改状态。",
    status: "READY",
    path: "/security/baseline",
  },
  {
    group: "合规运维",
    icon: <DesktopOutlined />,
    title: "Provider 状态",
    description: "查看数据库、图谱、模型和外部系统的健康状态。",
    status: "READY",
    path: "/system/providers",
  },
  {
    group: "合规运维",
    icon: <DesktopOutlined />,
    title: "通知设置",
    description: "配置站内信、短信和免打扰策略。",
    status: "READY",
    path: "/notifications/settings",
  },
];

const ADVANCED_TOOL_CARDS: CapabilityCard[] = [
  {
    group: "高级工具",
    icon: <FileSearchOutlined />,
    title: "来源追溯",
    description: "管理指南、文献、知识资产和发布证据链。",
    status: "BETA",
    path: "/provenance",
  },
  {
    group: "高级工具",
    icon: <ShareAltOutlined />,
    title: "图谱查询",
    description: "给专家和实施人员调试医学知识图谱。",
    status: "BETA",
    path: "/graph/explore",
  },
  {
    group: "高级工具",
    icon: <RobotOutlined />,
    title: "AI 工作流",
    description: "管理模型网关、降级链和工作流模板。",
    status: "READY",
    path: "/ai-workflows",
  },
];
