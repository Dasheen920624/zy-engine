import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ExecLogTimeline from "../ExecLogTimeline";
import type { RuleExecLog } from "../../../../api/rule";

const sampleLogs: RuleExecLog[] = [
  {
    log_id: "L1",
    trace_id: "T-1",
    rule_code: "R1",
    hit: true,
    severity: "HIGH",
    message: "命中：QT 延长",
    elapsed_ms: 32,
    result_status: "SUCCESS",
    created_time: "2026-05-21T08:00:00+08:00",
  },
  {
    log_id: "L2",
    trace_id: "T-2",
    rule_code: "R1",
    hit: false,
    elapsed_ms: 12,
    result_status: "SUCCESS",
    created_time: "2026-05-21T09:00:00+08:00",
  },
  {
    log_id: "L3",
    trace_id: "T-3",
    rule_code: "R1",
    hit: false,
    elapsed_ms: 0,
    result_status: "ERROR",
    error_code: "ADAPTER_TIMEOUT",
    error_message: "适配器超时",
    created_time: "2026-05-21T10:00:00+08:00",
  },
];

describe("ExecLogTimeline", () => {
  it("renders empty hint when logs is empty", () => {
    render(<ExecLogTimeline logs={[]} />);
    expect(screen.getByText("暂无触发记录")).toBeInTheDocument();
  });

  it("renders custom empty text when provided", () => {
    render(<ExecLogTimeline logs={[]} emptyText="无数据" />);
    expect(screen.getByText("无数据")).toBeInTheDocument();
  });

  it("renders hit / miss / error verdicts and trace ids", () => {
    render(<ExecLogTimeline logs={sampleLogs} />);
    expect(screen.getByText("命中")).toBeInTheDocument();
    expect(screen.getByText("未命中")).toBeInTheDocument();
    expect(screen.getByText("异常")).toBeInTheDocument();
    expect(screen.getByText(/trace: T-1/)).toBeInTheDocument();
    expect(screen.getByText("适配器超时")).toBeInTheDocument();
  });
});
