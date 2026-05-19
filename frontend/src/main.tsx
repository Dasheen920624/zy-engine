import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { App as AntdApp } from "antd";
import "antd/dist/reset.css";
import "./styles/tokens.css";
import "./styles/global.css";
import App from "./App";
import { MedKernelThemeProvider } from "./theme/ThemeProvider";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

async function bootstrap() {
  if (import.meta.env.VITE_ENABLE_MSW === "true") {
    const { worker } = await import("./mocks/browser");
    await worker.start({ onUnhandledRequest: "bypass" });
  }

  const root = document.getElementById("root");
  if (!root) {
    throw new Error("Root element #root not found");
  }

  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <MedKernelThemeProvider>
        <AntdApp>
          <QueryClientProvider client={queryClient}>
            <BrowserRouter>
              <App />
            </BrowserRouter>
          </QueryClientProvider>
        </AntdApp>
      </MedKernelThemeProvider>
    </React.StrictMode>,
  );
}

bootstrap();
