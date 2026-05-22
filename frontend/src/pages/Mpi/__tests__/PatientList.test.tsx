import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ConfigProvider } from "antd";
import PatientList from "../PatientList";
import { samplePatient } from "./fixtures";

vi.mock("antd", async (importOriginal) => {
  const actual = (await importOriginal()) as object;
  return {
    ...actual,
    Select: ({ value, options = [], onChange, placeholder, "aria-label": ariaLabel }: {
      value?: string;
      options?: Array<{ value: string; label: string }>;
      onChange?: (value: string | undefined) => void;
      placeholder?: string;
      "aria-label"?: string;
    }) => (
      <select
        aria-label={ariaLabel}
        value={value ?? ""}
        onChange={(event) => onChange?.(event.target.value || undefined)}
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

describe("PatientList", () => {
  it("renders patient list cards and result count", () => {
    render(
      <ConfigProvider>
        <PatientList
          patients={[samplePatient]}
          selectedPatientId="P-202605220001"
          onSearch={vi.fn()}
          onSelectPatient={vi.fn()}
        />
      </ConfigProvider>,
    );

    expect(screen.getByText("患者主索引")).toBeInTheDocument();
    expect(screen.getByText("共 1 条")).toBeInTheDocument();
    expect(screen.getByText("P-202605220001")).toBeInTheDocument();
  });

  it("submits search payload from the keyword input", () => {
    const onSearch = vi.fn();
    render(
      <ConfigProvider>
        <PatientList
          patients={[]}
          onSearch={onSearch}
          onSelectPatient={vi.fn()}
        />
      </ConfigProvider>,
    );

    fireEvent.change(screen.getByLabelText("患者搜索关键字"), {
      target: { value: "P-202605220001" },
    });
    fireEvent.click(screen.getByRole("button", { name: /搜索/ }));

    expect(onSearch).toHaveBeenCalledWith({
      keyword: "P-202605220001",
      identityType: "PLATFORM_PATIENT_ID",
      sourceSystem: "HIS",
    });
  });
});
