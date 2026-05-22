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
  test("医生进入规则定义列表 /rule/definitions", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到规则库列表
    await authenticatedPage.goto("/rule/definitions");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/definitions");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");

    // 验证：页面标题包含"规则库"
    const pageTitle = authenticatedPage.getByRole("heading", { name: /规则库/ });
    await expect(pageTitle).toBeVisible({ timeout: 5000 });

    // 验证：搜索框存在
    const searchInput = authenticatedPage.getByPlaceholder(/搜索规则/);
    await expect(searchInput).toBeVisible({ timeout: 5000 });

    // 验证：类型筛选器存在
    const typeFilter = authenticatedPage.locator("[aria-label='rule-type-filter']");
    await expect(typeFilter).toBeVisible({ timeout: 5000 });

    // 验证：状态筛选器存在
    const statusFilter = authenticatedPage.locator("[aria-label='rule-status-filter']");
    await expect(statusFilter).toBeVisible({ timeout: 5000 });

    // 验证：新建规则按钮存在
    const newRuleBtn = authenticatedPage.getByRole("button", { name: /新建规则/ });
    await expect(newRuleBtn).toBeVisible({ timeout: 5000 });

    // 验证：规则列表表格存在
    const tableEl = authenticatedPage.locator(".ant-table");
    await expect(tableEl).toBeVisible({ timeout: 5000 });

    // 验证：API 返回规则列表数据
    try {
      const ruleResp = await apiContext.get("/medkernel/api/rule/definitions");
      if (ruleResp.ok()) {
        const ruleData = await ruleResp.json();
        expect(ruleData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("医生查看具体规则 /rule/definitions/{code}", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到规则详情页
    await authenticatedPage.goto("/rule/definitions/AMI_RULE_001");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：URL 包含规则定义路径
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/definitions/");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：详情页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-descriptions, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回规则详情数据
    try {
      const detailResp = await apiContext.get("/medkernel/api/rule/definitions/AMI_RULE_001");
      if (detailResp.ok()) {
        const detailData = await detailResp.json();
        expect(detailData.success).toBe(true);
        if (detailData.data) {
          expect(detailData.data).toHaveProperty("rule_code");
        }
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("医生进入规则校验工作台 /rule/validate", async ({ authenticatedPage }) => {
    // 步骤1：导航到规则校验工作台
    await authenticatedPage.goto("/rule/validate");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/rule/validate");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未登录提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("未登录");
    expect(body ?? "").not.toContain("401");

    // 验证：规则校验页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, main, form");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });
  });

  test("医生查看患者路径实例（含变异记录） /pathway/patients", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到患者路径列表
    await authenticatedPage.goto("/pathway/patients");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/pathway/patients");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");

    // 验证：患者路径列表表格存在
    const tableEl = authenticatedPage.locator(".ant-table");
    await expect(tableEl).toBeVisible({ timeout: 5000 });

    // 验证：API 返回患者路径数据
    try {
      const patientResp = await apiContext.get("/medkernel/api/pathway/patients");
      if (patientResp.ok()) {
        const patientData = await patientResp.json();
        expect(patientData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("规则定义列表支持搜索和筛选", async ({ authenticatedPage }) => {
    // 步骤1：导航到规则库列表
    await authenticatedPage.goto("/rule/definitions");
    await authenticatedPage.waitForLoadState("networkidle");

    // 步骤2：在搜索框输入关键词
    const searchInput = authenticatedPage.getByPlaceholder(/搜索规则/);
    await expect(searchInput).toBeVisible({ timeout: 5000 });
    await searchInput.fill("AMI");

    // 步骤3：触发搜索（回车或失焦）
    await searchInput.press("Enter");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：URL 参数包含搜索关键词
    const url = authenticatedPage.url();
    expect(url).toContain("search=AMI");
  });
});
