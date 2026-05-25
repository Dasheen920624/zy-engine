import { useMemo } from "react";
import { ConfigProvider, theme as antdTheme } from "antd";
import zhCN from "antd/locale/zh_CN";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppRouter } from "./router";
import { useThemeStore } from "@/shared/lib/themeStore";
import { theme as medkernelTheme } from "@/shared/config/theme";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

export default function App() {
  const mode = useThemeStore((s) => s.mode);

  const themeConfig = useMemo(() => {
    const base = {
      token: medkernelTheme.token,
    };

    if (mode === "elder") {
      return {
        ...base,
        token: { ...base.token, fontSize: 16, controlHeight: 40, borderRadius: 8 },
      };
    }
    if (mode === "dark") {
      return { ...base, algorithm: antdTheme.darkAlgorithm };
    }
    if (mode === "eye") {
      return {
        ...base,
        token: {
          ...base.token,
          colorBgLayout: "var(--mk-bg-eye-layout)",
          colorBgContainer: "var(--mk-bg-eye-container)",
        },
      };
    }
    if (mode === "system") {
      const prefersDark =
        typeof window !== "undefined" && window.matchMedia("(prefers-color-scheme: dark)").matches;
      return prefersDark ? { ...base, algorithm: antdTheme.darkAlgorithm } : base;
    }
    return base;
  }, [mode]);

  return (
    <ConfigProvider locale={zhCN} theme={themeConfig}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AppRouter />
        </BrowserRouter>
      </QueryClientProvider>
    </ConfigProvider>
  );
}
