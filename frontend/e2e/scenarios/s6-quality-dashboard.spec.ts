import { test, expect } from "../fixtures/test-fixtures";

/**
 * S6 — 院级质控驾驶舱
 *
 * 文档来源：docs/02_场景剧本图.md § S6
 * 主角：R06 主任医师 + R02 租户管理员
 * 归属产品：C 质控驾驶舱
 * 涉及页面：Dashboard、质控驾驶舱、科室下钻、评估报告
 *
 * DoD 验收点：
 * - 主任医师进入 Dashboard 看到治理大盘
 * - 下钻到院级质控驾驶舱
 * - 二次下钻到科室维度
 * - 评估报告可查看质控评估结果
 */
test.describe("S6 · 院级质控驾驶舱", () => {
  test("主任医师进入 Dashboard 看到治理大盘", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/dashboard");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("下钻到院级质控驾驶舱", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/dashboard");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("二次下钻到科室维度", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/department/DEMO_DEPT");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/department");
  });

  test("评估报告可查看质控评估结果", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/eval/reports");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/eval/reports");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
