import { describe, it, expect, beforeEach } from "vitest";
import { useThemeStore } from "./themeStore";

describe("themeStore", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useThemeStore.setState({ mode: "default" });
  });

  it("default mode is default", () => {
    expect(useThemeStore.getState().mode).toBe("default");
  });

  it("setMode persists to localStorage", () => {
    useThemeStore.getState().setMode("elder");
    expect(useThemeStore.getState().mode).toBe("elder");
    expect(window.localStorage.getItem("medkernel.theme.mode")).toBe("elder");
  });

  it("setMode dark switches to darkAlgorithm-compatible mode", () => {
    useThemeStore.getState().setMode("dark");
    expect(useThemeStore.getState().mode).toBe("dark");
  });
});
