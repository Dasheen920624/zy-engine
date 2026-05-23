import { test, expect } from "../fixtures/medkernel-fixtures";

/**
 * 剧本 S1 · 院级质控驾驶舱（GA-QUALITY-01）
 *
 * 院长场景：登录 → 工作台 → 院级质控驾驶舱 → 看 4 指标 + 科室得分
 */
test.describe("S1 · 院级质控驾驶舱", () => {
  test("院长 30 秒看完质控全貌", async ({ page, goto }) => {
    await goto("/dashboard");

    // 工作台必含"租户生命周期"主面板
    await expect(page.locator("text=租户生命周期")).toBeVisible();

    // 工作台必含 4 主线统计
    await expect(page.locator("text=临床运行 · 今日提醒")).toBeVisible();
    await expect(page.locator("text=质控改进 · 未闭环")).toBeVisible();

    // 切到质控驾驶舱
    await page.locator('a[href="/qc/dashboard"], li:has-text("院级质控驾驶舱")').first().click();
    await page.waitForURL("**/qc/dashboard");

    // 4 个核心 KPI 必须可见
    await expect(page.locator("text=本月整改闭环率")).toBeVisible();
    await expect(page.locator("text=DRG 入组率")).toBeVisible();
    await expect(page.locator("text=CDSS 提醒采纳率")).toBeVisible();
    await expect(page.locator("text=医保拒付率")).toBeVisible();

    // 科室得分卡可见
    await expect(page.locator("text=科室质控得分")).toBeVisible();
  });
});
