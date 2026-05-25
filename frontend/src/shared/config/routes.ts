/**
 * 路由元数据（single source of truth）。
 * AppLayout + router + 菜单 + 面包屑 + 权限元数据全部读这里。
 */
export type RouteSectionKey =
  | "workbench"
  | "pilot-setup"
  | "clinical-run"
  | "quality-improve"
  | "compliance-ops"
  | "advanced-tools";

export type PageType =
  | "auth"
  | "workbench"
  | "list"
  | "configuration"
  | "dashboard"
  | "review"
  | "advanced"
  | "system";

export interface RouteMeta {
  path: string;
  title: string;
  breadcrumb: string[];
  requireAuth: boolean;
  sectionKey?: RouteSectionKey;
  menuKey?: string;
  menuLabel?: string;
  roles?: string[];
  permissions?: string[];
  hidden?: boolean;
  pageType?: PageType;
  stateMachine?: "config" | "change" | "todo" | "alert";
}

export interface RouteSectionMeta {
  key: RouteSectionKey;
  label: string;
  hidden?: boolean;
}

export const routeSections: RouteSectionMeta[] = [
  { key: "workbench", label: "工作台" },
  { key: "pilot-setup", label: "试点准备" },
  { key: "clinical-run", label: "临床运行" },
  { key: "quality-improve", label: "质控改进" },
  { key: "compliance-ops", label: "合规运维" },
  { key: "advanced-tools", label: "高级工具", hidden: true },
];

