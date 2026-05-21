import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import DryRunPanel from "../DryRunPanel";
import { simulateRule } from "../../../../api/rule";
import type { HitItem } from "../../../../api/types";

vi.mock("../../../../api/rule", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../api/rule")>("../../../../api/rule");
  return {
    ...actual,
    simulateRule: vi.fn(),
  };
});

function renderPanel(props: { ruleCode?: string; ruleDslText?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider>
      <QueryClientProvider client={queryClient}>
        <DryRunPanel {...props} />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

const SAMPLE_HIT: HitItem = {
  rule_code: "DEMO",
  rule_name: "示例",
  severity: "HIGH",
  hit: true,
  message: "命中提示",
};

describe("DryRunPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("pre-fills facts textarea from default AMI scenario", async () => {
    renderPanel();
    const textarea = screen.getByLabelText("patient-context-json") as HTMLTextAreaElement;
    await waitFor(() => {
      expect(textarea.value).toContain("P-202605210001");
    });
    expect(textarea.value).toContain("encounter");
    expect(textarea.value).toContain("facts");
  });

  it("calls simulateRule on click and renders hit summary", async () => {
    vi.mocked(simulateRule).mockResolvedValue(SAMPLE_HIT);
    const user = userEvent.setup();
    renderPanel({ ruleCode: "DEMO" });
    const button = screen.getByLabelText("trigger-dry-run");
    await user.click(button);
    await waitFor(() => {
      expect(screen.getByText("命中提示")).toBeInTheDocument();
    });
    expect(vi.mocked(simulateRule)).toHaveBeenCalledTimes(1);
  });
});
