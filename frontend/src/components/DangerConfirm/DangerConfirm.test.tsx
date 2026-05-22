import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import DangerConfirm from "./DangerConfirm";
import type { DangerConfirmProps } from "./DangerConfirm.types";

// Mock CSS module
vi.mock("./dangerConfirm.module.css", () => ({
  default: {
    iconWarning: "iconWarning",
    iconDanger: "iconDanger",
    titleWarning: "titleWarning",
    titleDanger: "titleDanger",
    consequenceBlock: "consequenceBlock",
    consequenceList: "consequenceList",
    fieldLabel: "fieldLabel",
    irreversibleAlert: "irreversibleAlert",
    confirmBlock: "confirmBlock",
    errorText: "errorText",
    textDanger: "textDanger",
  },
}));

const baseProps: DangerConfirmProps = {
  level: "low",
  title: "删除配置",
  description: "确认删除此配置？",
  consequences: ["配置将被永久删除"],
  onConfirm: vi.fn(),
  onCancel: vi.fn(),
};

describe("DangerConfirm", () => {
  it("应渲染弹窗标题", () => {
    render(<DangerConfirm {...baseProps} />);
    expect(screen.getByText("删除配置")).toBeTruthy();
  });

  it("应渲染描述文字", () => {
    render(<DangerConfirm {...baseProps} />);
    expect(screen.getByText("确认删除此配置？")).toBeTruthy();
  });

  it("应渲染后果列表", () => {
    render(<DangerConfirm {...baseProps} />);
    expect(screen.getByText("配置将被永久删除")).toBeTruthy();
  });

  it("应渲染确认按钮", () => {
    render(<DangerConfirm {...baseProps} />);
    expect(screen.getByText("确认删除配置")).toBeTruthy();
  });

  it("应渲染取消按钮", () => {
    render(<DangerConfirm {...baseProps} />);
    expect(screen.getByText("取消")).toBeTruthy();
  });

  it("点击取消应调用 onCancel", () => {
    const onCancel = vi.fn();
    render(<DangerConfirm {...baseProps} onCancel={onCancel} />);
    fireEvent.click(screen.getByText("取消"));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("low 级别确认按钮可直接点击", () => {
    const onConfirm = vi.fn();
    render(<DangerConfirm {...baseProps} level="low" onConfirm={onConfirm} />);
    fireEvent.click(screen.getByText("确认删除配置"));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("medium 级别需要输入 confirmText 才能确认", () => {
    const onConfirm = vi.fn();
    render(
      <DangerConfirm
        {...baseProps}
        level="medium"
        confirmText="DELETE"
        onConfirm={onConfirm}
      />
    );
    const confirmBtn = screen.getByText("确认删除配置");
    expect(confirmBtn).toBeDisabled();
  });

  it("high 级别应显示不可撤销提示", () => {
    render(
      <DangerConfirm
        {...baseProps}
        level="high"
        irreversibleNote="此操作不可撤销"
      />
    );
    expect(screen.getByText("此操作不可撤销")).toBeTruthy();
  });

  it("high 级别需要填写原因", () => {
    render(
      <DangerConfirm
        {...baseProps}
        level="high"
        reasonRequired
        confirmText="DELETE"
      />
    );
    expect(screen.getByPlaceholderText("请填写操作原因")).toBeTruthy();
  });
});
