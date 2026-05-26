import axios from "axios";

/**
 * MedKernel v1.0 GA · axios HTTP client（全局单例）。
 *
 * baseURL 走 vite proxy → /medkernel/api → http://localhost:18080/medkernel/api
 * （vite.config.ts 已配 /medkernel → :18080）。
 *
 * 请求自动带 Bearer token（GA-CORE-02 OAuth2 接通后从 token store 读）。
 */
export const apiClient = axios.create({
  baseURL: "/medkernel/api/v1",
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config) => {
  // 自动加 trace-id（与后端 OpenTelemetry 链路对齐）
  if (config.headers) {
    config.headers["X-Trace-Id"] = crypto.randomUUID();
  }
  return config;
});

apiClient.interceptors.response.use(
  (resp) => resp,
  (err) => {
    if (err.response?.status === 401) {
      window.dispatchEvent(new CustomEvent("medkernel:auth-required"));
    }
    return Promise.reject(err);
  },
);
