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
  ShareAltOutlined,
  ShopOutlined,
  TeamOutlined,
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
 * 两段式侧边栏菜单结构（PR-V2-03 原始设计 + 2026-05 全功能扩展）。
 *
 * 设计原则：
 * 1) 工作台 + 演示 直接放最顶；
 * 2) "配置治理" 收纳一切"医院/集团配置 + 引擎配置 + 字典 + 适配器 + 来源"等治理面；
 * 3) "运营治理" 收纳一切"看板/质控/评估/AI 知识/待办/通知/患者主索引/身份/租户/系统监控"等运行面；
 * 4) 单条菜单超过 12 行时仍按业务亲和分组，不让用户滚动找入口；
 * 5) `pr` 字段保留任务编号溯源；`disabled`/`placeholderHint` 提示尚未完成的入口。
 */
export const menuSections: MenuSection[] = [
  // ─── 顶部直放：工作台 + 演示 ───────────────────────────
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

  // ─── 配置治理 ─────────────────────────────────────────
  {
    key: "config-governance",
    label: "配置治理",
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
        key: "rule-definitions",
        label: "规则配置",
        icon: <SafetyCertificateOutlined />,
        path: "/rule/definitions",
        pr: "PR-V2-05",
        placeholderHint: "规则库列表与 DSL 编辑器规划中",
      },
      {
        key: "graph-explore",
        label: "图谱配置",
        icon: <ShareAltOutlined />,
        path: "/graph/explore",
        pr: "PR-V2-05",
        placeholderHint: "图谱查询工作台规划中",
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
        placeholderHint: "ADAPT-001 已落地后端，前端列表待补",
      },
      {
        key: "dify-workflows",
        label: "Dify 工作流",
        icon: <RobotOutlined />,
        path: "/dify/workflows",
        placeholderHint: "DIFY-002 模板后台已就绪，前端列表待补",
      },
      {
        key: "provenance",
        label: "来源追溯",
        icon: <FileSearchOutlined />,
        path: "/provenance",
      },
    ],
  },

  // ─── 运营治理 ─────────────────────────────────────────
  {
    key: "operations-governance",
    label: "运营治理",
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
        path: "/cdss/alert-fatigue",
      },
      {
        key: "aik-review",
        label: "AI 知识审核",
        icon: <ReadOutlined />,
        path: "/aik/review",
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

  // ─── 用户与组织 ───────────────────────────────────────
  {
    key: "tenant-identity",
    label: "用户与组织",
    items: [
      {
        key: "mpi",
        label: "患者主索引",
        icon: <IdcardOutlined />,
        path: "/mpi/patients",
        placeholderHint: "MPI-001 后端已落地，前端管理页待补",
      },
      {
        key: "identity-bindings",
        label: "身份绑定管理",
        icon: <UserSwitchOutlined />,
        path: "/identity/bindings",
      },
      {
        key: "tenant-onboarding",
        label: "租户开通",
        icon: <ShopOutlined />,
        path: "/tenant/onboarding",
        placeholderHint: "SEC-011 后端已落地，前端向导待补",
      },
    ],
  },

  // ─── 系统 ─────────────────────────────────────────────
  {
    key: "system",
    label: "系统",
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
        key: "admin-users",
        label: "用户管理",
        icon: <TeamOutlined />,
        path: "/admin/users",
        placeholderHint: "SEC-001 后端能力到位，前端管理页待补",
      },
      {
        key: "admin-audit",
        label: "审计日志",
        icon: <DatabaseOutlined />,
        path: "/admin/audit",
        placeholderHint: "AUDIT-001 后端能力到位，前端查询页待补",
      },
      {
        key: "notification-settings",
        label: "通知设置",
        icon: <ToolOutlined />,
        path: "/notifications/settings",
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
 * 根据当前 pathname 找到所在的 section（用于面包屑生成"配置治理 / 路径配置"二段式）。
 */
export function findSectionByPath(pathname: string): MenuSection | undefined {
  for (const section of menuSections) {
    if (section.items.some((it) => it.path && (it.path === pathname || pathname.startsWith(it.path + "/")))) {
      return section;
    }
  }
  return undefined;
}
