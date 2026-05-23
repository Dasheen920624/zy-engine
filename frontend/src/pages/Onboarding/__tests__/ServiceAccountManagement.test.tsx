import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import ServiceAccountManagement from "../ServiceAccountManagement";

describe("ServiceAccountManagement", () => {
  it("应渲染服务账号管理页面", () => {
    render(<ServiceAccountManagement />);
  });

  it("应显示服务账号管理标题", () => {
    render(<ServiceAccountManagement />);
    expect(screen.getByText("服务账号管理")).toBeTruthy();
  });

  it("应显示创建服务账号按钮", () => {
    render(<ServiceAccountManagement />);
    expect(screen.getByText("创建服务账号")).toBeTruthy();
  });

  it("应显示表格列标题", async () => {
    render(<ServiceAccountManagement />);
    expect(await screen.findByText("账号名称")).toBeTruthy();
    expect(screen.getByText("账号类型")).toBeTruthy();
    expect(screen.getByText("状态")).toBeTruthy();
  });
});
