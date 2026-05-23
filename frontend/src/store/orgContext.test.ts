import { describe, it, expect, beforeEach } from "vitest";
import { getOrgContext, setOrgContext, subscribeOrgContext } from "./orgContext";
import type { OrgContext } from "../api/types";

describe("orgContext store", () => {
  beforeEach(() => {
    // 重置到默认状态
    setOrgContext({ tenant_id: "TENANT_DEMO" });
  });

  it("getOrgContext 应返回当前组织上下文", () => {
    const ctx = getOrgContext();
    expect(ctx).toBeDefined();
    expect(ctx.tenant_id).toBeTruthy();
  });

  it("setOrgContext 应更新组织上下文", () => {
    const newCtx: OrgContext = {
      tenant_id: "TENANT_ALPHA",
      hospital_code: "HOSPITAL_001",
    };
    setOrgContext(newCtx);
    const ctx = getOrgContext();
    expect(ctx.tenant_id).toBe("TENANT_ALPHA");
    expect(ctx.hospital_code).toBe("HOSPITAL_001");
  });

  it("subscribeOrgContext 应在上下文变更时通知", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeOrgContext(listener);
    setOrgContext({ tenant_id: "TENANT_BETA" });
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it("unsubscribe 后不应再收到通知", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeOrgContext(listener);
    unsubscribe();
    setOrgContext({ tenant_id: "TENANT_GAMMA" });
    expect(listener).not.toHaveBeenCalled();
  });
});
