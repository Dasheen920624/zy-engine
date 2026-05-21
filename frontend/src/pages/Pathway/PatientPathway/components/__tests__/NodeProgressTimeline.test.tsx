import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import NodeProgressTimeline from "../NodeProgressTimeline";
import type { PatientNodeState } from "../../../../../api/pathway";

const NODES: PatientNodeState[] = [
  {
    instance_id: "I1",
    node_code: "N1",
    node_name: "急诊接诊",
    status: "COMPLETED",
    enter_time: "2026-05-21T08:00:00+08:00",
    complete_time: "2026-05-21T08:10:00+08:00",
  },
  {
    instance_id: "I1",
    node_code: "N2",
    node_name: "ECG",
    status: "ACTIVE",
    enter_time: "2026-05-21T08:10:00+08:00",
    timeout_flag: true,
  },
  {
    instance_id: "I1",
    node_code: "N3",
    node_name: "PCI 决策",
    status: "PENDING",
  },
];

describe("NodeProgressTimeline", () => {
  it("renders all nodes with status labels", () => {
    render(<NodeProgressTimeline nodes={NODES} currentNodeCode="N2" />);
    expect(screen.getByText("急诊接诊")).toBeInTheDocument();
    expect(screen.getByText("ECG")).toBeInTheDocument();
    expect(screen.getByText("PCI 决策")).toBeInTheDocument();
    expect(screen.getByText("已完成")).toBeInTheDocument();
    expect(screen.getByText("进行中")).toBeInTheDocument();
    expect(screen.getByText("待进入")).toBeInTheDocument();
  });

  it("marks timeout node visibly", () => {
    render(<NodeProgressTimeline nodes={NODES} currentNodeCode="N2" />);
    expect(screen.getByText(/超时/)).toBeInTheDocument();
  });

  it("renders empty hint when nodes is empty", () => {
    render(<NodeProgressTimeline nodes={[]} />);
    expect(screen.getByText("暂无节点状态")).toBeInTheDocument();
  });
});
