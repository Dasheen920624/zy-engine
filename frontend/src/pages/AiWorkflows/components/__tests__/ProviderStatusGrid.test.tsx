import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ProviderStatusGrid from "../ProviderStatusGrid";

const SAMPLE = [
  { provider_type: "QIANWEN", ready: true, status: "READY" },
  { provider_type: "DEEPSEEK", ready: false, status: "UNAVAILABLE", reason: "API key 未配置" },
  { provider_type: "OLLAMA_LOCAL", ready: false, status: "UNAVAILABLE" },
  { provider_type: "LOCAL", ready: true, status: "READY" },
  { provider_type: "DIFY", ready: false, status: "UNAVAILABLE" },
];

describe("ProviderStatusGrid", () => {
  it("renders 3 groups: domestic / local / workflow", () => {
    render(<ProviderStatusGrid providers={SAMPLE} />);
    expect(screen.getByText(/国产大模型直连/)).toBeInTheDocument();
    expect(screen.getByText(/本地与兜底/)).toBeInTheDocument();
    expect(screen.getByText(/工作流编排/)).toBeInTheDocument();
  });

  it("renders all provider names", () => {
    render(<ProviderStatusGrid providers={SAMPLE} />);
    expect(screen.getByText("通义千问")).toBeInTheDocument();
    expect(screen.getByText("DeepSeek")).toBeInTheDocument();
    expect(screen.getByText("Ollama 本地")).toBeInTheDocument();
    expect(screen.getByText("LOCAL 规则兜底")).toBeInTheDocument();
    expect(screen.getByText("Dify（仅 WORKFLOW）")).toBeInTheDocument();
  });

  it("renders empty hint when no providers", () => {
    render(<ProviderStatusGrid providers={[]} />);
    expect(screen.getByText(/无 Provider 注册/)).toBeInTheDocument();
  });
});
