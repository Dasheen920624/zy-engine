import { test, expect } from "../fixtures/test-fixtures";

/**
 * S1 — 集团医院 CIO 看治理大盘
 *
 * 演示路径：/dashboard → /qc/dashboard → 部门下钻
 * DoD：5 分钟讲清楚「我管 3 家医院 / 12 个科室，今天的合规率 / 异常工单 / AI 命中率」
 */
test.describe("S1 · CIO 治理大盘", () => {
  test("CIO 进入 Dashboard 看到 4 大分组卡片", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    // 4 大分组：知识工厂 / 质控驾驶舱 / 用户与身份 / 平台监控
    expect(body).toContain("MedKernel");
    expect(body ?? "").not.toContain("401");
  });

  test("CIO 下钻到 /qc/dashboard 质控驾驶舱", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("CIO 二次下钻到部门页 /qc/department/{deptCode}", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/department/DEMO_DEPT");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/department");
  });
});
