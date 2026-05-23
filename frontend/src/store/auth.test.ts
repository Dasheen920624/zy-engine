import { describe, it, expect, beforeEach } from "vitest";
import { getToken, getUser, isAuthenticated, setAuth, clearAuth, subscribeAuth } from "./auth";
import type { UserInfo } from "../api/types";

const mockUser: UserInfo = {
  id: 1,
  tenant_id: 1,
  username: "testuser",
  display_name: "测试用户",
  status: "ACTIVE",
  roles: ["R07"],
  permissions: ["pathway:read"],
  org_scopes: [],
};

describe("auth store", () => {
  beforeEach(() => {
    clearAuth();
  });

  it("初始状态应未认证", () => {
    expect(isAuthenticated()).toBe(false);
    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
  });

  it("setAuth 应设置 token 和用户", () => {
    setAuth("test-token-123", mockUser);
    expect(isAuthenticated()).toBe(true);
    expect(getToken()).toBe("test-token-123");
    expect(getUser()).toEqual(mockUser);
  });

  it("clearAuth 应清除 token 和用户", () => {
    setAuth("test-token-123", mockUser);
    clearAuth();
    expect(isAuthenticated()).toBe(false);
    expect(getToken()).toBeNull();
    expect(getUser()).toBeNull();
  });

  it("subscribeAuth 应在状态变更时通知", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeAuth(listener);
    setAuth("test-token-123", mockUser);
    expect(listener).toHaveBeenCalledTimes(1);
    clearAuth();
    expect(listener).toHaveBeenCalledTimes(2);
    unsubscribe();
  });

  it("unsubscribe 后不应再收到通知", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeAuth(listener);
    unsubscribe();
    setAuth("test-token-123", mockUser);
    expect(listener).not.toHaveBeenCalled();
  });
});
