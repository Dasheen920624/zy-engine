import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { StepFlow, SEVEN_STEPS } from "./StepFlow";

describe("StepFlow", () => {
  it("renders all 7 step titles", () => {
    render(<StepFlow currentStep="impact_preview" />);
    SEVEN_STEPS.forEach((s) => {
      expect(screen.getAllByText(s.title).length).toBeGreaterThan(0);
    });
  });

  it("renders the panel for current step", () => {
    render(
      <StepFlow
        currentStep="auto_validate"
        panelByStep={{ auto_validate: <div data-testid="my-panel">校验通过</div> }}
      />,
    );
    expect(screen.getByTestId("my-panel")).toBeInTheDocument();
  });

  it("falls back to placeholder when no panel provided", () => {
    render(<StepFlow currentStep="full_rollout" />);
    expect(screen.getByText(/待 GA-TENANT-01/)).toBeInTheDocument();
  });
});
