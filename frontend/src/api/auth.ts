import { http } from "./client";
import type { ApiResult, UserInfo } from "./types";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserInfo;
}

/**
 * 用户登录。
 */
export async function login(req: LoginRequest): Promise<LoginResponse> {
  const resp = await http.post<ApiResult<LoginResponse>>("/auth/login", req);
  return resp.data.data as LoginResponse;
}

/**
 * 获取当前登录用户信息。
 */
export async function fetchCurrentUser(): Promise<UserInfo> {
  const resp = await http.get<ApiResult<UserInfo>>("/auth/me");
  return resp.data.data as UserInfo;
}

/**
 * 用户登出。
 */
export async function logout(): Promise<void> {
  await http.post<ApiResult<void>>("/auth/logout");
}
