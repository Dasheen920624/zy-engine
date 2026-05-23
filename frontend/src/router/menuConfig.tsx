import type { ReactNode } from "react";
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  ClusterOutlined,
  ContainerOutlined,
  DatabaseOutlined,
  DesktopOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  IdcardOutlined,
  KeyOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  ReadOutlined,
  RobotOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  ToolOutlined,
  UnorderedListOutlined,
  UserSwitchOutlined,
} from "@ant-design/icons";

export interface MenuItem {
  key: string;
  label: string;
  icon?: ReactNode;
  path?: string;
  roles?: string[];
  disabled?: boolean;
  pr?: string;
}

export interface MenuSection {
  /** section key，纯标识，不展示给用户 */
  key: string;
  /** section 在侧边栏渲染的分组标题，null 表示无分组（顶部直放） */
  label: string | null;
  items: MenuItem[];
}

/**
 * 侧边栏菜单结构（v1.0 GA 收口）。
 *
 * 产品原则见 docs/PRODUCT_SIMPLIFICATION_V1_GA.md：
 * 试点准备 → 临床运行 → 质控改进 → 合规运维，高级能力二级展开。
 * 交互形态固定为左侧 SideMenu；顶部 Header 不承载主菜单。
 * `pr` 字段保留任务溯源，不展示给客户。
 */
