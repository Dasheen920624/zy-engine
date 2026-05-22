import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ConfigProvider } from "antd";
import MergeWorkbench from "../MergeWorkbench";
import { sampleConflicts, sampleIdentities } from "./fixtures";

vi.mock("antd", async (importOriginal) => {
  const actual = (await importOriginal()) as object;
  return {
    ...actual,
    Select: ({ value, options = [], onChange, placeholder, "aria-label": ariaLabel }: {
      value?: string | number;
      options?: Array<{ value: string | number; label: string }>;
      onChange?: (value: string | number | undefined) => void;
      placeholder?: string;
      "aria-label"?: string;
    }) => (
      <select
        aria-label={ariaLabel}
        value={value ?? ""}
        onChange={(event) => {
          const option = options.find((item) => String(item.value) === event.target.value);
          onChange?.(option?.value);
        }}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    ),
  };
});

describe("MergeWorkbench", () => {
  it("renders pending conflict with severity and description", () => {
    render(
      <ConfigProvider>
        <MergeWorkbench
          conflicts={sampleConflicts}
          identities={sampleIdentities}
          onDetect={vi.fn()}
          onResolve={vi.fn()}
        />
      </ConfigProvider>,
    );

    expect(screen.getByText("冲突合并工作台")).toBeInTheDocument();
    expect(screen.getByText("HIGH")).toBeInTheDocument();
    expect(screen.getAllByText("同一身份证命中两个平台患者 ID")[0]).toBeInTheDocument();
  });

  it("runs conflict detection from the toolbar", () => {
    const onDetect = vi.fn();
    render(
      <ConfigProvider>
        <MergeWorkbench
          conflicts={sampleConflicts}
          identities={sampleIdentities}
          onDetect={onDetect}
          onResolve={vi.fn()}
        />
      </ConfigProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: /重新检测/ }));
    expect(onDetect).toHaveBeenCalled();
  });
});
