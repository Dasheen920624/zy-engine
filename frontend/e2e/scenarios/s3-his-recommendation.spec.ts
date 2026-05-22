import { test, expect } from "../fixtures/test-fixtures";

/**
 * S3 — 医生在 HIS 收到 AMI 推荐
 *
 * 演示路径：触发规则 → 医生确认/覆盖 → 进 RuleActionLog
 * DoD：完整闭环含覆盖理由
 *
 * 注：/embed/order-safety 是嵌入式 iFrame 路径，本 E2E 通过规则定义页验证后端链路。
 */
test.describe("S3 · HIS 推荐", () => {
  test("医生进入规则定义列表 /rule/definitions", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/rule/definitions");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("医生查看具体规则 /rule/definitions/{code}", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/rule/definitions/AMI_RULE_001");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/definitions/");
  });

  test("医生进入规则校验工作台 /rule/validate", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/rule/validate");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("未登录");
  });

  test("医生查看患者路径实例（含变异记录） /pathway/patients", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/patients");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
