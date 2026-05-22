import { describe, it, expect } from "vitest";
import { ApiError } from "../types";
import type { ApiErrorCode } from "../types";

describe("ApiError", () => {
  it("应正确构造 ApiError", () => {
    const error = new ApiError("VALIDATION_ERROR" as ApiErrorCode, "参数校验失败", "trace-123", 400, "validation.error");
    expect(error.name).toBe("ApiError");
    expect(error.code).toBe("VALIDATION_ERROR");
    expect(error.message).toBe("参数校验失败");
    expect(error.traceId).toBe("trace-123");
    expect(error.httpStatus).toBe(400);
    expect(error.messageKey).toBe("validation.error");
    expect(error).toBeInstanceOf(Error);
  });

  it("应有默认参数", () => {
    const error = new ApiError("UNKNOWN_ERROR" as ApiErrorCode, "未知错误");
    expect(error.traceId).toBe("");
    expect(error.httpStatus).toBeUndefined();
    expect(error.messageKey).toBeUndefined();
  });
});
