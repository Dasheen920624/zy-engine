import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ProviderStatusCard from "../ProviderStatusCard";

describe("ProviderStatusCard", () => {
  it("renders Chinese label for known provider type", () => {
    render(
      <ProviderStatusCard
        info={{ provider_type: "QIANWEN", ready: true, status: "READY" }}
      />,
    );
    expect(screen.getByText("通义千问")).toBeInTheDocument();
    expect(screen.getByText("就绪")).toBeInTheDocument();
  });

  it("renders reason when degraded", () => {
    render(
      <ProviderStatusCard
        info={{
          provider_type: "DIFY",
          ready: false,
          status: "UNAVAILABLE",
          reason: "DIFY API key 未配置",
        }}
      />,
    );
    expect(screen.getByText("Dify（仅 WORKFLOW）")).toBeInTheDocument();
    expect(screen.getByText("不可用")).toBeInTheDocument();
    expect(screen.getByText("DIFY API key 未配置")).toBeInTheDocument();
  });

  it("renders raw type when unknown", () => {
    render(
      <ProviderStatusCard
        info={{ provider_type: "FUTURE_PROVIDER", ready: false, status: "NOT_FOUND" }}
      />,
    );
    expect(screen.getAllByText("FUTURE_PROVIDER").length).toBeGreaterThan(0);
    expect(screen.getByText("未注册")).toBeInTheDocument();
  });
});
