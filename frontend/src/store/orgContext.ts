import type { OrgContext } from "../api/types";

// 简易组织上下文 store（无第三方依赖）。
// 用 module-level 变量 + 订阅者，避免引入 zustand/redux。
// 真正生产中可换成 zustand，本脚手架先保持轻量。

const STORAGE_KEY = "medkernel.orgContext";

function loadInitial(): OrgContext {
  const env = (import.meta.env as Record<string, string>) || {};
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as OrgContext;
  } catch {
    /* localStorage 可能在隐私模式不可用，忽略 */
  }
  const tenantId = env.VITE_DEFAULT_TENANT_ID;
  if (!tenantId) {
    // 兜底使用演示租户但必须告警：生产部署如未设置 VITE_DEFAULT_TENANT_ID，
    // 所有请求会落到 TENANT_DEMO，数据会被错误归类。
    console.warn(
      "[orgContext] VITE_DEFAULT_TENANT_ID is not set; falling back to 'TENANT_DEMO'. " +
        "Production deployments MUST set this env to avoid cross-tenant data contamination.",
    );
  }
  return {
    tenant_id: tenantId || "TENANT_DEMO",
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
    // eslint-disable-next-line no-restricted-syntax -- 仅保存组织上下文，不包含 token、API Key 或患者隐私。
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
