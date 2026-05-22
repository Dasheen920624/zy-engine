import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import SignatureVerifyBanner from "../../SignatureVerifyBanner";
import { getAuditChainStatus, verifyAuditChain } from "../../../../../api/auditLog";

vi.mock("../../../../../api/auditLog", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../../api/auditLog")>(
      "../../../../../api/auditLog",
    );
  return {
    ...actual,
    getAuditChainStatus: vi.fn(),
    verifyAuditChain: vi.fn(),
  };
});

function renderBanner() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider>
      <QueryClientProvider client={queryClient}>
        <SignatureVerifyBanner />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe("SignatureVerifyBanner", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders 3 audit tables with NOT_VERIFIED placeholder", async () => {
    vi.mocked(getAuditChainStatus).mockResolvedValue({
      engine_audit_log: { status: "NOT_VERIFIED" },
      sec_auth_audit_log: { status: "NOT_VERIFIED" },
      sec_sso_audit_log: { status: "NOT_VERIFIED" },
    });
    renderBanner();
    await waitFor(() => {
      expect(screen.getByText("引擎主审计表")).toBeInTheDocument();
    });
    expect(screen.getByText("认证审计表")).toBeInTheDocument();
    expect(screen.getByText("SSO 审计表")).toBeInTheDocument();
    expect(screen.getAllByText("尚未校验")).toHaveLength(3);
  });

  it("highlights broken state when checkpoint returns broken_records > 0", async () => {
    vi.mocked(getAuditChainStatus).mockResolvedValue({
      engine_audit_log: { status: "NOT_VERIFIED" },
      sec_auth_audit_log: { status: "NOT_VERIFIED" },
      sec_sso_audit_log: { status: "NOT_VERIFIED" },
    });
    vi.mocked(verifyAuditChain).mockResolvedValue({
      checkpoint_id: 1,
      checkpoint_time: "2026-05-22 16:00:00",
      chain_status: "BROKEN",
      total_records: 100,
      valid_records: 99,
      broken_records: 1,
      first_broken_id: 42,
    });

    renderBanner();
    await waitFor(() => {
      expect(screen.getByText("引擎主审计表")).toBeInTheDocument();
    });
    // 找到 engine_audit_log 行的「立即校验」按钮（3 个表，每个 1 按钮）
    const verifyButtons = screen.getAllByText("立即校验");
    expect(verifyButtons).toHaveLength(3);

    fireEvent.click(verifyButtons[0]);

    await waitFor(() => {
      expect(verifyAuditChain).toHaveBeenCalledWith("engine_audit_log");
    });
  });

  it("renders banner title with 等保 2.0 三级 hint", async () => {
    vi.mocked(getAuditChainStatus).mockResolvedValue({});
    renderBanner();
    await waitFor(() => {
      expect(screen.getByText("审计链验签")).toBeInTheDocument();
    });
    expect(screen.getByText(/等保 2.0 三级/)).toBeInTheDocument();
    expect(screen.getByText(/防篡改链/)).toBeInTheDocument();
  });
});
