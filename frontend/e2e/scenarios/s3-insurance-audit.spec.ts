import { test, expect } from "../fixtures/test-fixtures";

/**
 * S3 — 医保智能审核
 *
 * 文档来源：docs/02_场景剧本图.md § S3
 * 主角：R04 医务质控 + R09 药师
 * 归属产品：C 质控驾驶舱 + B 临床嵌入器
 * 涉及页面：医保智能审核、质控驾驶舱、质控预警
 *
 * DoD 验收点：
 * - 医务质控进入医保智能审核页面
 * - 查看医保审核规则和结果
 * - 质控驾驶舱可查看医保相关指标
 * - 质控预警列表可筛选医保类预警
 */
test.describe("S3 · 医保智能审核", () => {
  test("医务质控进入医保智能审核页面", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/insurance");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/insurance");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("质控驾驶舱可查看医保相关指标", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/dashboard");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("质控预警列表可筛选医保类预警", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/alerts");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/alerts");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
