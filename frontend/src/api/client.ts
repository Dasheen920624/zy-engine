import axios, { AxiosError, AxiosRequestConfig, AxiosResponse } from "axios";
import { ApiError, ApiResult, OrgContext } from "./types";
import { generateTraceId } from "../utils/traceId";
import { getOrgContext } from "../store/orgContext";

/**
 * 统一 API client。
 *
 * - 所有请求自动带 X-Trace-Id
 * - 所有请求自动带组织上下文 Header（X-Tenant-Id / X-Hospital-Code 等）
 * - 响应若 ApiResult.success=false 则抛 ApiError，业务侧不需要重复判 success
 * - HTTP 非 2xx 也抛 ApiError，含 traceId 便于排错
 */

const baseURL =
  import.meta.env.VITE_API_BASE_URL ||
  // 内网部署通常前后端同源；开发期通过 vite proxy 转发
  "/medkernel/api";

export const http = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

function applyOrgHeaders(headers: Record<string, string>, org: OrgContext) {
  if (org.tenant_id) headers["X-Tenant-Id"] = org.tenant_id;
  if (org.group_code) headers["X-Group-Code"] = org.group_code;
  if (org.hospital_code) headers["X-Hospital-Code"] = org.hospital_code;
  if (org.campus_code) headers["X-Campus-Code"] = org.campus_code;
  if (org.site_code) headers["X-Site-Code"] = org.site_code;
  if (org.department_code) headers["X-Department-Code"] = org.department_code;
}

http.interceptors.request.use((config) => {
  const headers = (config.headers || {}) as Record<string, string>;
  if (!headers["X-Trace-Id"]) {
    headers["X-Trace-Id"] = generateTraceId();
  }
  applyOrgHeaders(headers, getOrgContext());
  config.headers = headers as never;
  return config;
});

http.interceptors.response.use(
  (resp: AxiosResponse<ApiResult>) => {
    const body = resp.data;
    if (body && typeof body === "object" && "success" in body && body.success === false) {
      throw new ApiError(
        (body.code as never) || "UNKNOWN_ERROR",
        body.message || "未知错误",
        body.trace_id || (resp.headers["x-trace-id"] as string) || "",
        resp.status,
      );
    }
    return resp;
  },
  (error: AxiosError<ApiResult>) => {
    const traceId =
      (error.response?.data?.trace_id as string) ||
      (error.response?.headers?.["x-trace-id"] as string) ||
      (error.config?.headers?.["X-Trace-Id"] as string) ||
      "";
    const status = error.response?.status;
    // 401/403 单独可观测：将来引入鉴权后业务层可据此跳登录或提示无权限。
    // 当前没有鉴权链路，但保留分类信息以便日志告警。
    let code: string;
    let message: string;
    if (status === 401) {
      code = "UNAUTHORIZED";
      message = "未认证或登录已过期";
    } else if (status === 403) {
      code = "FORBIDDEN";
      message = "无权限执行此操作";
    } else {
      code = (error.response?.data?.code as never) || "UNKNOWN_ERROR";
      message =
        error.response?.data?.message ||
        error.message ||
        "网络异常或后端不可达";
    }
    throw new ApiError(code as never, message, traceId, status);
  },
);

/**
 * 业务层使用的薄封装：直接拿 data。
 * 例：const providers = await get<SystemProviders>("/system/providers");
 */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const resp = await http.get<ApiResult<T>>(url, config);
  return resp.data.data as T;
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const resp = await http.post<ApiResult<T>>(url, body, config);
  return resp.data.data as T;
}
