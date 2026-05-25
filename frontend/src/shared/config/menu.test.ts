import { describe, it, expect } from "vitest";
import { menuSections } from "./menu";
import { routeMetas } from "./routes";

describe("menu config", () => {
  it("has exactly 6 sections (5 visible + 1 hidden advanced)", () => {
    expect(menuSections).toHaveLength(6);
    const visible = menuSections.filter((s) => !s.hidden);
    expect(visible).toHaveLength(5);
  });

  it("matches CONSTITUTION §2.1 ordering", () => {
    expect(menuSections[0].key).toBe("workbench");
    expect(menuSections[1].label).toBe("试点准备");
    expect(menuSections[2].label).toBe("临床运行");
    expect(menuSections[3].label).toBe("质控改进");
    expect(menuSections[4].label).toBe("合规运维");
    expect(menuSections[5].label).toBe("高级工具");
  });

  it("advanced tools section is hidden", () => {
    const advanced = menuSections.find((s) => s.key === "advanced-tools");
    expect(advanced?.hidden).toBe(true);
  });

  it("all items have a valid path starting with /", () => {
    menuSections.forEach((s) => {
      s.items.forEach((it) => {
        expect(it.path).toMatch(/^\//);
      });
    });
  });

  it("derives every menu item from route metadata", () => {
    const routePaths = new Set(routeMetas.map((route) => route.path));

    menuSections.forEach((section) => {
      section.items.forEach((item) => {
        expect(routePaths.has(item.path)).toBe(true);
      });
    });
  });

  it("total customer-facing menu items <= 30 (per §2.2)", () => {
    const visible = menuSections.filter((s) => !s.hidden);
    const total = visible.reduce((sum, s) => sum + s.items.length, 0);
    expect(total).toBeLessThanOrEqual(30);
  });
});
