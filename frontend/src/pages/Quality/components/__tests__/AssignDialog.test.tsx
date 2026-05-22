import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import AssignDialog from "../AssignDialog";

// Mock API
vi.mock("../../../../api/quality", () => ({
  assignProblem: vi.fn().mockResolvedValue({}),
}));

// Mock CSS module
vi.mock("../assignDialog.module.css", () => ({
  default: {
    form: "form",
    fullWidth: "fullWidth",
  },
}));

const mockAlert = {
  id: "alert-001",
  rule_code: "R-001",
  rule_name: "药物相互作用检查",
  severity: "HIGH",
  status: "OPEN",
  patient_id: "P-001",
  encounter_id: "E-001",
  trigger_point: "ORDER",
  description: "检测到药物相互作用",
  created_at: "2026-05-20T10:00:00",
};

describe("AssignDialog", () => {
  it("应在 visible=true 时渲染派单弹窗", () => {
    render(
      <AssignDialog
        visible={true}
        alert={mockAlert as never}
        onClose={() => {}}
        onAssigned={() => {}}
      />
    );
    expect(screen.getByText(/派单/)).toBeTruthy();
  });

  it("应显示指派给表单项", () => {
    render(
      <AssignDialog
        visible={true}
        alert={mockAlert as never}
        onClose={() => {}}
        onAssigned={() => {}}
      />
    );
    expect(screen.getByText("指派给")).toBeTruthy();
  });

  it("应显示角色表单项", () => {
    render(
      <AssignDialog
        visible={true}
        alert={mockAlert as never}
        onClose={() => {}}
        onAssigned={() => {}}
      />
    );
    expect(screen.getByText("角色")).toBeTruthy();
  });

  it("应显示确认派单按钮", () => {
    render(
      <AssignDialog
        visible={true}
        alert={mockAlert as never}
        onClose={() => {}}
        onAssigned={() => {}}
      />
    );
    expect(screen.getByText("确认派单")).toBeTruthy();
  });

  it("应在 alert=null 时不渲染", () => {
    const { container } = render(
      <AssignDialog
        visible={true}
        alert={null}
        onClose={() => {}}
        onAssigned={() => {}}
      />
    );
    expect(container.innerHTML).toBe("");
  });
});
