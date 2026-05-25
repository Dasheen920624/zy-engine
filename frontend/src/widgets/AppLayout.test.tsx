import { fireEvent, render, screen } from "@testing-library/react";
import { ConfigProvider } from "antd";
import { afterEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AppLayout } from "./AppLayout";

const originalInnerWidth = window.innerWidth;
const originalMatchMedia = window.matchMedia;

function mockViewport(width: number) {
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width,
  });

  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    writable: true,
    value: (query: string) => ({
      matches: matchesMediaQuery(query, width),
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}

function matchesMediaQuery(query: string, width: number) {
  const minWidth = query.match(/min-width:\s*(\d+)px/);
  const maxWidth = query.match(/max-width:\s*(\d+)px/);
  if (minWidth && width < Number(minWidth[1])) {
    return false;
  }
  if (maxWidth && width > Number(maxWidth[1])) {
    return false;
  }
  return Boolean(minWidth || maxWidth);
}

function renderLayout(initialPath = "/terminology/mapping") {
  return render(
    <ConfigProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/terminology/mapping" element={<div>字典映射内容</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </ConfigProvider>,
  );
}

afterEach(() => {
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: originalInnerWidth,
  });
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    writable: true,
    value: originalMatchMedia,
  });
});

describe("AppLayout", () => {
  it("renders route title and metadata-backed side menu", () => {
    mockViewport(1280);
    renderLayout();

    expect(screen.getAllByText("字典映射").length).toBeGreaterThan(0);
    expect(screen.getAllByText("试点准备").length).toBeGreaterThan(0);
    expect(screen.getByText("字典映射内容")).toBeInTheDocument();
  });

  it("uses drawer navigation on mobile width", () => {
    mockViewport(390);
    renderLayout();

    expect(document.querySelector(".ant-layout-sider")).toBeNull();
    expect(screen.getByText("字典映射内容")).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole("button")[0]);

    expect(screen.getAllByText("试点准备").length).toBeGreaterThan(0);
  });
});
