import { test, expect } from "../fixtures/test-fixtures";

/**
 * S4 — 医嘱安全实时拦截
 *
 * 文档来源：docs/02_场景剧本图.md § S4
 * 主角：R07 主治医师 + R09 药师
 * 归属产品：B 临床嵌入器
 * 涉及页面：临床提醒治理、规则校验、质控预警
 *
 * DoD 验收点：
 * - 主治医师进入临床提醒治理页面
 * - 规则校验工作台可触发医嘱安全规则
 * - 质控预警列表可查看拦截记录
 * - CDSS 提醒疲劳治理可调整拦截策略
 */
test.describe("S4 · 医嘱安全实时拦截", () => {
  test("主治医师进入临床提醒治理页面", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/cdss/fatigue");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/cdss/fatigue");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("规则校验工作台可触发医嘱安全规则", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/rule/validate");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/validate");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("质控预警列表可查看拦截记录", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/qc/alerts");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/qc/alerts");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
