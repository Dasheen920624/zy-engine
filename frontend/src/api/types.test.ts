import { describe, it, expect } from "vitest";
import { ApiError } from "../types";
import type { ApiResult, PagedResult, OrgContext, ProviderStatus } from "../types";

describe("ApiResult 类型守卫", () => {
  it("成功的 ApiResult 应有 success=true", () => {
    const result: ApiResult<string> = {
      success: true,
      code: "SUCCESS",
      message: "操作成功",
      data: "test-data",
      trace_id: "trace-001",
    };
    expect(result.success).toBe(true);
    expect(result.data).toBe("test-data");
  });

  it("失败的 ApiResult 应有 success=false", () => {
    const result: ApiResult<null> = {
      success: false,
      code: "VALIDATION_ERROR",
      message: "参数错误",
      data: null,
      trace_id: "trace-002",
    };
    expect(result.success).toBe(false);
    expect(result.data).toBeNull();
  });
});

describe("PagedResult", () => {
  it("应正确构造分页结果", () => {
    const paged: PagedResult<string> = {
      items: ["a", "b"],
      total: 10,
      page: 1,
      page_size: 2,
      total_pages: 5,
    };
    expect(paged.items).toHaveLength(2);
    expect(paged.total_pages).toBe(5);
  });
});

describe("OrgContext", () => {
  it("应允许部分字段", () => {
    const ctx: OrgContext = { tenant_id: "T1" };
    expect(ctx.tenant_id).toBe("T1");
    expect(ctx.hospital_code).toBeUndefined();
  });
});

describe("ApiError 类", () => {
  it("应正确继承 Error", () => {
    const err = new ApiError("DB_ERROR", "数据库异常", "t-1");
    expect(err).toBeInstanceOf(Error);
    expect(err.name).toBe("ApiError");
    expect(err.code).toBe("DB_ERROR");
    expect(err.message).toBe("数据库异常");
    expect(err.traceId).toBe("t-1");
  });
});
