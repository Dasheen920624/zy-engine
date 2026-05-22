import { test, expect } from "../fixtures/test-fixtures";

/**
 * S2 — 病历内涵质控
 *
 * 文档来源：docs/02_场景剧本图.md § S2
 * 主角：R04 医务质控 + R07 主治医师
 * 归属产品：C 质控驾驶舱 + B 临床嵌入器
 * 涉及页面：质控驾驶舱、质控预警、科室下钻
 *
 * DoD 验收点：
 * - 医务质控进入院级质控驾驶舱
 * - 查看质控预警列表
 * - 下钻到科室维度查看质控详情
 * - 评估指标库可查看病历内涵指标
 */
test.describe("S2 · 病历内涵质控", () => {
  test("医务质控进入院级质控驾驶舱", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/dashboard");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/dashboard");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("查看质控预警列表", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/alerts");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/alerts");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("下钻到科室维度查看质控详情", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/department/DEMO_DEPT");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/department");
  });

  test("评估指标库可查看病历内涵指标", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/eval/sets");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/eval/sets");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
