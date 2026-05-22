import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ConfigProvider } from "antd";
import PatientDetail from "../PatientDetail";
import { sampleIdentities, samplePatient, sampleVisits } from "./fixtures";

describe("PatientDetail", () => {
  it("renders master index detail with masked identifiers and visits", () => {
    render(
      <ConfigProvider>
        <PatientDetail
          patient={samplePatient}
          identities={sampleIdentities}
          visits={sampleVisits}
          showSensitive={false}
          onRevealChange={vi.fn()}
          onVerifyIdentity={vi.fn()}
        />
      </ConfigProvider>,
    );

    expect(screen.getByText("P-202605220001")).toBeInTheDocument();
    expect(screen.getAllByText("1101**********1234")[0]).toBeInTheDocument();
    expect(screen.getByText("V-202605220001")).toBeInTheDocument();
  });

  it("requests reveal permission and identity verification", () => {
    const onRevealChange = vi.fn();
    const onVerifyIdentity = vi.fn();
    render(
      <ConfigProvider>
        <PatientDetail
          patient={samplePatient}
          identities={sampleIdentities}
          visits={sampleVisits}
          showSensitive={false}
          onRevealChange={onRevealChange}
          onVerifyIdentity={onVerifyIdentity}
        />
      </ConfigProvider>,
    );

    fireEvent.click(screen.getByLabelText("查看完整敏感信息"));
    fireEvent.click(screen.getByRole("button", { name: "人工核验" }));

    expect(onRevealChange).toHaveBeenCalledWith(true, expect.anything());
    expect(onVerifyIdentity).toHaveBeenCalledWith(101);
  });
});
