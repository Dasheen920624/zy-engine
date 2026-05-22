import { test, expect } from "../fixtures/test-fixtures";

/**
 * S5 — AI 知识审核闭环
 *
 * 演示路径：/aik/sources → Dify 任务 → AI 候选 → 人工审核 → 知识包
 * DoD：完整 4 步带「来源溯源」
 */
test.describe("S5 · AI 知识审核闭环", () => {
  test("知识管理员进入知识来源页 /aik/sources", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/aik/sources");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("知识管理员进入 AI 候选审核台 /aik/review", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/aik/review");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("知识管理员查看 AI 工作流引擎 /ai-workflows", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/ai-workflows");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("知识溯源页 /provenance 可访问", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/provenance");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
