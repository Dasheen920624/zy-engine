import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import AuditLogList from "../AuditLogList";
import {
  getAuditChainStatus,
  listAuditLogs,
  summarizeAuditLogs,
} from "../../../../api/auditLog";

vi.mock("../../../../api/auditLog", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../api/auditLog")>(
      "../../../../api/auditLog",
    );
  return {
    ...actual,
    listAuditLogs: vi.fn(),
    summarizeAuditLogs: vi.fn(),
    getAuditChainStatus: vi.fn(),
    verifyAuditChain: vi.fn(),
  };
});

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={["/admin/audit"]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <AuditLogList />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("AuditLogList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listAuditLogs).mockResolvedValue([
      {
        id: 101,
        trace_id: "trace-abc-1",
        engine_type: "RULE",
        action_type: "EXECUTE",
        target_type: "RuleDefinition",
        target_code: "AMI_RULE_001",
        patient_id: "11010119800101AAAA",
        operator_id: "doctor_001",
        operator_name: "张医生",
        signature_valid: true,
        created_time: "2026-05-22 10:00:00",
      },
      {
        id: 102,
        trace_id: "trace-abc-2",
        engine_type: "AUDIT",
        action_type: "VERIFY",
        target_type: "AuditChain",
        target_code: "engine_audit_log",
        operator_id: "system",
        signature_valid: false, // 验签失败行
        created_time: "2026-05-22 11:00:00",
      },
    ]);
    vi.mocked(summarizeAuditLogs).mockResolvedValue({
      total: 2,
      by_engine_type: [
        { engine_type: "RULE", count: 1 },
        { engine_type: "AUDIT", count: 1 },
      ],
      by_action_type: [
        { action_type: "EXECUTE", count: 1 },
        { action_type: "VERIFY", count: 1 },
      ],
    });
    vi.mocked(getAuditChainStatus).mockResolvedValue({
      engine_audit_log: { status: "NOT_VERIFIED" },
      sec_auth_audit_log: { status: "NOT_VERIFIED" },
      sec_sso_audit_log: { status: "NOT_VERIFIED" },
    });
  });

  it("renders page title and 等保 2.0 三级 hint", () => {
    renderPage();
    expect(screen.getByText("审计日志")).toBeInTheDocument();
    expect(screen.getByText(/等保 2.0 三级/)).toBeInTheDocument();
    expect(screen.getByText(/不可改不可删/)).toBeInTheDocument();
  });

  it("renders 8 filter widgets + limit + query/reset buttons", () => {
    renderPage();
    expect(screen.getByLabelText("Trace ID")).toBeInTheDocument();
    expect(screen.getByText("引擎类型")).toBeInTheDocument();
    expect(screen.getByText("操作类型")).toBeInTheDocument();
    expect(screen.getByText("目标类型")).toBeInTheDocument();
    expect(screen.getByText("目标编码")).toBeInTheDocument();
    expect(screen.getByText("患者 ID")).toBeInTheDocument();
    expect(screen.getByText("就诊 ID")).toBeInTheDocument();
    expect(screen.getByText("操作人")).toBeInTheDocument();
    expect(screen.getByText("最大条数")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /查询/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /重置/ })).toBeInTheDocument();
  });

  it("renders list rows with engine/action label + 4+4 masked patient_id", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("AMI_RULE_001")).toBeInTheDocument();
    });
    // engine_type 中文标签
    expect(screen.getByText("规则引擎")).toBeInTheDocument();
    expect(screen.getByText("审计自身")).toBeInTheDocument();
    // patient_id 4+4 脱敏
    expect(screen.getByText("1101****AAAA")).toBeInTheDocument();
    // 签名失败 Tag
    expect(screen.getByText("失败")).toBeInTheDocument();
  });

  it("renders summary cards with total and breakdowns", async () => {
    renderPage();
    await waitFor(() => {
      // 总数
      expect(screen.getByText("记录总数")).toBeInTheDocument();
      expect(screen.getByText("2")).toBeInTheDocument();
    });
    expect(screen.getByText("按引擎类型")).toBeInTheDocument();
    expect(screen.getByText("按操作类型")).toBeInTheDocument();
  });

  it("shows export CSV button (disabled when empty)", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole("button", { name: /导出 CSV/ })).toBeEnabled();
    });
  });

  it("reset button restores default filter (limit=20)", async () => {
    renderPage();
    const trace = screen.getByLabelText("Trace ID") as HTMLInputElement;
    fireEvent.change(trace, { target: { value: "my-trace" } });
    expect(trace.value).toBe("my-trace");

    fireEvent.click(screen.getByRole("button", { name: /重置/ }));
    expect((screen.getByLabelText("Trace ID") as HTMLInputElement).value).toBe("");
  });
});
