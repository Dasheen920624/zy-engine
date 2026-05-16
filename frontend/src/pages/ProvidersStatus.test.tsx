import { describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import ProvidersStatus from "./ProvidersStatus";

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <ProvidersStatus />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("ProvidersStatus page", () => {
  it("renders providers list returned by MSW mock", async () => {
    renderPage();

    // 标题应当立即出现
    expect(screen.getByText("Provider 运行状态")).toBeInTheDocument();

    // MSW 返回 4 个 provider，等待表格 render
    await waitFor(() => {
      expect(screen.getByText("Database")).toBeInTheDocument();
      expect(screen.getByText("Graph")).toBeInTheDocument();
      expect(screen.getByText("Dify")).toBeInTheDocument();
      expect(screen.getByText("Adapter")).toBeInTheDocument();
    });

    // 降级原因应被展示
    expect(
      screen.getByText(/Neo4j 不可达/),
    ).toBeInTheDocument();

    // 运行模式标签
    expect(screen.getByText(/HYBRID/)).toBeInTheDocument();
  });
});
