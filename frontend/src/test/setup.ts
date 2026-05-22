import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll, vi } from "vitest";
import { server } from "../mocks/server";

const originalGetComputedStyle = window.getComputedStyle.bind(window);
const originalElementQuerySelectorAll = Element.prototype.querySelectorAll;

Object.defineProperty(window, "getComputedStyle", {
  configurable: true,
  value: (element: Element, pseudoElement?: string | null) =>
    originalGetComputedStyle(element, pseudoElement ? undefined : pseudoElement),
});

Object.defineProperty(Element.prototype, "querySelectorAll", {
  configurable: true,
  value(selectors: string) {
    try {
      return originalElementQuerySelectorAll.call(this, selectors);
    } catch (error) {
      if (selectors.includes("ant-select-item-option-selected")) {
        return document.createDocumentFragment().querySelectorAll("*");
      }
      throw error;
    }
  },
});

Object.defineProperty(window, "matchMedia", {
  configurable: true,
  writable: true,
  value: vi.fn().mockImplementation(
    (query: string): MediaQueryList =>
      ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }) as MediaQueryList,
  ),
});

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Object.defineProperty(window, "ResizeObserver", {
  configurable: true,
  writable: true,
  value: MockResizeObserver,
});

beforeAll(() => server.listen({ onUnhandledRequest: "warn" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
