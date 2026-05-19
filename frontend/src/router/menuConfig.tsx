import type { ReactNode } from "react";
import {
  AppstoreOutlined,
  AuditOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  MedicineBoxOutlined,
  NodeIndexOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
  ShareAltOutlined,
  ToolOutlined,
  UnorderedListOutlined,
} from "@ant-design/icons";

export interface MenuItem {
  key: string;
  label: string;
  icon?: ReactNode;
  path?: string;
  children?: MenuItem[];
  roles?: string[]; // 允许访问的角色，为空则所有角色可见
  disabled?: boolean;
  pr?: string; // 关联的 PR 编号，用于占位页显示
}

/**
 * 顶级菜单配置（11 项）
 * 按 04_页面规格书.md §0.2 的 18 页面清单生成
 */
export const topMenuItems: MenuItem[] = [
  {
    key: "dashboard",
    label: "工作台",
    icon: <AppstoreOutlined />,
    path: "/dashboard",
  },
  {
    key: "pathway",
    label: "路径",
    icon: <NodeIndexOutlined />,
    children: [
      {
        key: "pathway-templates",
        label: "路径模板列表",
        path: "/pathway/templates",
        pr: "PR-V2-06",
      },
      {
        key: "pathway-editor",
        label: "路径模板编辑器",
        path: "/pathway/templates/:code/edit",
        pr: "PR-V2-07",
        disabled: true,
      },
      {
        key: "pathway-diff",
        label: "路径版本对比",
        path: "/pathway/templates/:code/diff",
        pr: "PR-V2-07",
        disabled: true,
      },
      {
        key: "pathway-patients",
        label: "患者路径管理",
        path: "/pathway/patients",
        pr: "PR-V2-09",
      },
    ],
  },
  {
    key: "rule",
    label: "规则",
    icon: <SafetyCertificateOutlined />,
    children: [
      {
        key: "rule-definitions",
        label: "规则库",
        path: "/rule/definitions",
        pr: "PR-V2-05",
      },
      {
        key: "rule-editor",
        label: "规则 DSL 编辑器",
        path: "/rule/definitions/:code/edit",
        pr: "PR-V2-05",
        disabled: true,
      },
      {
        key: "rule-validate",
        label: "规则校验工作台",
        path: "/rule/validate",
      },
    ],
  },
  {
    key: "graph",
    label: "图谱",
    icon: <ShareAltOutlined />,
    children: [
      {
        key: "graph-explore",
        label: "图谱查询工作台",
        path: "/graph/explore",
        pr: "PR-V2-05",
      },
    ],
  },
  {
    key: "terminology",
    label: "字典",
    icon: <MedicineBoxOutlined />,
    children: [
      {
        key: "terminology-mapping",
        label: "字典映射工作台",
        path: "/terminology/mapping",
        pr: "PR-V2-08",
      },
    ],
  },
  {
    key: "config",
    label: "配置",
    icon: <FileSearchOutlined />,
    children: [
      {
        key: "config-packages",
        label: "配置包列表",
        path: "/config/packages",
      },
      {
        key: "config-import",
        label: "配置包发布向导",
        path: "/config/packages/import",
        pr: "PR-V2-05",
      },
    ],
  },
  {
    key: "qc",
    label: "质控",
    icon: <AuditOutlined />,
    children: [
      {
        key: "qc-alerts",
        label: "质控预警列表",
        path: "/qc/alerts",
        pr: "PR-V2-11",
      },
      {
        key: "qc-dashboard",
        label: "院级质控驾驶舱",
        path: "/qc/dashboard",
        pr: "PR-V2-12",
      },
      {
        key: "qc-insurance",
        label: "医保智能审核",
        path: "/qc/insurance",
        pr: "PR-V2-12",
      },
    ],
  },
  {
    key: "aik",
    label: "知识",
    icon: <ReadOutlined />,
    children: [
      {
        key: "aik-review",
        label: "知识审核台",
        path: "/aik/review",
        pr: "PR-V2-05",
      },
    ],
  },
  {
    key: "system",
    label: "系统",
    icon: <ToolOutlined />,
    children: [
      {
        key: "admin-users",
        label: "用户管理",
        path: "/admin/users",
        pr: "PR-V2-04",
      },
      {
        key: "admin-audit",
        label: "审计日志",
        path: "/admin/audit",
        pr: "PR-V2-04",
      },
      {
        key: "system-providers",
        label: "Provider 状态",
        path: "/system/providers",
      },
    ],
  },
  {
    key: "workflow",
    label: "待办",
    icon: <UnorderedListOutlined />,
    children: [
      {
        key: "workflow-todos",
        label: "待办中心",
        path: "/workflow/todos",
      },
    ],
  },
  {
    key: "demo",
    label: "演示",
    icon: <ExperimentOutlined />,
    path: "/demo-validation",
  },
];

/**
 * 根据当前路径获取顶级菜单 key
 */
export function getTopMenuKeyByPath(pathname: string): string {
  for (const item of topMenuItems) {
    if (item.path && pathname.startsWith(item.path)) {
      return item.key;
    }
    if (item.children) {
      for (const child of item.children) {
        if (child.path && pathname.startsWith(child.path)) {
          return item.key;
        }
      }
    }
  }
  return "dashboard";
}

/**
 * 获取指定顶级菜单的子菜单
 */
export function getSideMenuItems(topKey: string): MenuItem[] {
  const topItem = topMenuItems.find((item) => item.key === topKey);
  return topItem?.children || [];
}

/**
 * 所有路由路径列表（用于注册路由）
 */
export function getAllRoutes(): { path: string; pr?: string }[] {
  const routes: { path: string; pr?: string }[] = [];
  for (const item of topMenuItems) {
    if (item.path) {
      routes.push({ path: item.path, pr: item.pr });
    }
    if (item.children) {
      for (const child of item.children) {
        if (child.path) {
          routes.push({ path: child.path, pr: child.pr });
        }
      }
    }
  }
  return routes;
}
