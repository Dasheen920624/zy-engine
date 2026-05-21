import { describe, expect, it } from "vitest";
import { describeDiffItem, diffTotals, pickDiffSection } from "../pathwayDiff";
import type { PathwayDiffResult } from "../../../../api/pathway";

const SAMPLE_DIFF: PathwayDiffResult = {
  pathway_code: "PATH_AMI_STEMI",
  from_version: "1.0.0",
  to_version: "2.0.0",
  nodes_added: [{ node_code: "N3", node_name: "PCI 决策" }],
  nodes_removed: [],
  nodes_modified: [{ node_code: "N1", node_name: "急诊接诊" }],
  edges_added: [{ edge_code: "E1->N3" }],
  edges_removed: [],
  edges_modified: [],
  tasks_added: [],
  tasks_removed: [{ task_code: "T_X" }],
  tasks_modified: [{ task_code: "T_ECG" }],
  summary: {
    nodes_added: 1,
    nodes_removed: 0,
    nodes_modified: 1,
    edges_added: 1,
    edges_removed: 0,
    edges_modified: 0,
    tasks_added: 0,
    tasks_removed: 1,
    tasks_modified: 1,
  },
};

describe("pathwayDiff.pickDiffSection", () => {
  it("picks nodes section", () => {
    const s = pickDiffSection(SAMPLE_DIFF, "nodes");
    expect(s.added).toHaveLength(1);
    expect(s.modified).toHaveLength(1);
    expect(s.removed).toHaveLength(0);
  });
  it("returns empty arrays when diff is undefined", () => {
    const s = pickDiffSection(undefined, "tasks");
    expect(s).toEqual({ added: [], removed: [], modified: [] });
  });
});

describe("pathwayDiff.diffTotals", () => {
  it("uses summary when present", () => {
    expect(diffTotals(SAMPLE_DIFF)).toEqual({ nodes: 2, edges: 1, tasks: 2 });
  });
  it("computes from arrays when summary absent", () => {
    const withoutSummary: PathwayDiffResult = {
      ...SAMPLE_DIFF,
      summary: undefined,
    };
    expect(diffTotals(withoutSummary)).toEqual({ nodes: 2, edges: 1, tasks: 2 });
  });
  it("returns zeros when diff is undefined", () => {
    expect(diffTotals(undefined)).toEqual({ nodes: 0, edges: 0, tasks: 0 });
  });
});

describe("pathwayDiff.describeDiffItem", () => {
  it("uses node_code + node_name when present", () => {
    expect(describeDiffItem({ node_code: "N1", node_name: "接诊" })).toBe("N1 · 接诊");
  });
  it("falls back to code only", () => {
    expect(describeDiffItem({ task_code: "T_ECG" })).toBe("T_ECG");
  });
  it("returns (unknown) when no identifier", () => {
    expect(describeDiffItem({})).toBe("(unknown)");
  });
});
