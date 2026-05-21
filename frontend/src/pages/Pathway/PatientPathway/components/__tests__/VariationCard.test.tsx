import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import VariationCard from "../VariationCard";
import type { PathwayVariationRecord } from "../../../../../api/pathway";

const SAMPLE: PathwayVariationRecord = {
  variation_id: "V1",
  instance_id: "I1",
  pathway_code: "PATH_AMI",
  patient_id: "P-202605210001",
  encounter_id: "E1",
  node_code: "N3",
  variation_type: "SKIP",
  reason: "因合并症跳过 PCI 决策",
  operator_id: "doctor-zhao",
  created_time: "2026-05-21T09:00:00+08:00",
};

describe("VariationCard", () => {
  it("renders variation type label", () => {
    render(<VariationCard variation={SAMPLE} />);
    expect(screen.getByText("跳过节点")).toBeInTheDocument();
  });

  it("renders reason and node tag", () => {
    render(<VariationCard variation={SAMPLE} />);
    expect(screen.getByText("因合并症跳过 PCI 决策")).toBeInTheDocument();
    expect(screen.getByText("节点 N3")).toBeInTheDocument();
  });

  it("renders operator and time in footer", () => {
    render(<VariationCard variation={SAMPLE} />);
    expect(screen.getByText(/doctor-zhao/)).toBeInTheDocument();
    expect(screen.getByText(/2026-05-21/)).toBeInTheDocument();
  });
});
