import { get, post } from "./client";

// ==================== 类型定义 ====================

export interface SsoLoginResult {
  login_url: string;
  state: string;
  provider_code: string;
  protocol_type: string;
}

export interface SsoCallbackResult {
  success: boolean;
  token?: string;
  session_id?: string;
  platform_user_id?: number;
  external_subject?: string;
  login_method?: string;
  error?: string;
}

export interface SsoProviderList {
  protocols: string[];
  tenant_id: string;
}

// ==================== SSO API ====================

export async function initiateSsoLogin(provider: string, callbackBaseUrl?: string): Promise<SsoLoginResult> {
  const qs = new URLSearchParams();
  qs.set("provider", provider);
  if (callbackBaseUrl) qs.set("callback_base_url", callbackBaseUrl);
  return get<SsoLoginResult>(`/auth/sso/login?${qs.toString()}`);
}

export async function listSsoProviders(): Promise<SsoProviderList> {
  return get<SsoProviderList>("/auth/sso/providers");
}

export async function ldapDirectLogin(params: {
  provider: string;
  username: string;
  password: string;
}): Promise<SsoCallbackResult> {
  return post<SsoCallbackResult>("/auth/sso/ldap", params);
}

export async function ssoLogout(sessionId: string): Promise<boolean> {
  const result = await post<boolean>("/auth/sso/logout", { session_id: sessionId });
  return result;
}
