import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { StatusBadge, STATUS_MACHINES } from "./StatusBadge";

describe("StatusBadge", () => {
  it("renders config 已发布 label", () => {
    render(<StatusBadge machine="config" status="published" />);
    expect(screen.getByText("已发布")).toBeInTheDocument();
  });

  it("renders alert 已派单 label", () => {
    render(<StatusBadge machine="alert" status="assigned" />);
    expect(screen.getByText("已派单")).toBeInTheDocument();
  });

  it("STATUS_MACHINES contains 4 machines", () => {
    expect(Object.keys(STATUS_MACHINES)).toHaveLength(4);
    expect(STATUS_MACHINES.config).toContain("active");
    expect(STATUS_MACHINES.todo).toContain("escalated");
  });

  it("falls back to 未知状态 for invalid value", () => {
    // @ts-expect-error testing fallback
    render(<StatusBadge machine="config" status="bogus" />);
    expect(screen.getByText(/未知状态/)).toBeInTheDocument();
  });
});
