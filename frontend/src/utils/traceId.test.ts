import { describe, expect, it } from "vitest";
import { generateTraceId } from "./traceId";

describe("generateTraceId", () => {
  it("returns id with fe- prefix and dash-separated segments", () => {
    const id = generateTraceId();
    expect(id).toMatch(/^fe-[0-9a-f]+-[0-9a-f]+$/);
  });

  it("returns unique ids on subsequent calls", () => {
    const a = generateTraceId();
    const b = generateTraceId();
    expect(a).not.toBe(b);
  });
});
