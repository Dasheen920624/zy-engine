import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { useThemeStore } from "@/shared/lib/themeStore";

import { ThemeSwitcher } from "./ThemeSwitcher";

describe("ThemeSwitcher", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useThemeStore.setState({ mode: "default" });
  });

  it("点击后展开主题菜单并写入选择结果", async () => {
    render(<ThemeSwitcher />);

    fireEvent.click(screen.getByRole("button", { name: "主题模式：默认" }));
    fireEvent.click(await screen.findByText("暗黑模式"));

    await waitFor(() => expect(useThemeStore.getState().mode).toBe("dark"));
    expect(window.localStorage.getItem("medkernel.theme.mode")).toBe("dark");
  });
});
