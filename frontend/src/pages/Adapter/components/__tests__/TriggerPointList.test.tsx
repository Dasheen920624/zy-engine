import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import TriggerPointList from "../TriggerPointList";
import { listTriggerPoints } from "../../../../api/adapterHub";

vi.mock("../../../../api/adapterHub", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../api/adapterHub")>(
      "../../../../api/adapterHub",
    );
  return {
    ...actual,
    listTriggerPoints: vi.fn(),
    registerTriggerPoint: vi.fn(),
    updateTriggerPoint: vi.fn(),
    executeTriggerPoint: vi.fn(),
  };
});

function renderList() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider>
      <QueryClientProvider client={queryClient}>
        <TriggerPointList />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe("TriggerPointList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listTriggerPoints).mockResolvedValue([
      {
        id: 1,
        triggerCode: "AMI_ON_ADMIT",
        triggerName: "AMI 入院触发",
        triggerType: "EVENT",
        businessScenario: "EmergencyAdmit",
        accessStrategy: "CDS_HOOKS",
        riskLevel: "HIGH",
        priority: 100,
        enabled: "Y",
      },
    ]);
  });

  it("renders 国情合规 hint + filter widgets + new button", async () => {
    renderList();
    expect(screen.getByText(/CDSS 触发点定义院内业务事件/)).toBeInTheDocument();
    expect(screen.getByText("业务场景")).toBeInTheDocument();
    expect(screen.getByText("接入策略")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /新建触发点/ })).toBeInTheDocument();
  });

  it("renders trigger row with code/name/type/risk labels", async () => {
    renderList();
    await waitFor(() => {
      expect(screen.getByText("AMI_ON_ADMIT")).toBeInTheDocument();
    });
    expect(screen.getByText("AMI 入院触发")).toBeInTheDocument();
    expect(screen.getByText("事件触发")).toBeInTheDocument();
    expect(screen.getByText("CDS Hooks")).toBeInTheDocument();
    expect(screen.getByText("高")).toBeInTheDocument();
    expect(screen.getByText("启用")).toBeInTheDocument();
  });

  it("opens TriggerEditModal in create mode when clicking 新建", async () => {
    renderList();
    fireEvent.click(screen.getByRole("button", { name: /新建触发点/ }));
    await waitFor(() => {
      expect(screen.getByText("新建触发点")).toBeInTheDocument();
    });
    // 表单字段
    expect(screen.getByText("触发点编码")).toBeInTheDocument();
  });

  it("reset button clears business scenario filter input", async () => {
    renderList();
    const sceneInput = screen.getByPlaceholderText("business_scenario") as HTMLInputElement;
    fireEvent.change(sceneInput, { target: { value: "EmergencyAdmit" } });
    expect(sceneInput.value).toBe("EmergencyAdmit");

    fireEvent.click(screen.getByRole("button", { name: /重置/ }));
    expect((screen.getByPlaceholderText("business_scenario") as HTMLInputElement).value).toBe("");
  });
});
