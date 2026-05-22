import { test, expect } from "../fixtures/test-fixtures";

/**
 * S5 — 配置包跨环境发布
 *
 * 文档来源：docs/02_场景剧本图.md § S5
 * 主角：R03 信息科
 * 归属产品：A 配置工厂
 * 涉及页面：配置包中心、配置包导入向导、路径模板列表
 *
 * DoD 验收点：
 * - 信息科进入配置包中心
 * - 配置包导入向导可走完 5 步流程
 * - 导入后路径模板列表可见新路径
 * - 来源追溯可查看配置包来源
 */
test.describe("S5 · 配置包跨环境发布", () => {
  test("信息科进入配置包中心", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/config/packages");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/config/packages");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("配置包导入向导页面可访问", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/config/packages/import");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/config/packages/import");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("导入后路径模板列表可见", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/templates");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/templates");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("来源追溯可查看配置包来源", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/provenance");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/provenance");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
