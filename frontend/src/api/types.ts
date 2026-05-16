// 后端统一返回信封。与 zy-engine-mvp 的 ApiResult 对齐。
export interface ApiResult<T = unknown> {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  trace_id: string;
}

// 组织上下文五段式。Header / Query / Body 三方合并，Body 优先。
export interface OrgContext {
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
}

// Provider 运行状态（与 /api/system/providers 对齐）
export interface ProviderStatus {
  name: string;
  ready: boolean;
  provider: string;
  reason?: string;
}

export interface SystemProviders {
  run_mode: "DB_ONLY" | "HYBRID" | "FULL_INTEGRATION" | "IN_MEMORY_DEMO";
  providers: ProviderStatus[];
  timestamp?: string;
}

// 通用错误码（与 00_总入口 §9 一致）
export type ApiErrorCode =
  | "SUCCESS"
  | "VALIDATION_ERROR"
  | "DATA_MISSING"
  | "CONFIG_NOT_FOUND"
  | "ENGINE_TIMEOUT"
  | "ADAPTER_TIMEOUT"
  | "DIFY_TIMEOUT"
  | "DB_ERROR"
  | "UNKNOWN_ERROR"
  | "NO_RULES_MATCHED"
  | "PENDING_MAPPING"
  | "MISSING_SOURCE";

export class ApiError extends Error {
  public readonly code: ApiErrorCode;
  public readonly traceId: string;
  public readonly httpStatus?: number;

  constructor(code: ApiErrorCode, message: string, traceId = "", httpStatus?: number) {
    super(message);
    this.code = code;
    this.traceId = traceId;
    this.httpStatus = httpStatus;
    this.name = "ApiError";
  }
}
