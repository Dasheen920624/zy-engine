import type { UserInfo } from "../api/types";

/**
 * 认证状态管理（轻量级，无第三方依赖）。
 * 与 orgContext.ts 模式一致：module-level 变量 + 订阅者。
 */

const TOKEN_KEY = "medkernel.auth.token";
const USER_KEY = "medkernel.auth.user";

let currentToken: string | null = null;
let currentUser: UserInfo | null = null;
const listeners = new Set<() => void>();

function loadInitial(): void {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const userRaw = localStorage.getItem(USER_KEY);
    if (token) {
      currentToken = token;
    }
    if (userRaw) {
      currentUser = JSON.parse(userRaw) as UserInfo;
    }
  } catch {
    // ignore
  }
}

// 初始化
loadInitial();

export function getToken(): string | null {
  return currentToken;
}

export function getUser(): UserInfo | null {
  return currentUser;
}

export function isAuthenticated(): boolean {
  return currentToken != null && currentToken.length > 0;
}

export function setAuth(token: string, user: UserInfo): void {
  currentToken = token;
  currentUser = user;
  try {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch {
    // ignore
  }
  notify();
}

export function clearAuth(): void {
  currentToken = null;
  currentUser = null;
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  } catch {
    // ignore
  }
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
