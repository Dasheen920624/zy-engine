import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfigProvider } from "antd";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";

import { ExperienceFilterBar } from "./ExperienceFilterBar";
import type { ExperienceFilterDefinition, ExperienceFilterValue } from "./experienceTypes";

const filters: ExperienceFilterDefinition[] = [
  {
    key: "status",
    label: "映射状态",
    kind: "select",
    options: [{ label: "草稿", value: "DRAFT" }],
  },
  { key: "sourceSystem", label: "来源系统", kind: "search" },
  { key: "keyword", label: "关键词", kind: "search", placeholder: "输入编码关键词" },
];

function renderFilterBar(onChange = vi.fn()) {
  function FilterBarHarness() {
    const [value, setValue] = useState<ExperienceFilterValue[]>([]);

    function handleChange(next: ExperienceFilterValue[]) {
      setValue(next);
      onChange(next);
    }

    return <ExperienceFilterBar filters={filters} value={value} onChange={handleChange} />;
  }

  return render(
    <ConfigProvider>
      <FilterBarHarness />
    </ConfigProvider>,
  );
}

describe("ExperienceFilterBar", () => {
  it("rejects more than three default filters", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    expect(() =>
      render(
        <ExperienceFilterBar
          filters={[...filters, { key: "category", label: "类别", kind: "search" }]}
          value={[]}
          onChange={vi.fn()}
        />,
      ),
    ).toThrow(/最多 3 个/);
    vi.restoreAllMocks();
  });

  it("requires a controlled option source for select filters", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    expect(() =>
      render(
        <ExperienceFilterBar
          filters={[{ key: "status", label: "状态", kind: "select" }]}
          value={[]}
          onChange={vi.fn()}
        />,
      ),
    ).toThrow(/选项来源/);
    vi.restoreAllMocks();
  });

  it("uses Chinese placeholders and emits search changes", async () => {
    const onChange = vi.fn();
    renderFilterBar(onChange);

    const search = screen.getByPlaceholderText("请输入来源系统");
    await userEvent.type(search, "HIS");

    expect(screen.getByPlaceholderText("输入编码关键词")).toBeInTheDocument();
    expect(onChange).toHaveBeenLastCalledWith([{ key: "sourceSystem", value: "HIS" }]);
  });
});
