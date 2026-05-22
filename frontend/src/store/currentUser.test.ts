import { describe, it, expect, beforeEach } from "vitest";
import { getCurrentUser, getCurrentRole, switchMockRole, getAvailableRoles, subscribeCurrentUser } from "../currentUser";

describe("currentUser store", () => {
  beforeEach(() => {
    switchMockRole("R07");
  });

  it("getCurrentUser 应返回当前 mock 用户", () => {
    const user = getCurrentUser();
    expect(user).toBeDefined();
    expect(user?.username).toBe("doctor");
  });

  it("getCurrentRole 应返回当前角色", () => {
    expect(getCurrentRole()).toBe("R07");
  });

  it("switchMockRole 应切换角色", () => {
    switchMockRole("R02");
    expect(getCurrentRole()).toBe("R02");
    const user = getCurrentUser();
    expect(user?.username).toBe("admin");
  });

  it("switchMockRole 对无效角色不应切换", () => {
    switchMockRole("INVALID_ROLE");
    expect(getCurrentRole()).toBe("R07");
  });

  it("getAvailableRoles 应返回所有可用角色", () => {
    const roles = getAvailableRoles();
    expect(roles.length).toBeGreaterThan(0);
    expect(roles).toContain("R02");
    expect(roles).toContain("R07");
  });

  it("subscribeCurrentUser 应在角色切换时通知", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeCurrentUser(listener);
    switchMockRole("R02");
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });
});
