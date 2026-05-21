import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import PatientPathwayList from "../PatientPathwayList";
import {
  listPatientPathwayInstances,
  type PatientPathwayInstance,
} from "../../../../api/pathway";

vi.mock("../../../../api/pathway", async () => {
  const actual =
    await vi.importActual<typeof import("../../../../api/pathway")>("../../../../api/pathway");
  return {
    ...actual,
    listPatientPathwayInstances: vi.fn(),
  };
});

const SAMPLE: PatientPathwayInstance[] = [
  {
    instance_id: "I-001",
    patient_id: "P-202605210001",
    encounter_id: "E-001",
    pathway_code: "PATH_AMI_STEMI",
    version_no: "1.0.0",
    status: "ACTIVE",
    current_node_code: "N_PCI_DECISION",
    hospital_code: "HOSPITAL_DEMO",
  },
  {
    instance_id: "I-002",
    patient_id: "P-202605210002",
    encounter_id: "E-002",
    pathway_code: "PATH_STROKE_THROMBOLYSIS",
    status: "COMPLETED",
  },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={["/pathway/patients"]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <PatientPathwayList />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("PatientPathwayList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listPatientPathwayInstances).mockResolvedValue(SAMPLE);
  });

  it("renders rows with masked patient ids", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("I-001")).toBeInTheDocument();
    });
    expect(screen.getByText("P-20****0001")).toBeInTheDocument();
    expect(screen.getByText("P-20****0002")).toBeInTheDocument();
  });

  it("renders status tags", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("进行中")).toBeInTheDocument();
      expect(screen.getByText("已完成")).toBeInTheDocument();
    });
  });

  it("renders empty hint when no instances", async () => {
    vi.mocked(listPatientPathwayInstances).mockResolvedValue([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("暂无患者路径实例")).toBeInTheDocument();
    });
  });
});
