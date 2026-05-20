import { http } from "./client";
import type { ApiResult, UserInfo } from "./types";

export interface SsoProvider {
  id: number;
  providerCode: string;
  providerName: string;
  providerType: string;
  status: string;
  adapterCode: string;
}

export interface SsoInitiateResult {
  providerId: number;
  providerType: string;
  redirectUrl: string;
  state: string;
}

export interface SsoCallbackResult {
  token: string;
  user: UserInfo;
}

/** 获取可用的 SSO 身份源列表 */
export async function listSsoProviders(): Promise<SsoProvider[]> {
  const resp = await http.get<ApiResult<SsoProvider[]>>("/security/sso/providers");
  return resp.data.data ?? [];
}

/** 发起 SSO 登录 */
export async function initiateSso(providerId: number): Promise<SsoInitiateResult> {
  const resp = await http.post<ApiResult<SsoInitiateResult>>(`/security/sso/providers/${providerId}/initiate`);
  return resp.data.data as SsoInitiateResult;
}

/** 处理 SSO 回调（CAS/OIDC） */
export async function handleSsoCallback(
  providerId: number,
  code: string,
  state?: string
): Promise<SsoCallbackResult> {
  const resp = await http.get<ApiResult<SsoCallbackResult>>("/security/sso/callback", {
    params: { providerId, code, state },
  });
  return resp.data.data as SsoCallbackResult;
}

/** LDAP 认证 */
export async function ldapAuthenticate(
  providerId: number,
  username: string,
  password: string
): Promise<SsoCallbackResult> {
  const resp = await http.post<ApiResult<SsoCallbackResult>>("/security/sso/ldap/authenticate", {
    providerId,
    username,
    password,
  });
  return resp.data.data as SsoCallbackResult;
}
