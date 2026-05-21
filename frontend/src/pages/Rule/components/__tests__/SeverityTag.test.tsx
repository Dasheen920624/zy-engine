import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import SeverityTag from "../SeverityTag";

describe("SeverityTag", () => {
  it("renders Chinese label for HIGH", () => {
    render(<SeverityTag severity="HIGH" />);
    expect(screen.getByText("高")).toBeInTheDocument();
  });

  it("renders dash for undefined", () => {
    render(<SeverityTag severity={undefined} />);
    expect(screen.getByText("—")).toBeInTheDocument();
  });
});
