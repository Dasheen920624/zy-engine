import { describe, expect, it } from "vitest";
import {
  describeInstanceStatus,
  describeNodeStatus,
  describeTaskStatus,
  describeVariationType,
  describeConfidence,
  formatDurationHours,
  formatPercent,
  formatTimestamp,
  maskPatientId,
  stringifyJson,
} from "../pathwayFormatters";

describe("pathwayFormatters.describe*", () => {
  it("describes instance status with Chinese labels", () => {
    expect(describeInstanceStatus("ACTIVE")).toBe("进行中");
    expect(describeInstanceStatus("COMPLETED")).toBe("已完成");
    expect(describeInstanceStatus(undefined)).toBe("—");
  });

  it("describes node status", () => {
    expect(describeNodeStatus("PENDING")).toBe("待进入");
    expect(describeNodeStatus("ACTIVE")).toBe("进行中");
    expect(describeNodeStatus("BLOCKED")).toBe("阻塞");
  });

  it("describes task status", () => {
    expect(describeTaskStatus("COMPLETED")).toBe("已完成");
    expect(describeTaskStatus("FAILED")).toBe("失败");
  });

  it("describes variation type", () => {
    expect(describeVariationType("SKIP")).toBe("跳过节点");
    expect(describeVariationType("ROLLBACK")).toBe("回退");
  });

  it("describes confidence range", () => {
    expect(describeConfidence(0.95)).toBe("高");
    expect(describeConfidence(0.6)).toBe("中");
    expect(describeConfidence(0.1)).toBe("低");
    expect(describeConfidence(undefined)).toBe("—");
  });
});

describe("pathwayFormatters.formatTimestamp", () => {
  it("formats ISO timestamp", () => {
    expect(formatTimestamp("2026-05-21T10:30:00+08:00")).toMatch(/2026-05-21/);
  });
  it("returns dash for empty", () => {
    expect(formatTimestamp(undefined)).toBe("—");
    expect(formatTimestamp(null)).toBe("—");
  });
  it("returns raw when not parseable", () => {
    expect(formatTimestamp("not-a-date")).toBe("not-a-date");
  });
});

describe("pathwayFormatters.formatDurationHours", () => {
  it("formats sub-hour in minutes", () => {
    expect(formatDurationHours(0.5)).toBe("30 分");
  });
  it("formats hours range", () => {
    expect(formatDurationHours(5.5)).toBe("5.5 时");
  });
  it("formats days range", () => {
    expect(formatDurationHours(48)).toBe("2.0 天");
  });
  it("returns dash for nullish", () => {
    expect(formatDurationHours(undefined)).toBe("—");
    expect(formatDurationHours(null)).toBe("—");
  });
});

describe("pathwayFormatters.maskPatientId", () => {
  it("masks middle when length > 8", () => {
    expect(maskPatientId("P-202605210001")).toBe("P-20****0001");
  });
  it("keeps short ids unmasked", () => {
    expect(maskPatientId("P12345")).toBe("P12345");
  });
  it("returns dash for empty", () => {
    expect(maskPatientId(undefined)).toBe("—");
    expect(maskPatientId(null)).toBe("—");
  });
});

describe("pathwayFormatters.formatPercent", () => {
  it("formats ratio to percent", () => {
    expect(formatPercent(0.756)).toBe("75.6%");
    expect(formatPercent(0.5, 0)).toBe("50%");
  });
  it("returns dash for nullish or NaN", () => {
    expect(formatPercent(undefined)).toBe("—");
    expect(formatPercent(Number.NaN)).toBe("—");
  });
});

describe("pathwayFormatters.stringifyJson", () => {
  it("formats with 2-space indent", () => {
    expect(stringifyJson({ a: 1 })).toBe(`{\n  "a": 1\n}`);
  });
  it("handles null/undefined", () => {
    expect(stringifyJson(null)).toBe("{}");
    expect(stringifyJson(undefined)).toBe("{}");
  });
});
