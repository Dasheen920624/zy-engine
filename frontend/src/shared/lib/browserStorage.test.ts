import { describe, expect, it, beforeEach } from "vitest";
import { readUiPreference, writeUiPreference } from "./browserStorage";

describe("browserStorage", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("allows whitelisted UI preference keys", () => {
    writeUiPreference("medkernel.theme.mode", "elder");
    writeUiPreference("medkernel.view.pathways", JSON.stringify({ visible: ["name"] }));

    expect(readUiPreference("medkernel.theme.mode")).toBe("elder");
    expect(readUiPreference("medkernel.view.pathways")).toBe(JSON.stringify({ visible: ["name"] }));
  });

  it("rejects sensitive or unapproved keys", () => {
    expect(() => writeUiPreference("medkernel.token", "secret-token")).toThrow(/敏感/);
    expect(() => writeUiPreference("medkernel.patient.snapshot", "{}")).toThrow(/敏感/);
    expect(() => writeUiPreference("random.ui.state", "value")).toThrow(/允许/);
  });
});
