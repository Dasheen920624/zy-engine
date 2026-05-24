// MedKernel v1.0 GA · Vitest 全局 setup
// 接入 @testing-library/jest-dom 的 toBeInTheDocument 等 matchers
import "@testing-library/jest-dom";
// Antd 5 内部用 window.matchMedia（responsive breakpoint），jsdom 不实现，需 mock
if (typeof window !== "undefined" && !window.matchMedia) {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: (query: string) => ({
      matches: false,
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

// ResizeObserver 在 Antd 5 + rc-* 里也常用，jsdom 不实现
if (typeof window !== "undefined" && typeof window.ResizeObserver === "undefined") {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (window as any).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}
