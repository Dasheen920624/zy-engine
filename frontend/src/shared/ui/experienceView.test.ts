import { beforeEach, describe, expect, it } from "vitest";

import {
  buildAsyncExportRequest,
  normalizePageResponse,
  readExperienceView,
  writeExperienceView,
} from "./experienceView";

const snapshot = {
  viewKey: "terminology.mapping",
  filters: [{ key: "status", value: "DRAFT" }],
  pageRequest: {
    pageNumber: 1,
    pageSize: 20,
    sortBy: "updatedAt",
    sortOrder: "desc",
    filters: { status: "DRAFT" },
  },
  visibleColumnKeys: ["sourceSystem", "status"],
  expertMode: false,
  capturedAt: "2026-05-26T00:00:00.000Z",
} as const;

describe("experienceView", () => {
  beforeEach(() => window.localStorage.clear());

  it("normalizes current PageResponse into the experience pagination contract", () => {
    const result = normalizePageResponse({
      items: [{ id: 1 }],
      page: 2,
      size: 20,
      total: 41,
      hasNext: true,
      totalEstimated: false,
      traceId: "trace-1",
    });

    expect(result).toMatchObject({
      items: [{ id: 1 }],
      pageNumber: 2,
      pageSize: 20,
      totalEstimate: 41,
      hasMore: true,
      traceId: "trace-1",
    });
  });

  it("stores reproducible UI view snapshots without sensitive content", () => {
    writeExperienceView("terminology.mapping", snapshot);

    expect(readExperienceView("terminology.mapping")?.visibleColumnKeys).toEqual([
      "sourceSystem",
      "status",
    ]);
    expect(readExperienceView("terminology.mapping")?.expertMode).toBe(false);
    expect(window.localStorage.getItem("medkernel.view.terminology.mapping")).toContain(
      "updatedAt",
    );
  });

  it("rejects sensitive snapshot keys before writing local storage", () => {
    expect(() =>
      writeExperienceView("terminology.mapping", {
        ...snapshot,
        pageRequest: {
          ...snapshot.pageRequest,
          filters: { patientId: "p-1" },
        },
      }),
    ).toThrow(/敏感/);
    expect(window.localStorage.getItem("medkernel.view.terminology.mapping")).toBeNull();
  });

  it("builds auditable export requests from view and selection snapshots", () => {
    const request = buildAsyncExportRequest({
      resourceType: "terminology.mapping",
      requestSnapshot: snapshot,
      selectedScope: "currentPage",
      selectionSnapshot: { selectedRowKeys: [1, 2], rowCount: 2 },
      reason: "导出当前页用于实施核查",
    });

    expect(request.requestSnapshot.pageRequest).toMatchObject({
      pageNumber: 1,
      pageSize: 20,
      sortBy: "updatedAt",
      sortOrder: "desc",
    });
    expect(request.selectionSnapshot?.selectedRowKeys).toEqual([1, 2]);
    expect(request.reason).toBe("导出当前页用于实施核查");
  });
});
