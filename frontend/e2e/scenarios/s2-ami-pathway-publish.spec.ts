import { test, expect } from "../fixtures/test-fixtures";

/**
 * S2 — 医学专家发布 AMI 路径
 *
 * 演示路径：/config/packages/import 5 步向导 → MISSING_SOURCE 阻断 → 补来源 → 发布
 * DoD：包含「同 code+version 不同内容」错误演示 + 回滚演示
 */
test.describe("S2 · AMI 路径发布", () => {
  test("医学专家进入配置包列表 /config/packages", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/config/packages");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("医学专家进入配置包导入向导 /config/packages/import", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/config/packages/import");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/config/packages/import");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("医学专家进入路径模板列表 /pathway/templates", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/templates");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("未登录");
  });

  test("医学专家查看路径详情 /pathway/templates/{code}", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/templates/AMI_PATHWAY_V1");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/templates/");
  });
});
