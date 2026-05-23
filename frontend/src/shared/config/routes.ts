/**
 * 路由元数据（single source of truth）。
 * App.tsx + router.tsx + 菜单 + 面包屑 + 权限元数据全部读这里。
 */
export interface RouteMeta {
  path: string;
  title: string;
  breadcrumb?: string[];
  requireAuth?: boolean;
  roles?: string[];
}

export const routes: RouteMeta[] = [
  { path: "/login", title: "登录", requireAuth: false },
  { path: "/dashboard", title: "工作台", requireAuth: true },
];
