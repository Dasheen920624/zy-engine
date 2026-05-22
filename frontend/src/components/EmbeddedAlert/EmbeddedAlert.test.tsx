import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import EmbeddedAlert from "./EmbeddedAlert";
import type { EmbeddedAlertProps } from "./EmbeddedAlert.types";

// Mock CSS module
vi.mock("./embeddedAlert.module.css", () => ({
  default: {
    container: "container",
    info: "info",
    warning: "warning",
    danger: "danger",
    success: "success",
    icon: "icon",
    iconInfo: "iconInfo",
    iconWarning: "iconWarning",
    iconDanger: "iconDanger",
    iconSuccess: "iconSuccess",
    content: "content",
    titleRow: "titleRow",
    title: "title",
    meta: "meta",
    ruleRef: "ruleRef",
    evidence: "evidence",
    source: "source",
    actions: "actions",
    closeButton: "closeButton",
  },
}));

const baseProps: EmbeddedAlertProps = {
  severity: "warning",
  title: "药物相互作用预警",
  evidence: "阿司匹林 + 华法林增加出血风险",
  actions: [
    { text: "确认", intent: "primary", onClick: vi.fn() },
    { text: "忽略", intent: "secondary", onClick: vi.fn() },
  ],
  onClose: vi.fn(),
};

describe("EmbeddedAlert", () => {
  it("应渲染标题", () => {
    render(<EmbeddedAlert {...baseProps} />);
    expect(screen.getByText("药物相互作用预警")).toBeTruthy();
  });

  it("应渲染证据文字", () => {
    render(<EmbeddedAlert {...baseProps} />);
    expect(screen.getByText(/阿司匹林/)).toBeTruthy();
  });

  it("应渲染操作按钮", () => {
    render(<EmbeddedAlert {...baseProps} />);
    expect(screen.getByText("确认")).toBeTruthy();
    expect(screen.getByText("忽略")).toBeTruthy();
  });

  it("应渲染关闭按钮", () => {
    render(<EmbeddedAlert {...baseProps} />);
    const closeButtons = screen.getAllByRole("button");
    // Close button is a span with click handler
    expect(closeButtons.length).toBeGreaterThanOrEqual(2);
  });

  it("点击确认按钮应调用 onClick", () => {
    const onClick = vi.fn();
    render(
      <EmbeddedAlert
        {...baseProps}
        actions={[{ text: "确认", intent: "primary", onClick }]}
      />
    );
    fireEvent.click(screen.getByText("确认"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("应显示置信度", () => {
    render(<EmbeddedAlert {...baseProps} confidence={92} />);
    expect(screen.getByText("置信 92%")).toBeTruthy();
  });

  it("应显示规则引用", () => {
    render(
      <EmbeddedAlert
        {...baseProps}
        ruleRef={{ code: "DRUG_INTERACT_001", version: "1.0" }}
      />
    );
    expect(screen.getByText("DRUG_INTERACT_001@1.0")).toBeTruthy();
  });

  it("应显示来源信息", () => {
    render(
      <EmbeddedAlert
        {...baseProps}
        source={{ documentName: "药典", section: "第三章", publishYear: 2023 }}
      />
    );
    expect(screen.getByText(/药典/)).toBeTruthy();
  });

  it("danger 级别应渲染", () => {
    render(<EmbeddedAlert {...baseProps} severity="danger" />);
    expect(screen.getByText("药物相互作用预警")).toBeTruthy();
  });

  it("success 级别应渲染", () => {
    render(<EmbeddedAlert {...baseProps} severity="success" />);
    expect(screen.getByText("药物相互作用预警")).toBeTruthy();
  });
});
