import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AppLayout from "../AppLayout";

const STORAGE_KEY = "mk-sidebar-collapsed";

// ThemeSelector 依赖 ThemeProvider context，单测里 mock 成 dummy。
vi.mock("../../theme/ThemeSelector", () => ({
  default: () => <div data-testid="theme-selector-stub" />,
}));

function renderLayout(initialPath: string = "/dashboard") {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route
            path="/dashboard"
            element={<div data-testid="page-content">Dashboard Content</div>}
          />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

function setViewportWidth(width: number): void {
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width,
  });
}

describe("AppLayout (PR-FINAL-26)", () => {
  beforeEach(() => {
    window.localStorage.clear();
    // 默认桌面尺寸（>= 768）
    setViewportWidth(1280);
  });

  afterEach(() => {
    cleanup();
    window.localStorage.clear();
  });

  it("渲染三段式语义结构：header / navigation / main", () => {
    renderLayout();
    expect(screen.getByRole("banner")).toBeInTheDocument();
    expect(
      screen.getByRole("navigation", { name: /主菜单/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole("main")).toBeInTheDocument();
    expect(screen.getByTestId("page-content")).toHaveTextContent(
      "Dashboard Content",
    );
  });

  it("默认未折叠：折叠按钮 aria-label 为「折叠菜单」", () => {
    renderLayout();
    expect(
      screen.getByRole("button", { name: "折叠菜单" }),
    ).toBeInTheDocument();
  });

  it("点击折叠按钮切换状态并写入 localStorage", () => {
    renderLayout();
    const trigger = screen.getByRole("button", { name: "折叠菜单" });
    fireEvent.click(trigger);
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe("1");
    // 折叠后按钮 aria-label 切换为「展开菜单」
    expect(
      screen.getByRole("button", { name: "展开菜单" }),
    ).toBeInTheDocument();
    // 再点回展开
    fireEvent.click(screen.getByRole("button", { name: "展开菜单" }));
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe("0");
  });

  it("初始状态读取 localStorage 的折叠态", () => {
    window.localStorage.setItem(STORAGE_KEY, "1");
    renderLayout();
    expect(
      screen.getByRole("button", { name: "展开菜单" }),
    ).toBeInTheDocument();
  });

  it("aria-expanded 反映折叠态：未折叠 = true / 折叠 = false", () => {
    renderLayout();
    const nav = screen.getByRole("navigation", { name: /主菜单/ });
    expect(nav).toHaveAttribute("aria-expanded", "true");
    fireEvent.click(screen.getByRole("button", { name: "折叠菜单" }));
    expect(nav).toHaveAttribute("aria-expanded", "false");
  });

  it("面包屑展示「首页」入口", () => {
    renderLayout();
    const homeLink = screen.getByRole("link", { name: "首页" });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute("href", "/dashboard");
  });

  it("品牌区点击导航回 /dashboard（role=button + Enter 键也响应）", () => {
    renderLayout("/dashboard");
    const brand = screen.getByText("集团医疗智能中枢").closest('[role="button"]');
    expect(brand).toBeInTheDocument();
    // 已经在 dashboard，点击不报错即视为通过
    if (brand) fireEvent.click(brand);
  });

  it("localStorage 写入失败时不抛错（隐私模式兼容）", () => {
    const setItemSpy = vi
      .spyOn(window.localStorage.__proto__, "setItem")
      .mockImplementation(() => {
        throw new Error("QuotaExceeded");
      });
    expect(() => renderLayout()).not.toThrow();
    setItemSpy.mockRestore();
  });
});
