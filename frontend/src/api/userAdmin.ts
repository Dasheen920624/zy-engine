import { get, post } from "./client";
import { http } from "./client";

/**
 * 用户管理后台 API 契约（PR-FINAL-08 /admin/users）。
 *
 * 端点：
 *   GET  /api/admin/users                — 分页列表
 *   GET  /api/admin/users/{id}           — 用户详情
 *   POST /api/admin/users/{id}/status    — 启用 / 禁用
 *   POST /api/admin/users/{id}/unlock    — 解锁
 *   POST /api/admin/users/{id}/roles     — 分配角色（幂等替换）
 *   POST /api/admin/users/{id}/reset-password — 重置密码
 *   POST /api/admin/users/import         — CSV 批量导入（multipart）
 *   GET  /api/admin/roles                — 可分配角色列表
 */

// ─── 类型定义 ──────────────────────────────────────────────────────────────────

export type UserStatus = "ACTIVE" | "DISABLED";

/** 用户列表行 */
export interface AdminUser {
  id: number;
  tenant_id: number;
  username: string;
  display_name: string;
  email: string | null;
  phone: string | null;
  avatar_url: string | null;
  status: UserStatus | "LOCKED";
  user_type: string;
  employee_id: string | null;
  last_login_time: string | null;
  last_login_ip: string | null;
  /** 登录失败次数（国情合规：展示锁定状态用）*/
  login_attempts: number;
  /** 锁定截止时间（非 null 说明当前被锁）*/
  locked_until: string | null;
  roles: string[];
}

/** 身份绑定（用户详情中展示） */
export interface IdentityBinding {
  provider_id: number;
  external_subject: string;
  external_display_name: string | null;
  external_org_code: string | null;
  binding_status: string;
  last_verified_time: string | null;
}

/** 用户详情（含身份绑定） */
export interface AdminUserDetail extends AdminUser {
  identity_bindings: IdentityBinding[];
  org_scopes: Array<{ scope_level: string; scope_code: string; scope_name: string }>;
}

/** 分页结果 */
export interface AdminUserPage {
  items: AdminUser[];
  total: number;
  page: number;
  page_size: number;
  total_pages: number;
}

/** 可分配角色 */
export interface RoleOption {
  role_code: string;
  role_name: string;
  role_type: string;
}

/** 列表筛选条件 */
export interface UserListFilters {
  keyword?: string;
  status?: string;
  role?: string;
  page?: number;
  size?: number;
}

/** CSV 导入结果 */
export interface ImportResult {
  created: number;
  skipped: number;
  errors: string[];
}

// ─── API 函数 ──────────────────────────────────────────────────────────────────

/** 分页查询用户列表 */
export async function listUsers(filters: UserListFilters = {}): Promise<AdminUserPage> {
  const params: Record<string, string | number> = {};
  if (filters.keyword) params.keyword = filters.keyword;
  if (filters.status) params.status = filters.status;
  if (filters.role) params.role = filters.role;
  params.page = filters.page ?? 1;
  params.size = filters.size ?? 20;
  return get<AdminUserPage>("/admin/users", { params });
}

/** 查询用户详情 */
export async function getUserDetail(id: number): Promise<AdminUserDetail> {
  return get<AdminUserDetail>(`/admin/users/${id}`);
}

/** 更新用户状态 */
export async function updateUserStatus(id: number, status: UserStatus): Promise<void> {
  return post<void>(`/admin/users/${id}/status`, { status });
}

/** 解锁用户 */
export async function unlockUser(id: number): Promise<void> {
  return post<void>(`/admin/users/${id}/unlock`);
}

/** 分配用户角色（幂等替换） */
export async function assignRoles(id: number, roleCodes: string[]): Promise<void> {
  return post<void>(`/admin/users/${id}/roles`, { role_codes: roleCodes });
}

/** 重置用户密码 */
export async function resetPassword(id: number, newPassword: string): Promise<void> {
  return post<void>(`/admin/users/${id}/reset-password`, { new_password: newPassword });
}

/** 查询可分配角色列表 */
export async function listRoles(): Promise<RoleOption[]> {
  return get<RoleOption[]>("/admin/roles");
}

/** CSV 批量导入 */
export async function importUsers(file: File): Promise<ImportResult> {
  const form = new FormData();
  form.append("file", file);
  const resp = await http.post<{ data: ImportResult }>("/admin/users/import", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return resp.data.data;
}

// ─── 显示辅助 ──────────────────────────────────────────────────────────────────

export const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "正常",
  DISABLED: "禁用",
  LOCKED: "已锁定",
};

export const STATUS_COLORS: Record<string, string> = {
  ACTIVE: "success",
  DISABLED: "default",
  LOCKED: "error",
};

export const USER_TYPE_LABELS: Record<string, string> = {
  STAFF: "医院员工",
  ADMIN: "平台管理员",
  EXTERNAL: "外部用户",
};

export const ROLE_TYPE_LABELS: Record<string, string> = {
  SYSTEM: "系统角色",
  PLATFORM: "平台角色",
  HOSPITAL: "院级角色",
  DEPARTMENT: "科室角色",
};
