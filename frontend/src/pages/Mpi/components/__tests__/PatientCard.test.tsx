import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ConfigProvider } from "antd";
import PatientCard from "../PatientCard";
import { samplePatient } from "../../__tests__/fixtures";

describe("PatientCard", () => {
  it("renders masked patient identifiers by default", () => {
    render(
      <ConfigProvider>
        <PatientCard patient={samplePatient} />
      </ConfigProvider>,
    );

    expect(screen.getByText("1101**********1234")).toBeInTheDocument();
    expect(screen.getByText("138****5678")).toBeInTheDocument();
    expect(screen.getByText("汉族")).toBeInTheDocument();
  });

  it("calls onSelect when opening the master index", () => {
    const onSelect = vi.fn();
    render(
      <ConfigProvider>
        <PatientCard patient={samplePatient} onSelect={onSelect} />
      </ConfigProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "查看主索引" }));
    expect(onSelect).toHaveBeenCalledWith("P-202605220001");
  });
});
