import { describe, it, expect, vi } from "vitest";
import { render } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import UnsavedChangesGuard from "../UnsavedChangesGuard";

// Mock react-router-dom useBlocker
const mockBlocker = { state: "unblocked", proceed: vi.fn(), reset: vi.fn() };
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useBlocker: () => mockBlocker,
  };
});

describe("UnsavedChangesGuard", () => {
  it("应正常渲染（返回 null）", () => {
    const { container } = render(
      <BrowserRouter>
        <UnsavedChangesGuard dirty={false} />
      </BrowserRouter>
    );
    expect(container.innerHTML).toBe("");
  });

  it("在 dirty=true 时也应正常渲染", () => {
    const { container } = render(
      <BrowserRouter>
        <UnsavedChangesGuard dirty={true} />
      </BrowserRouter>
    );
    expect(container.innerHTML).toBe("");
  });
});
