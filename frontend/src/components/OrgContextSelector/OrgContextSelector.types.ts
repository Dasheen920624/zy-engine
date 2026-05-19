/**
 * OrgContextSelector 内部用的 view-only 类型。
 *
 * 与 {@link ../../api/types.ts} 中的 `OrgContext`（snake_case，与后端契约对齐）**不同**：
 * - 这里采用 camelCase 是为了在 Cascader options/label 渲染时贴近 React 习惯；
 * - 必填字段 `tenantId` + `hospitalCode` 反映"组件渲染前父组件必须确定的最小组织上下文"。
 *
 * 跨边界写入（即用户切换组织后通知请求拦截器）由 {@link ./OrgContextSelector.tsx} 内的
 * 适配函数 `toStoreShape` 转回 snake_case 并 `setOrgContext()` 到全局 store，避免出现
 * "切换 UI 上看到了，但请求 Header 没变"的隐蔽 Bug（AUDIT §3.2）。
 *
 * 待 `api/types.ts` 的 OrgContext 可写时，应直接复用并删除本接口；当前 PR-V2-06/PR-V2-11
 * 持有 api/types.ts 的 write_scope，本文件仅做本地 view 类型。
 */
export interface OrgContext {
  tenantId: string;
  groupCode?: string;
  hospitalCode: string;
  campusCode?: string;
  siteCode?: string;
  departmentCode?: string;
}

export interface OrgContextSelectorProps {
  current: OrgContext;
  allowedScopes: OrgContext[];
  onChange: (next: OrgContext) => void;
  level?: 'hospital' | 'department';
  variant?: 'inline' | 'dropdown';
}