export const menuSections: MenuSection[] = [
  {
    key: "top",
    label: null,
    items: [
      {
        key: "dashboard",
        label: "工作台",
        icon: <AppstoreOutlined />,
        path: "/dashboard",
      },
      {
        key: "demo-validation",
        label: "演示与校验",
        icon: <ExperimentOutlined />,
        path: "/demo-validation",
      },
    ],
  },

  {
    key: "pilot-setup",
    label: "试点准备",
    items: [
      {
        key: "implementation-guide",
        label: "客户实施向导",
        icon: <RocketOutlined />,
        path: "/onboarding/implementation-guide",
        pr: "FE-014",
      },
      {
        key: "tenant-onboarding",
        label: "租户开通",
        icon: <ShopOutlined />,
        path: "/tenant/onboarding",
        pr: "PR-FINAL-10",
      },
      {
        key: "tenant-application",
        label: "租户申请",
        icon: <IdcardOutlined />,
        path: "/tenant/application",
      },
      {
        key: "config-packages",
        label: "配置包中心",
        icon: <ContainerOutlined />,
        path: "/config/packages",
      },
      {
        key: "pathway-templates",
        label: "路径配置",
        icon: <NodeIndexOutlined />,
        path: "/pathway/templates",
      },
      {
        key: "rule-definitions",
        label: "规则库",
        icon: <SafetyCertificateOutlined />,
        path: "/rule/definitions",
        pr: "PR-FINAL-11",
      },
      {
        key: "terminology-mapping",
        label: "字典映射",
        icon: <MedicineBoxOutlined />,
        path: "/terminology/mapping",
      },
      {
        key: "adapter-hub",
        label: "适配器中心",
        icon: <ClusterOutlined />,
        path: "/adapter/hub",
        pr: "PR-FINAL-12",
      },
    ],
  },

  {
    key: "clinical-run",
    label: "临床运行",
    items: [
      {
        key: "mpi",
        label: "患者主索引",
        icon: <IdcardOutlined />,
        path: "/mpi/patients",
        pr: "PR-FINAL-07",
      },
      {
        key: "patient-pathways",
        label: "患者路径",
        icon: <UserSwitchOutlined />,
        path: "/pathway/patients",
        pr: "PATHWAY-ENGINE-COMPLETE",
      },
      {
        key: "cdss-fatigue",
        label: "临床提醒治理",
        icon: <SafetyCertificateOutlined />,
        path: "/cdss/fatigue",
      },
      {
        key: "rule-validate",
        label: "规则校验",
        icon: <SafetyCertificateOutlined />,
        path: "/rule/validate",
        pr: "PR-V3-09",
      },
      {
        key: "workflow-todos",
        label: "待办中心",
        icon: <UnorderedListOutlined />,
        path: "/workflow/todos",
      },
      {
        key: "notifications",
        label: "通知中心",
        icon: <BellOutlined />,
        path: "/notifications",
      },
    ],
  },

  {
    key: "quality-improve",
    label: "质控改进",
    items: [
      {
        key: "qc-dashboard",
        label: "院级质控驾驶舱",
        icon: <LineChartOutlined />,
        path: "/qc/dashboard",
      },
      {
        key: "qc-alerts",
        label: "质控预警",
        icon: <AuditOutlined />,
        path: "/qc/alerts",
      },
      {
        key: "insurance-audit",
        label: "医保智能审核",
        icon: <FileSearchOutlined />,
        path: "/qc/insurance",
        pr: "PR-V2-12",
      },
      {
        key: "qc-eval-sets",
        label: "评估指标库",
        icon: <LineChartOutlined />,
        path: "/qc/eval/sets",
      },
      {
        key: "qc-eval-results",
        label: "评估结果",
        icon: <LineChartOutlined />,
        path: "/qc/eval/results",
      },
      {
        key: "aik-review",
        label: "AI 知识审核",
        icon: <ReadOutlined />,
        path: "/aik/sources",
      },
      {
        key: "aik-review-workbench",
        label: "知识审核台",
        icon: <ReadOutlined />,
        path: "/aik/review",
        pr: "PR-V2-05",
      },
      {
        key: "aik-candidates",
        label: "候选审核台",
        icon: <RobotOutlined />,
        path: "/aik/candidates",
      },
    ],
  },

  {
    key: "compliance-ops",
    label: "合规运维",
    items: [
      {
        key: "admin-users",
        label: "用户管理",
        icon: <UserSwitchOutlined />,
        path: "/admin/users",
        pr: "PR-FINAL-08",
      },
      {
        key: "identity-bindings",
        label: "身份绑定",
        icon: <UserSwitchOutlined />,
        path: "/security/identity-binding",
      },
      {
        key: "admin-audit",
        label: "审计日志",
        icon: <DatabaseOutlined />,
        path: "/admin/audit",
        pr: "PR-FINAL-09",
      },
      {
        key: "admin-service-accounts",
        label: "服务账号管理",
        icon: <KeyOutlined />,
        path: "/admin/service-accounts",
      },
      {
        key: "security-baseline",
        label: "安全基线",
        icon: <SafetyCertificateOutlined />,
        path: "/security/baseline",
      },
      {
        key: "admin-license",
        label: "授权管理",
        icon: <SafetyCertificateOutlined />,
        path: "/admin/license",
      },
      {
        key: "admin-usage",
        label: "用量报告",
        icon: <BarChartOutlined />,
        path: "/admin/usage",
      },
      {
        key: "system-providers",
        label: "Provider 状态",
        icon: <DesktopOutlined />,
        path: "/system/providers",
      },
      {
        key: "notification-settings",
        label: "通知设置",
        icon: <ToolOutlined />,
        path: "/notifications/settings",
      },
    ],
  },
  {
    key: "advanced-tools",
    label: "高级工具",
    items: [
      {
        key: "provenance",
        label: "来源追溯",
        icon: <FileSearchOutlined />,
        path: "/provenance",
      },
      {
        key: "graph-explore",
        label: "图谱查询",
        icon: <ClusterOutlined />,
        path: "/graph/explore",
        pr: "PR-V2-05",
      },
      {
        key: "ai-workflows",
        label: "AI 工作流",
        icon: <RobotOutlined />,
        path: "/ai-workflows",
        pr: "PR-FINAL-13",
      },
    ],
  },
];

/**
 * 扁平化所有菜单项，便于路由匹配 / 面包屑 / 占位页查找。
 */
export function getAllMenuItems(): MenuItem[] {
  return menuSections.flatMap((s) => s.items);
}

/**
 * 根据当前 pathname 找到匹配的 MenuItem（精确前缀匹配）。
 * 用于：高亮当前菜单、面包屑、占位页 hint。
 */
export function findMenuItemByPath(pathname: string): MenuItem | undefined {
  return getAllMenuItems().find(
    (it) => it.path && (it.path === pathname || pathname.startsWith(it.path + "/")),
  );
}

/**
 * 根据当前 pathname 找到所在的 section（用于面包屑生成"知识工厂 / 路径配置"二段式）。
 */
export function findSectionByPath(pathname: string): MenuSection | undefined {
  for (const section of menuSections) {
    if (section.items.some((it) => it.path && (it.path === pathname || pathname.startsWith(it.path + "/")))) {
      return section;
    }
  }
  return undefined;
}