export const routeMetas: RouteMeta[] = [
  {
    path: "/login",
    title: "登录",
    breadcrumb: ["登录"],
    requireAuth: false,
    hidden: true,
    pageType: "auth",
  },
  {
    path: "/",
    title: "工作台",
    breadcrumb: ["工作台"],
    requireAuth: true,
    hidden: true,
    pageType: "workbench",
  },
  {
    path: "/dashboard",
    title: "工作台",
    breadcrumb: ["工作台"],
    requireAuth: true,
    sectionKey: "workbench",
    menuKey: "workbench",
    menuLabel: "工作台",
    pageType: "workbench",
  },
  {
    path: "/onboarding/guide",
    title: "客户实施向导",
    breadcrumb: ["试点准备", "客户实施向导"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "implementation-guide",
    menuLabel: "客户实施向导",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/tenant/onboarding",
    title: "租户开通",
    breadcrumb: ["试点准备", "租户开通"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "tenant-onboarding",
    menuLabel: "租户开通",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/config/packages",
    title: "配置包中心",
    breadcrumb: ["试点准备", "配置包中心"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "config-packages",
    menuLabel: "配置包中心",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/config/packages/demo",
    title: "7 步流演示",
    breadcrumb: ["试点准备", "配置包中心", "7 步流演示"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    hidden: true,
    pageType: "configuration",
    stateMachine: "change",
  },
  {
    path: "/pathway/templates",
    title: "路径配置",
    breadcrumb: ["试点准备", "路径配置"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "pathway-templates",
    menuLabel: "路径配置",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/rule/definitions",
    title: "规则库",
    breadcrumb: ["试点准备", "规则库"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "rule-definitions",
    menuLabel: "规则库",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/terminology/mapping",
    title: "字典映射",
    breadcrumb: ["试点准备", "字典映射"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "terminology-mapping",
    menuLabel: "字典映射",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/adapter/hub",
    title: "适配器中心",
    breadcrumb: ["试点准备", "适配器中心"],
    requireAuth: true,
    sectionKey: "pilot-setup",
    menuKey: "adapter-hub",
    menuLabel: "适配器中心",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/mpi",
    title: "患者主索引",
    breadcrumb: ["临床运行", "患者主索引"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "mpi",
    menuLabel: "患者主索引",
    pageType: "list",
  },
  {
    path: "/pathway/patients",
    title: "患者路径",
    breadcrumb: ["临床运行", "患者路径"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "patient-pathways",
    menuLabel: "患者路径",
    pageType: "list",
    stateMachine: "todo",
  },
  {
    path: "/cdss/fatigue",
    title: "临床提醒治理",
    breadcrumb: ["临床运行", "临床提醒治理"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "cdss-fatigue",
    menuLabel: "临床提醒治理",
    pageType: "list",
    stateMachine: "alert",
  },
  {
    path: "/rule/validate",
    title: "规则校验",
    breadcrumb: ["临床运行", "规则校验"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "rule-validate",
    menuLabel: "规则校验",
    pageType: "configuration",
  },
  {
    path: "/workflow/todos",
    title: "待办中心",
    breadcrumb: ["临床运行", "待办中心"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "workflow-todos",
    menuLabel: "待办中心",
    pageType: "list",
    stateMachine: "todo",
  },
  {
    path: "/notifications",
    title: "通知中心",
    breadcrumb: ["临床运行", "通知中心"],
    requireAuth: true,
    sectionKey: "clinical-run",
    menuKey: "notifications",
    menuLabel: "通知中心",
    pageType: "list",
    stateMachine: "todo",
  },
  {
    path: "/qc/dashboard",
    title: "院级质控驾驶舱",
    breadcrumb: ["质控改进", "院级质控驾驶舱"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "qc-dashboard",
    menuLabel: "院级质控驾驶舱",
    pageType: "dashboard",
  },
  {
    path: "/qc/alerts",
    title: "质控预警",
    breadcrumb: ["质控改进", "质控预警"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "qc-alerts",
    menuLabel: "质控预警",
    pageType: "list",
    stateMachine: "alert",
  },
  {
    path: "/qc/insurance",
    title: "医保智能审核",
    breadcrumb: ["质控改进", "医保智能审核"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "insurance-audit",
    menuLabel: "医保智能审核",
    pageType: "review",
    stateMachine: "config",
  },
  {
    path: "/qc/eval/sets",
    title: "评估指标库",
    breadcrumb: ["质控改进", "评估指标库"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "qc-eval-sets",
    menuLabel: "评估指标库",
    pageType: "configuration",
    stateMachine: "config",
  },
  {
    path: "/qc/eval/results",
    title: "评估结果",
    breadcrumb: ["质控改进", "评估结果"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "qc-eval-results",
    menuLabel: "评估结果",
    pageType: "list",
  },
  {
    path: "/aik/review",
    title: "AI 知识审核",
    breadcrumb: ["质控改进", "AI 知识审核"],
    requireAuth: true,
    sectionKey: "quality-improve",
    menuKey: "aik-review",
    menuLabel: "AI 知识审核",
    pageType: "review",
    stateMachine: "config",
  },
  {
    path: "/admin/users",
    title: "用户管理",
    breadcrumb: ["合规运维", "用户管理"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "admin-users",
    menuLabel: "用户管理",
    pageType: "list",
  },
  {
    path: "/security/identity-binding",
    title: "身份绑定",
    breadcrumb: ["合规运维", "身份绑定"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "identity-bindings",
    menuLabel: "身份绑定",
    pageType: "system",
  },
  {
    path: "/admin/audit",
    title: "审计日志",
    breadcrumb: ["合规运维", "审计日志"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "admin-audit",
    menuLabel: "审计日志",
    pageType: "list",
  },
  {
    path: "/security/baseline",
    title: "安全基线",
    breadcrumb: ["合规运维", "安全基线"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "security-baseline",
    menuLabel: "安全基线",
    pageType: "dashboard",
  },
  {
    path: "/system/providers",
    title: "Provider 状态",
    breadcrumb: ["合规运维", "Provider 状态"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "system-providers",
    menuLabel: "Provider 状态",
    pageType: "system",
  },
  {
    path: "/notifications/settings",
    title: "通知设置",
    breadcrumb: ["合规运维", "通知设置"],
    requireAuth: true,
    sectionKey: "compliance-ops",
    menuKey: "notification-settings",
    menuLabel: "通知设置",
    pageType: "configuration",
  },
  {
    path: "/advanced/provenance",
    title: "来源追溯",
    breadcrumb: ["高级工具", "来源追溯"],
    requireAuth: true,
    sectionKey: "advanced-tools",
    menuKey: "provenance",
    menuLabel: "来源追溯",
    hidden: true,
    pageType: "advanced",
  },
  {
    path: "/advanced/graph",
    title: "图谱查询",
    breadcrumb: ["高级工具", "图谱查询"],
    requireAuth: true,
    sectionKey: "advanced-tools",
    menuKey: "graph-explore",
    menuLabel: "图谱查询",
    hidden: true,
    pageType: "advanced",
  },
  {
    path: "/advanced/ai-workflows",
    title: "AI 工作流",
    breadcrumb: ["高级工具", "AI 工作流"],
    requireAuth: true,
    sectionKey: "advanced-tools",
    menuKey: "ai-workflows",
    menuLabel: "AI 工作流",
    hidden: true,
    pageType: "advanced",
  },
  {
    path: "/advanced/domestic",
    title: "国产化自检",
    breadcrumb: ["高级工具", "国产化自检"],
    requireAuth: true,
    sectionKey: "advanced-tools",
    menuKey: "domestic-check",
    menuLabel: "国产化自检",
    hidden: true,
    pageType: "advanced",
  },
  {
    path: "/advanced/dev-console",
    title: "开发者控制台",
    breadcrumb: ["高级工具", "开发者控制台"],
    requireAuth: true,
    sectionKey: "advanced-tools",
    menuKey: "dev-console",
    menuLabel: "开发者控制台",
    hidden: true,
    pageType: "advanced",
  },
  {
    path: "*",
    title: "未找到页面",
    breadcrumb: ["未找到页面"],
    requireAuth: false,
    hidden: true,
    pageType: "system",
  },
];

export const customerRouteMetas = routeMetas.filter(
  (route) => route.requireAuth && !route.hidden && route.sectionKey !== "advanced-tools",
);

export function findRouteByPath(path: string): RouteMeta | undefined {
  return routeMetas.find((route) => route.path === path);
}

export function getRouteBreadcrumb(path: string): string[] {
  return findRouteByPath(path)?.breadcrumb ?? ["未找到页面"];
}

export function getRouteTitle(path: string): string {
  return findRouteByPath(path)?.title ?? "未找到页面";
}
