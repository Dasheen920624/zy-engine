import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import ConfigWizardModal from "../ConfigWizardModal";

// Mock CSS module
vi.mock("../configWizardModal.module.css", () => ({
  default: {
    sectionHeader: "sectionHeader",
    smallText: "smallText",
    footerRight: "footerRight",
  },
}));

const defaultProps = {
  visible: true,
  type: "org" as const,
  onClose: () => {},
  onDepartmentsChange: () => {},
  onRolesChange: () => {},
  onRulePackagesChange: () => {},
  onPermTemplatesChange: () => {},
  departments: [],
  roles: [],
  availableRulePackages: [],
  selectedRulePackages: [],
  selectedPermTemplates: [],
};

describe("ConfigWizardModal", () => {
  it("应渲染组织配置向导", () => {
    render(<ConfigWizardModal {...defaultProps} />);
    expect(screen.getByText("组织配置向导")).toBeTruthy();
  });

  it("应显示科室管理标签", () => {
    render(<ConfigWizardModal {...defaultProps} />);
    expect(screen.getByText("科室管理")).toBeTruthy();
  });

  it("应显示角色管理标签", () => {
    render(<ConfigWizardModal {...defaultProps} />);
    expect(screen.getByText("角色管理")).toBeTruthy();
  });

  it("应显示确认保存按钮", () => {
    render(<ConfigWizardModal {...defaultProps} />);
    expect(screen.getByText("确认保存")).toBeTruthy();
  });

  it("应显示导入预设科室按钮", () => {
    render(<ConfigWizardModal {...defaultProps} />);
    expect(screen.getByText("导入预设科室")).toBeTruthy();
  });

  it("应渲染规则配置向导", () => {
    render(<ConfigWizardModal {...defaultProps} type="rule" />);
    expect(screen.getByText("规则配置向导")).toBeTruthy();
  });

  it("应渲染权限配置向导", () => {
    render(<ConfigWizardModal {...defaultProps} type="permission" />);
    expect(screen.getByText("权限配置向导")).toBeTruthy();
  });
});
