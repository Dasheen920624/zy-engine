import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import DegradationChainViewer from "../DegradationChainViewer";
import type { AllDegradationChains } from "../../../../api/aiWorkflows";

const CHAINS: AllDegradationChains = {
  RESEARCH: {
    call_type: "RESEARCH",
    chain: "QIANWEN,DEEPSEEK,OLLAMA_LOCAL,LOCAL",
    providers: [
      { provider_type: "QIANWEN", ready: true, status: "READY" },
      { provider_type: "DEEPSEEK", ready: true, status: "READY" },
      { provider_type: "OLLAMA_LOCAL", ready: false, status: "UNAVAILABLE" },
      { provider_type: "LOCAL", ready: true, status: "READY" },
    ],
  },
  EMBEDDING: {
    call_type: "EMBEDDING",
    chain: "QIANWEN,LOCAL",
    providers: [
      { provider_type: "QIANWEN", ready: true, status: "READY" },
      { provider_type: "LOCAL", ready: true, status: "READY" },
    ],
  },
  WORKFLOW: {
    call_type: "WORKFLOW",
    chain: "DIFY,LOCAL",
    providers: [
      { provider_type: "DIFY", ready: false, status: "UNAVAILABLE" },
      { provider_type: "LOCAL", ready: true, status: "READY" },
    ],
  },
};

describe("DegradationChainViewer", () => {
  it("renders Chinese labels for callType", () => {
    render(<DegradationChainViewer chains={CHAINS} />);
    expect(screen.getByText("医学研究")).toBeInTheDocument();
    expect(screen.getByText("向量嵌入")).toBeInTheDocument();
    expect(screen.getByText("多步工作流")).toBeInTheDocument();
  });

  it("renders provider nodes for each chain", () => {
    render(<DegradationChainViewer chains={CHAINS} />);
    expect(screen.getByLabelText("node-QIANWEN")).toBeInTheDocument();
    expect(screen.getByLabelText("node-DEEPSEEK")).toBeInTheDocument();
    expect(screen.getByLabelText("node-DIFY")).toBeInTheDocument();
  });

  it("falls back to chain string when providers array missing", () => {
    const fallback: AllDegradationChains = {
      RERANK: { call_type: "RERANK", chain: "LOCAL", providers: [] },
    };
    render(<DegradationChainViewer chains={fallback} />);
    expect(screen.getByText("二阶重排")).toBeInTheDocument();
    expect(screen.getByLabelText("node-LOCAL")).toBeInTheDocument();
  });
});
