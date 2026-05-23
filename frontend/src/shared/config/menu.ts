/**
 * 5 组菜单 + 30 项二级 + 5 项隐藏式高级工具
 * 与 docs/CONSTITUTION.md §2 锁定。任何新增/修改必须先改宪法。
 */
export interface MenuItem {
  key: string;
  label: string;
  path: string;
}

export interface MenuSection {
  key: string;
  label: string;
  items: MenuItem[];
  hidden?: boolean;
}

export const menuSections: MenuSection[] = [
  {
    key: "workbench",
    label: "工作台",
    items: [{ key: "workbench", label: "工作台", path: "/dashboard" }],
  },
  {
    key: "pilot-setup",
    label: "试点准备",
    items: [
      { key: "implementation-guide", label: "客户实施向导", path: "/onboarding/guide" },
      { key: "tenant-onboarding", label: "租户开通", path: "/tenant/onboarding" },
      { key: "config-packages", label: "配置包中心", path: "/config/packages" },
      { key: "pathway-templates", label: "路径配置", path: "/pathway/templates" },
      { key: "rule-definitions", label: "规则库", path: "/rule/definitions" },
      { key: "terminology-mapping", label: "字典映射", path: "/terminology/mapping" },
      { key: "adapter-hub", label: "适配器中心", path: "/adapter/hub" },
    ],
  },
  {
    key: "clinical-run",
    label: "临床运行",
    items: [
      { key: "mpi", label: "患者主索引", path: "/mpi" },
      { key: "patient-pathways", label: "患者路径", path: "/pathway/patients" },
      { key: "cdss-fatigue", label: "临床提醒治理", path: "/cdss/fatigue" },
      { key: "rule-validate", label: "规则校验", path: "/rule/validate" },
      { key: "workflow-todos", label: "待办中心", path: "/workflow/todos" },
      { key: "notifications", label: "通知中心", path: "/notifications" },
    ],
  },
  {
    key: "quality-improve",
    label: "质控改进",
    items: [
      { key: "qc-dashboard", label: "院级质控驾驶舱", path: "/qc/dashboard" },
      { key: "qc-alerts", label: "质控预警", path: "/qc/alerts" },
      { key: "insurance-audit", label: "医保智能审核", path: "/qc/insurance" },
      { key: "qc-eval-sets", label: "评估指标库", path: "/qc/eval/sets" },
      { key: "qc-eval-results", label: "评估结果", path: "/qc/eval/results" },
      { key: "aik-review", label: "AI 知识审核", path: "/aik/review" },
    ],
  },
  {
    key: "compliance-ops",
    label: "合规运维",
    items: [
      { key: "admin-users", label: "用户管理", path: "/admin/users" },
      { key: "identity-bindings", label: "身份绑定", path: "/security/identity-binding" },
      { key: "admin-audit", label: "审计日志", path: "/admin/audit" },
      { key: "security-baseline", label: "安全基线", path: "/security/baseline" },
      { key: "system-providers", label: "Provider 状态", path: "/system/providers" },
      { key: "notification-settings", label: "通知设置", path: "/notifications/settings" },
    ],
  },
  {
    key: "advanced-tools",
    label: "高级工具",
    hidden: true,
    items: [
      { key: "provenance", label: "来源追溯", path: "/advanced/provenance" },
      { key: "graph-explore", label: "图谱查询", path: "/advanced/graph" },
      { key: "ai-workflows", label: "AI 工作流", path: "/advanced/ai-workflows" },
      { key: "domestic-check", label: "国产化自检", path: "/advanced/domestic" },
      { key: "dev-console", label: "开发者控制台", path: "/advanced/dev-console" },
    ],
  },
];
