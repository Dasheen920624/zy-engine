import { OrgContext } from "../api/types";

// 简易组织上下文 store（无第三方依赖）。
// 用 module-level 变量 + 订阅者，避免引入 zustand/redux。
// 真正生产中可换成 zustand，本脚手架先保持轻量。

const STORAGE_KEY = "zy-engine.orgContext";

function loadInitial(): OrgContext {
  const env = (import.meta.env as Record<string, string>) || {};
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as OrgContext;
  } catch {
    /* localStorage 可能在隐私模式不可用，忽略 */
  }
  return {
    tenant_id: env.VITE_DEFAULT_TENANT_ID || "TENANT_DEMO",
    group_code: env.VITE_DEFAULT_GROUP_CODE || undefined,
    hospital_code: env.VITE_DEFAULT_HOSPITAL_CODE || undefined,
  };
}

let current: OrgContext = loadInitial();
const listeners = new Set<(ctx: OrgContext) => void>();

export function getOrgContext(): OrgContext {
  return current;
}

export function setOrgContext(next: OrgContext): void {
  current = { ...next };
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(current));
  } catch {
    /* ignore */
  }
  listeners.forEach((l) => l(current));
}

export function subscribeOrgContext(listener: (ctx: OrgContext) => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}
