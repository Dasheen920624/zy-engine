import { test, expect } from "../fixtures/test-fixtures";

/**
 * S4 — CDSS 提醒疲劳治理
 *
 * 演示路径：/cdss/fatigue → 统计 → 静默规则
 * DoD：演示「医生覆盖率 > 80% 自动静默 7 天」
 */
test.describe("S4 · CDSS 提醒疲劳治理", () => {
  test("管理员进入 CDSS 疲劳治理页 /cdss/fatigue", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/cdss/fatigue");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("管理员查看告警列表 /qc/alerts", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/alerts");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("CDSS 疲劳治理页 URL 不重定向到登录", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/cdss/fatigue");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).not.toContain("/login");
  });
});
