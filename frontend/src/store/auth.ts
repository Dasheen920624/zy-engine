import type { UserInfo } from "../api/types";

/**
 * 认证状态管理（轻量级，无第三方依赖）。
 * token 只保存在内存里，避免把登录凭据写入 localStorage。
 */

let currentToken: string | null = null;
let currentUser: UserInfo | null = null;
const listeners = new Set<() => void>();

export function getToken(): string | null {
  return currentToken;
}

export function getUser(): UserInfo | null {
  return currentUser;
}

export function isAuthenticated(): boolean {
  return currentToken !== null && currentToken.length > 0;
}

export function setAuth(token: string, user: UserInfo): void {
  currentToken = token;
  currentUser = user;
  notify();
}

export function clearAuth(): void {
  currentToken = null;
  currentUser = null;
  notify();
}

export function subscribeAuth(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function notify(): void {
  listeners.forEach((l) => l());
}
