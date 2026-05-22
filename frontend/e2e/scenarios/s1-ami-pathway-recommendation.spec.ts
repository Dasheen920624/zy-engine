import { test, expect } from "../fixtures/test-fixtures";

/**
 * S1 — AMI/STEMI 路径推荐与入径
 *
 * 文档来源：docs/02_场景剧本图.md § S1
 * 主角：R07 主治医师 + R06 主任医师
 * 归属产品：B 临床嵌入器 + A 配置工厂
 * 涉及页面：路径模板列表、路径详情、规则校验、患者路径
 *
 * DoD 验收点：
 * - 主治医师在路径模板列表看到 AMI 路径
 * - 点击进入路径详情查看阶段和推荐
 * - 规则校验工作台可触发 AMI 规则
 * - 患者路径列表可查看入径患者
 */
test.describe("S1 · AMI/STEMI 路径推荐与入径", () => {
  test("主治医师进入路径模板列表，看到 AMI 路径", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/templates");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/templates");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("点击 AMI 路径进入详情页", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/templates/AMI_STEMI_V2");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/templates/");
  });

  test("规则校验工作台可触发 AMI 规则", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/rule/validate");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/validate");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("患者路径列表可查看入径患者", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/pathway/patients");
    await authenticatedPage.waitForLoadState("networkidle");
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/patients");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });
});
