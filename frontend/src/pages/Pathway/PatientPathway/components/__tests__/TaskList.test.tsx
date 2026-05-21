import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import TaskList from "../TaskList";
import type { PatientTaskState } from "../../../../../api/pathway";

const TASKS: PatientTaskState[] = [
  {
    instance_id: "I1",
    node_code: "N1",
    task_code: "T_ECG",
    task_name: "12 导联心电图",
    required: true,
    status: "PENDING",
  },
  {
    instance_id: "I1",
    node_code: "N1",
    task_code: "T_DAPT",
    task_name: "双联抗血小板",
    required: false,
    status: "COMPLETED",
  },
];

describe("TaskList", () => {
  it("renders task names and labels required with ★", () => {
    render(<TaskList tasks={TASKS} />);
    expect(screen.getByText(/12 导联心电图/)).toBeInTheDocument();
    expect(screen.getByText("双联抗血小板")).toBeInTheDocument();
    expect(screen.getByText(/★ 12 导联心电图/)).toBeInTheDocument();
  });

  it("does not show complete/skip buttons for final state tasks", () => {
    render(<TaskList tasks={TASKS} onComplete={vi.fn()} onSkip={vi.fn()} />);
    expect(screen.queryByLabelText("complete-T_DAPT")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("skip-T_DAPT")).not.toBeInTheDocument();
    expect(screen.getByLabelText("complete-T_ECG")).toBeInTheDocument();
    expect(screen.getByLabelText("skip-T_ECG")).toBeInTheDocument();
  });

  it("calls onComplete with task", async () => {
    const onComplete = vi.fn();
    const user = userEvent.setup();
    render(<TaskList tasks={TASKS} onComplete={onComplete} onSkip={vi.fn()} />);
    await user.click(screen.getByLabelText("complete-T_ECG"));
    expect(onComplete).toHaveBeenCalledTimes(1);
    expect(onComplete.mock.calls[0][0].task_code).toBe("T_ECG");
  });

  it("renders empty hint when no tasks", () => {
    render(<TaskList tasks={[]} />);
    expect(screen.getByText("该节点暂无任务")).toBeInTheDocument();
  });
});
