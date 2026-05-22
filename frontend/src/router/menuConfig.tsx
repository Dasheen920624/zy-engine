import type { ReactNode } from "react";
import {
  AppstoreOutlined,
  AuditOutlined,
  BellOutlined,
  ClusterOutlined,
  ContainerOutlined,
  DatabaseOutlined,
  DesktopOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  IdcardOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  ReadOutlined,
  RobotOutlined,
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
  /** 占位说明：用于占位页 placeholder hint */
  placeholderHint?: string;
}

export interface MenuSection {
  /** section key，纯标识，不展示给用户 */
  key: string;
  /** section 在侧边栏渲染的分组标题，null 表示无分组（顶部直放） */
  label: string | null;
  items: MenuItem[];
}

/**
 * 侧边栏菜单结构（v0.3-final）。
 *
 * 命名收口（PRODUCT_ARCHITECTURE_FINAL.md §1.1，禁止再用旧叫法）：
 *   M1「知识工厂」← 旧「配置治理」
 *   M3「质控驾驶舱」← 旧「运营治理」
 *   M4「用户与身份」← 旧「用户与组织」
 *   M4「平台监控」← 旧「系统」
 *
 * 占位入口隐藏策略（v0.3-final）：所有未实装的页面**不出现在菜单**，避免客户点击翻车。
 * 路由本身保留为 PlaceholderPage 兜底（直接访问 URL 仍能看到 hint），AI 团队按 docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md 接力实装后再加回菜单。
 *
 * Dify 退化为可选 Provider（ADR-0013 去 Dify 化）：菜单改为「AI 工作流引擎」，去 Dify 品牌。
 *
 * `pr` 字段保留任务编号溯源；`placeholderHint` 仅用于 URL 兜底场景，菜单层面不再展示。
 */
export const menuSections: MenuSection[] = [
  // ─── 顶部直放：工作台 ───────────────────────────────────
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

  // ─── M1 知识工厂 ──────────────────────────────────────
  {
    key: "knowledge-factory",
    label: "知识工厂",
    items: [
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
        key: "patient-pathways",
        label: "患者路径管理",
        icon: <UserSwitchOutlined />,
        path: "/pathway/patients",
        pr: "PATHWAY-ENGINE-COMPLETE",
      },
      {
        key: "terminology-mapping",
        label: "字典映射",
        icon: <MedicineBoxOutlined />,
        path: "/terminology/mapping",
      },
      {
        key: "provenance",
        label: "来源追溯",
        icon: <FileSearchOutlined />,
        path: "/provenance",
      },
      {
        key: "rule-definitions",
        label: "规则库",
        icon: <SafetyCertificateOutlined />,
        path: "/rule/definitions",
        pr: "PR-FINAL-11",
      },
      {
        key: "ai-workflows",
        label: "AI 工作流引擎",
        icon: <RobotOutlined />,
        path: "/ai-workflows",
        pr: "PR-FINAL-13",
      },
      {
        key: "adapter-hub",
        label: "适配器中心",
        icon: <ClusterOutlined />,
        path: "/adapter/hub",
        pr: "PR-FINAL-12",
      },
      // 以下入口待实装（参考 docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md），实装后从下方移到上方：
      //   key: graph-explore      path: /graph/explore      PR-V0.4
    ],
  },

  // ─── M3 质控驾驶舱 ────────────────────────────────────
  {
    key: "cockpit",
    label: "质控驾驶舱",
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
        key: "cdss-fatigue",
        label: "CDSS 提醒疲劳",
        icon: <SafetyCertificateOutlined />,
        path: "/cdss/fatigue",
      },
      {
        key: "aik-review",
        label: "AI 知识审核",
        icon: <ReadOutlined />,
        path: "/aik/sources",
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

  // ─── M4 用户与身份 ────────────────────────────────────
  {
    key: "identity",
    label: "用户与身份",
    items: [
      {
        key: "identity-bindings",
        label: "身份绑定管理",
        icon: <UserSwitchOutlined />,
        path: "/security/identity-binding",
      },
      {
        key: "tenant-onboarding",
        label: "租户开通向导",
        icon: <ShopOutlined />,
        path: "/tenant/onboarding",
        pr: "PR-FINAL-10",
      },
      {
        key: "mpi",
        label: "患者主索引",
        icon: <IdcardOutlined />,
        path: "/mpi/patients",
        pr: "PR-FINAL-07",
      },
      // 以下入口待实装：
      //   key: admin-users        path: /admin/users         PR-FINAL-08
    ],
  },

  // ─── M4 平台监控 ──────────────────────────────────────
  {
    key: "platform",
    label: "平台监控",
    items: [
      {
        key: "security-baseline",
        label: "安全基线",
        icon: <SafetyCertificateOutlined />,
        path: "/security/baseline",
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
      {
        key: "admin-audit",
        label: "审计日志",
        icon: <DatabaseOutlined />,
        path: "/admin/audit",
        pr: "PR-FINAL-09",
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
