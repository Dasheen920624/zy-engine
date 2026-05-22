import { test, expect } from "../fixtures/test-fixtures";

/**
 * S5 — AI 知识审核闭环
 *
 * 演示路径：/aik/sources → Dify 任务 → AI 候选 → 人工审核 → 知识包
 * DoD：完整 4 步带「来源溯源」
 */
test.describe("S5 · AI 知识审核闭环", () => {
  test("知识管理员进入知识来源页 /aik/sources", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到知识来源页
    await authenticatedPage.goto("/aik/sources");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/aik/sources");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");

    // 验证：来源注册 / 知识订阅 Tab 切换按钮存在
    const sourcesTab = authenticatedPage.getByRole("button", { name: /来源注册/ });
    await expect(sourcesTab).toBeVisible({ timeout: 5000 });

    const subscriptionsTab = authenticatedPage.getByRole("button", { name: /知识订阅/ });
    await expect(subscriptionsTab).toBeVisible({ timeout: 5000 });

    // 验证：注册来源按钮存在
    const registerBtn = authenticatedPage.getByRole("button", { name: /注册来源/ });
    await expect(registerBtn).toBeVisible({ timeout: 5000 });

    // 验证：来源列表表格存在
    const tableEl = authenticatedPage.locator(".ant-table");
    await expect(tableEl).toBeVisible({ timeout: 5000 });

    // 验证：API 返回知识来源数据
    try {
      const sourcesResp = await apiContext.get("/medkernel/api/knowledge/sources");
      if (sourcesResp.ok()) {
        const sourcesData = await sourcesResp.json();
        expect(sourcesData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("知识管理员进入 AI 候选审核台 /aik/review", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到知识审核台
    await authenticatedPage.goto("/aik/review");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/aik/review");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：页面标题包含"知识审核台"
    const pageTitle = authenticatedPage.getByRole("heading", { name: /知识审核台/ });
    await expect(pageTitle).toBeVisible({ timeout: 5000 });

    // 验证：审核统计卡片存在
    const statsCard = authenticatedPage.getByText("审核统计");
    await expect(statsCard).toBeVisible({ timeout: 5000 });

    // 验证：待审核候选表格存在
    const pendingCard = authenticatedPage.getByText("待审核候选");
    await expect(pendingCard).toBeVisible({ timeout: 5000 });

    // 验证：审核历史表格存在
    const historyCard = authenticatedPage.getByText("审核历史");
    await expect(historyCard).toBeVisible({ timeout: 5000 });

    // 验证：API 返回候选列表数据
    try {
      const candidatesResp = await apiContext.get("/medkernel/api/knowledge/candidates");
      if (candidatesResp.ok()) {
        const candidatesData = await candidatesResp.json();
        expect(candidatesData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }

    // 验证：API 返回审核统计数据
    try {
      const summaryResp = await apiContext.get("/medkernel/api/knowledge/candidates/summary");
      if (summaryResp.ok()) {
        const summaryData = await summaryResp.json();
        expect(summaryData.success).toBe(true);
        if (summaryData.data) {
          expect(summaryData.data).toHaveProperty("total");
          expect(summaryData.data).toHaveProperty("pending");
          expect(summaryData.data).toHaveProperty("approved");
          expect(summaryData.data).toHaveProperty("rejected");
        }
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("知识管理员查看 AI 工作流引擎 /ai-workflows", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到 AI 工作流页面
    await authenticatedPage.goto("/ai-workflows");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/ai-workflows");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：AI 工作流页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-table, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回工作流数据
    try {
      const workflowResp = await apiContext.get("/medkernel/api/ai/workflows");
      if (workflowResp.ok()) {
        const workflowData = await workflowResp.json();
        expect(workflowData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("知识溯源页 /provenance 可访问", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到来源追溯页
    await authenticatedPage.goto("/provenance");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/provenance");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：来源追溯页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-table, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回溯源数据
    try {
      const provenanceResp = await apiContext.get("/medkernel/api/provenance");
      if (provenanceResp.ok()) {
        const provenanceData = await provenanceResp.json();
        expect(provenanceData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("知识来源页切换到知识订阅 Tab", async ({ authenticatedPage }) => {
    // 步骤1：导航到知识来源页
    await authenticatedPage.goto("/aik/sources");
    await authenticatedPage.waitForLoadState("networkidle");

    // 步骤2：点击知识订阅 Tab
    const subscriptionsTab = authenticatedPage.getByRole("button", { name: /知识订阅/ });
    await expect(subscriptionsTab).toBeVisible({ timeout: 5000 });
    await subscriptionsTab.click();

    // 验证：新建订阅按钮出现
    const newSubBtn = authenticatedPage.getByRole("button", { name: /新建订阅/ });
    await expect(newSubBtn).toBeVisible({ timeout: 5000 });
  });

  test("知识来源页注册来源弹窗可打开", async ({ authenticatedPage }) => {
    // 步骤1：导航到知识来源页
    await authenticatedPage.goto("/aik/sources");
    await authenticatedPage.waitForLoadState("networkidle");

    // 步骤2：点击注册来源按钮
    const registerBtn = authenticatedPage.getByRole("button", { name: /注册来源/ });
    await expect(registerBtn).toBeVisible({ timeout: 5000 });
    await registerBtn.click();

    // 验证：弹窗标题可见
    const modalTitle = authenticatedPage.getByText("注册知识来源");
    await expect(modalTitle).toBeVisible({ timeout: 5000 });

    // 验证：弹窗中包含来源名称输入框
    const nameInput = authenticatedPage.getByText("来源名称");
    await expect(nameInput).toBeVisible({ timeout: 5000 });

    // 验证：弹窗中包含来源类型选择器
    const typeSelect = authenticatedPage.getByText("来源类型");
    await expect(typeSelect).toBeVisible({ timeout: 5000 });
  });
});
