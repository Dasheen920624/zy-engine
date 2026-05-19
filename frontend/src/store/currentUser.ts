import type { UserInfo } from "../api/types";

/**
 * 当前用户状态管理
 * SEC-001 未完成前，使用 mock 数据
 */

// Mock 用户数据（开发模式）
const mockUsers: Record<string, UserInfo> = {
  R02: {
    id: "1",
    tenantId: "1",
    username: "admin",
    displayName: "租户管理员",
    email: "admin@medkernel.com",
    phone: "13800138001",
    status: "ACTIVE",
    roles: ["R02"],
    permissions: ["*"],
    orgScopes: [],
  },
  R03: {
    id: "2",
    tenantId: "1",
    username: "info_admin",
    displayName: "信息科管理员",
    email: "info@medkernel.com",
    phone: "13800138002",
    status: "ACTIVE",
    roles: ["R03"],
    permissions: ["system:*", "config:*", "audit:*"],
    orgScopes: [],
  },
  R04: {
    id: "3",
    tenantId: "1",
    username: "qc_admin",
    displayName: "质控管理员",
    email: "qc@medkernel.com",
    phone: "13800138003",
    status: "ACTIVE",
    roles: ["R04"],
    permissions: ["qc:*", "audit:read"],
    orgScopes: [],
  },
  R05: {
    id: "4",
    tenantId: "1",
    username: "pathway_expert",
    displayName: "路径专家",
    email: "pathway@medkernel.com",
    phone: "13800138004",
    status: "ACTIVE",
    roles: ["R05"],
    permissions: ["pathway:*", "rule:read", "graph:read"],
    orgScopes: [],
  },
  R06: {
    id: "5",
    tenantId: "1",
    username: "director",
    displayName: "科室主任",
    email: "director@medkernel.com",
    phone: "13800138005",
    status: "ACTIVE",
    roles: ["R06"],
    permissions: ["pathway:read", "pathway:review", "rule:read", "qc:read"],
    orgScopes: [],
  },
  R07: {
    id: "6",
    tenantId: "1",
    username: "doctor",
    displayName: "主治医师",
    email: "doctor@medkernel.com",
    phone: "13800138006",
    status: "ACTIVE",
    roles: ["R07"],
    permissions: ["pathway:read", "rule:read", "qc:read"],
    orgScopes: [],
  },
  R08: {
    id: "7",
    tenantId: "1",
    username: "nurse",
    displayName: "护士长",
    email: "nurse@medkernel.com",
    phone: "13800138007",
    status: "ACTIVE",
    roles: ["R08"],
    permissions: ["pathway:read", "qc:read"],
    orgScopes: [],
  },
  R09: {
    id: "8",
    tenantId: "1",
    username: "pharmacist",
    displayName: "药师",
    email: "pharmacist@medkernel.com",
    phone: "13800138008",
    status: "ACTIVE",
    roles: ["R09"],
    permissions: ["rule:read", "qc:read"],
    orgScopes: [],
  },
};

let currentMockRole: string = "R07"; // 默认角色：主治医师
const listeners = new Set<() => void>();

/**
 * 获取当前 mock 用户
 */
export function getCurrentUser(): UserInfo | null {
  return mockUsers[currentMockRole] || null;
}

/**
 * 获取当前 mock 角色
 */
export function getCurrentRole(): string {
  return currentMockRole;
}

/**
 * 切换 mock 角色（开发模式使用）
 */
export function switchMockRole(role: string): void {
  if (mockUsers[role]) {
    currentMockRole = role;
    notify();
  }
}

/**
 * 获取所有可用的 mock 角色
 */
export function getAvailableRoles(): string[] {
  return Object.keys(mockUsers);
}

/**
 * 订阅用户变更
 */
export function subscribeCurrentUser(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function notify(): void {
  listeners.forEach((l) => l());
}
