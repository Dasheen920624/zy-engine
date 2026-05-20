import { http } from './client';

export interface IdentityBinding {
  id: number;
  tenantId: number;
  userId: number;
  providerId: number;
  externalSubject: string;
  externalOrgCode: string;
  externalDisplayName: string;
  bindingStatus: string;
  lastVerifiedTime: string | null;
  createdBy: string;
  createdTime: string;
}

export interface BindingConflict {
  providerId: number;
  externalSubject: string;
  userCount: number;
}

export interface MergeResult {
  transferredCount: number;
  conflictCount: number;
  sourceUserId: number;
  targetUserId: number;
}

/** 查询用户的身份绑定列表 */
export async function listBindingsByUser(userId: number): Promise<IdentityBinding[]> {
  const res = await http.get(`/security/bindings/users/${userId}`);
  return res.data?.data ?? [];
}

/** 绑定外部身份 */
export async function bindIdentity(params: {
  userId: number;
  providerId: number;
  externalSubject: string;
  externalDisplayName?: string;
}): Promise<IdentityBinding> {
  const res = await http.post('/security/bindings/bind', params);
  return res.data?.data;
}

/** 解绑 */
export async function unbindIdentity(bindingId: number): Promise<void> {
  await http.delete(`/security/bindings/${bindingId}`);
}

/** 合并绑定 */
export async function mergeBindings(params: {
  sourceUserId: number;
  targetUserId: number;
}): Promise<MergeResult> {
  const res = await http.post('/security/bindings/merge', params);
  return res.data?.data;
}

/** 查找冲突绑定 */
export async function findConflicts(): Promise<BindingConflict[]> {
  const res = await http.get('/security/bindings/conflicts');
  return res.data?.data ?? [];
}
