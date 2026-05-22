import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import CdssAlertDialog from "../CdssAlertDialog";

// Mock API
vi.mock("../../../api/cdss", () => ({
  resolveCdssAlert: vi.fn().mockResolvedValue({}),
}));

// Mock CSS module
vi.mock("../cdssAlertDialog.module.css", () => ({
  default: {
    riskIcon: "riskIcon",
    riskInfo: "riskInfo",
    riskLow: "riskLow",
    riskMedium: "riskMedium",
    riskHigh: "riskHigh",
    riskCritical: "riskCritical",
    riskLabel: "riskLabel",
    contentBlock: "contentBlock",
    alertSpacing: "alertSpacing",
    evidenceBlock: "evidenceBlock",
    evidenceItem: "evidenceItem",
    sourceBlock: "sourceBlock",
    ruleBlock: "ruleBlock",
  },
}));

const mockAlert = {
  alertId: "ALERT-001",
  triggerPoint: "ORDER",
  riskLevel: "HIGH" as const,
  title: "药物相互作用告警",
  message: "检测到药物相互作用风险",
  evidence: [{ description: "药物A与药物B存在相互作用" }],
  source: { documentCode: "DOC-001", citationId: "CIT-001" },
  requiresConfirmation: true,
  isBlocking: false,
  patientId: "P-001",
  encounterId: "E-001",
  ruleCode: "R-DRUG-001",
  ruleVersion: "1.0",
  createdAt: "2026-05-20T10:00:00",
};

describe("CdssAlertDialog", () => {
  it("应在 visible=true 且 alert 存在时渲染弹窗", () => {
    render(
      <CdssAlertDialog
        alert={mockAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("药物相互作用告警")).toBeTruthy();
  });

  it("应显示风险等级标签", () => {
    render(
      <CdssAlertDialog
        alert={mockAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("[高风险]")).toBeTruthy();
  });

  it("应显示操作人表单项", () => {
    render(
      <CdssAlertDialog
        alert={mockAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("操作人")).toBeTruthy();
  });

  it("应显示确认知悉按钮", () => {
    render(
      <CdssAlertDialog
        alert={mockAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("确认知悉")).toBeTruthy();
  });

  it("非阻断级告警应显示覆盖继续按钮", () => {
    render(
      <CdssAlertDialog
        alert={mockAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("覆盖继续")).toBeTruthy();
  });

  it("阻断级告警应显示上报上级按钮", () => {
    const criticalAlert = { ...mockAlert, riskLevel: "CRITICAL" as const, isBlocking: true };
    render(
      <CdssAlertDialog
        alert={criticalAlert as never}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(screen.getByText("上报上级")).toBeTruthy();
  });

  it("应在 alert=null 时不渲染", () => {
    const { container } = render(
      <CdssAlertDialog
        alert={null}
        visible={true}
        onClose={() => {}}
        onResolved={() => {}}
      />
    );
    expect(container.innerHTML).toBe("");
  });
});
