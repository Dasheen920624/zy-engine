import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import ReviewDrawer from "./ReviewDrawer";
import type { ReviewDrawerProps } from "./ReviewDrawer.types";

// Mock CSS module
vi.mock("./reviewDrawer.module.css", () => ({
  default: {
    footer: "footer",
    fullWidth: "fullWidth",
    reasonInput: "reasonInput",
  },
}));

const baseProps: ReviewDrawerProps = {
  visible: true,
  onClose: vi.fn(),
  title: "审核详情",
  reviewStatus: "pending",
  onApprove: vi.fn(),
  onReject: vi.fn(),
  onTransfer: vi.fn(),
  children: <div data-testid="drawer-content">审核内容</div>,
};

describe("ReviewDrawer", () => {
  it("visible=true 时应渲染抽屉标题", () => {
    render(<ReviewDrawer {...baseProps} />);
    expect(screen.getByText("审核详情")).toBeTruthy();
  });

  it("应渲染子内容", () => {
    render(<ReviewDrawer {...baseProps} />);
    expect(screen.getByTestId("drawer-content")).toBeTruthy();
  });

  it("应显示通过按钮", () => {
    render(<ReviewDrawer {...baseProps} showApprove />);
    expect(screen.getByText("通过")).toBeTruthy();
  });

  it("应显示驳回按钮", () => {
    render(<ReviewDrawer {...baseProps} showReject />);
    expect(screen.getByText("驳回")).toBeTruthy();
  });

  it("点击通过按钮应调用 onApprove", () => {
    const onApprove = vi.fn();
    render(<ReviewDrawer {...baseProps} onApprove={onApprove} />);
    fireEvent.click(screen.getByText("通过"));
    expect(onApprove).toHaveBeenCalledTimes(1);
  });

  it("点击驳回按钮应展开驳回表单", () => {
    render(<ReviewDrawer {...baseProps} showReject />);
    fireEvent.click(screen.getByText("驳回"));
    expect(screen.getByPlaceholderText("请输入驳回理由")).toBeTruthy();
  });

  it("pending 状态下按钮应可用", () => {
    render(<ReviewDrawer {...baseProps} reviewStatus="pending" />);
    expect(screen.getByText("通过")).not.toBeDisabled();
  });

  it("approved 状态下按钮应禁用", () => {
    render(<ReviewDrawer {...baseProps} reviewStatus="approved" />);
    expect(screen.getByText("通过")).toBeDisabled();
  });

  it("showTransfer=true 时应显示转人工按钮", () => {
    render(<ReviewDrawer {...baseProps} showTransfer />);
    expect(screen.getByText("转人工")).toBeTruthy();
  });

  it("loading 状态下按钮应禁用", () => {
    render(<ReviewDrawer {...baseProps} loading />);
    expect(screen.getByText("通过")).toBeDisabled();
  });
});
