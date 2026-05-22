import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import NotFound from "./NotFound";

describe("NotFound", () => {
  it("应渲染 404 页面", () => {
    render(
      <BrowserRouter>
        <NotFound />
      </BrowserRouter>
    );
    expect(screen.getByText("404")).toBeTruthy();
  });

  it("应包含返回首页链接", () => {
    render(
      <BrowserRouter>
        <NotFound />
      </BrowserRouter>
    );
    const link = screen.getByRole("link", { name: /首页|返回/i });
    expect(link).toBeTruthy();
  });
});
