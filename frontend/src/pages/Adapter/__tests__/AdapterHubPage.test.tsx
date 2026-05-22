import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import AdapterHubPage from "../AdapterHubPage";
import {
  listAdapterDefinitions,
  listCdsHooksServices,
  listInteropAdapters,
  listSmartApps,
  listTriggerPoints,
} from "../../../api/adapterHub";

vi.mock("../../../api/adapterHub", async () => {
  const actual =
    await vi.importActual<typeof import("../../../api/adapterHub")>(
      "../../../api/adapterHub",
    );
  return {
    ...actual,
    listAdapterDefinitions: vi.fn(),
    listInteropAdapters: vi.fn(),
    listCdsHooksServices: vi.fn(),
    listSmartApps: vi.fn(),
    listTriggerPoints: vi.fn(),
  };
});

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={["/adapter/hub"]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <AdapterHubPage />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("AdapterHubPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listAdapterDefinitions).mockResolvedValue([
      {
        adapter_code: "HIS_OUTPATIENT",
        adapter_name: "门诊主索引",
        adapter_category: "HIS",
        query_code: "GET_LATEST_VISIT",
        query_name: "取最新就诊",
        endpoint_url: "http://his.local/api/visits/latest",
        enabled: true,
      },
    ]);
    vi.mocked(listInteropAdapters).mockResolvedValue([]);
    vi.mocked(listCdsHooksServices).mockResolvedValue([]);
    vi.mocked(listSmartApps).mockResolvedValue([]);
    vi.mocked(listTriggerPoints).mockResolvedValue([]);
  });

  it("renders page title and ADAPT-001 / INTEROP-001 hint", () => {
    renderPage();
    expect(screen.getByText("适配器中心")).toBeInTheDocument();
    expect(screen.getByText(/ADAPT-001 \/ INTEROP-001/)).toBeInTheDocument();
  });

  it("renders 3 tabs", () => {
    renderPage();
    expect(screen.getByRole("tab", { name: /业务适配器/ })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /互联互通/ })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /CDSS 触发点/ })).toBeInTheDocument();
  });

  it("renders business adapter row with HIS category tag and 中文 query name", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("HIS_OUTPATIENT")).toBeInTheDocument();
    });
    expect(screen.getByText("门诊主索引")).toBeInTheDocument();
    expect(screen.getByText("HIS 医院信息系统")).toBeInTheDocument();
    expect(screen.getByText("取最新就诊")).toBeInTheDocument();
  });
});
