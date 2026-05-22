import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import IdentityBindingManagement from "./IdentityBindingManagement";

// Mock CSS module
vi.mock("./identityBindingManagement.module.css", () => ({
  default: {
    page: "page",
    conflictAlert: "conflictAlert",
    queryCard: "queryCard",
    userInput: "userInput",
    resultTable: "resultTable",
  },
}));

// Mock API
vi.mock("../api/identityBinding", () => ({
  listBindingsByUser: vi.fn().mockResolvedValue([]),
  bindIdentity: vi.fn(),
  unbindIdentity: vi.fn(),
  mergeBindings: vi.fn(),
  findConflicts: vi.fn().mockResolvedValue([]),
}));

describe("IdentityBindingManagement", () => {
  it("应渲染身份绑定管理标题", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByText("身份绑定管理")).toBeTruthy();
  });

  it("应显示用户绑定查询卡片", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByText("用户绑定查询")).toBeTruthy();
  });

  it("应显示查询按钮", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByText("查询")).toBeTruthy();
  });

  it("应显示绑定身份按钮", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByText("绑定身份")).toBeTruthy();
  });

  it("应显示合并用户按钮", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByText("合并用户")).toBeTruthy();
  });

  it("应显示用户 ID 输入框", () => {
    render(<IdentityBindingManagement />);
    expect(screen.getByPlaceholderText("输入用户 ID")).toBeTruthy();
  });
});
