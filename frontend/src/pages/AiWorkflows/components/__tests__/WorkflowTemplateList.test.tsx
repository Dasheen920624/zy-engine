import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import WorkflowTemplateList from "../WorkflowTemplateList";
import type { WorkflowTemplate } from "../../../../api/aiWorkflows";

const TEMPLATES: WorkflowTemplate[] = [
  {
    workflow_code: "WF_AMI_TRIAGE",
    workflow_name: "AMI 急诊分诊编排",
    workflow_version: "1.0.0",
    description: "急性心梗到达急诊后的多步分诊 + PCI 决策编排。",
    dify_app_code: "ami-triage",
    timeout_ms: 8000,
    retry_count: 2,
    required_inputs: ["patient_id", "ecg_image"],
    reference_document_code: "GUIDE_AMI_2025",
  },
  {
    workflow_code: "WF_STROKE_THROMB",
  },
];

describe("WorkflowTemplateList", () => {
  it("renders workflow name and version tag", () => {
    render(<WorkflowTemplateList templates={TEMPLATES} />);
    expect(screen.getByText("AMI 急诊分诊编排")).toBeInTheDocument();
    expect(screen.getByText("v1.0.0")).toBeInTheDocument();
  });

  it("renders meta line with timeout/retry/inputs/source", () => {
    render(<WorkflowTemplateList templates={TEMPLATES} />);
    expect(screen.getByText(/timeout: 8000 ms/)).toBeInTheDocument();
    expect(screen.getByText(/retry: 2/)).toBeInTheDocument();
    expect(screen.getByText(/inputs: patient_id, ecg_image/)).toBeInTheDocument();
    expect(screen.getByText(/source: GUIDE_AMI_2025/)).toBeInTheDocument();
  });

  it("falls back to code when name missing", () => {
    render(<WorkflowTemplateList templates={TEMPLATES} />);
    expect(screen.getByText("WF_STROKE_THROMB")).toBeInTheDocument();
  });

  it("renders empty hint when no templates", () => {
    render(<WorkflowTemplateList templates={[]} />);
    expect(screen.getByText("暂无多步工作流模板")).toBeInTheDocument();
  });
});
