import { describe, it, expect } from "vitest";
import {
  loginRuntimeConfig,
  isDemoProfile,
  resolveInitialTab,
  type LoginTabKey,
} from "../config";

describe("config", () => {
  it("loginRuntimeConfig 应包含正确的默认值", () => {
    expect(loginRuntimeConfig.cryptoSuite).toBe("SM2");
    expect(loginRuntimeConfig.lockThreshold).toBe(5);
    expect(loginRuntimeConfig.sessionTimeoutMinutes).toBe(30);
    expect(loginRuntimeConfig.icpNumber).toBeTruthy();
    expect(loginRuntimeConfig.psbNumber).toBeTruthy();
  });

  it("isDemoProfile 在 profile 为 demo 时返回 true", () => {
    // 默认 env 下 profile 为 "demo"
    expect(isDemoProfile()).toBe(true);
  });

  it("resolveInitialTab 默认返回 password", () => {
    expect(resolveInitialTab(undefined, "")).toBe("password");
  });

  it("resolveInitialTab 从 search 参数读取 method", () => {
    expect(resolveInitialTab(undefined, "?method=sso")).toBe("sso");
    expect(resolveInitialTab(undefined, "?method=password")).toBe("password");
  });

  it("resolveInitialTab 优先使用 initialTab 参数", () => {
    expect(resolveInitialTab("sso", "?method=password")).toBe("sso");
  });

  it("resolveInitialTab 对无效值回退到 password", () => {
    expect(resolveInitialTab("invalid" as LoginTabKey, "")).toBe("password");
  });
});
