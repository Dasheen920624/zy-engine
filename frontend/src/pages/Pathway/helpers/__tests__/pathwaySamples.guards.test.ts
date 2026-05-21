import { describe, expect, it } from "vitest";
import { safeParseJson } from "../pathwaySamples.guards";

describe("pathwaySamples.guards.safeParseJson", () => {
  it("parses valid JSON", () => {
    const result = safeParseJson('{"x":1}');
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.value).toEqual({ x: 1 });
  });

  it("returns error for invalid JSON", () => {
    const result = safeParseJson("not-json");
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error.length).toBeGreaterThan(0);
  });
});
