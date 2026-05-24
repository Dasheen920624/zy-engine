package com.medkernel.shared.api.error;

/**
 * MedKernel v1.0 GA 统一错误码。
 *
 * <p>命名前缀：
 * <ul>
 *   <li>{@code ENG-API-*}：API 契约相关（参数、鉴权、HTTP 语义）</li>
 *   <li>{@code ENG-BASE-*}：基础底座（租户、组织、权限上下文）</li>
 *   <li>{@code ENG-SYS-*}：系统级（内部错误、下游故障）</li>
 *   <li>{@code ENG-KNOW-*}、{@code ENG-RULE-*}、{@code ENG-PATH-*}、{@code ENG-CDSS-*} 等业务域错误码在 E3 各引擎实施时继续登记</li>
 * </ul>
 *
 * <p>code 一旦发布对客户端可见，禁止改名；只能新增或废弃（标记 @Deprecated 并保留）。
 */
public enum ErrorCode {

    /** 成功 */
    OK("OK", 200, "操作成功"),

    /** 请求体无法解析（JSON 损坏、必填字段缺失等结构性错误） */
    BAD_REQUEST("ENG-API-001", 400, "请求参数无效"),

    /** Bean Validation 校验失败 */
    VALIDATION_FAILED("ENG-API-002", 400, "请求参数校验失败"),

    /** 未携带或携带了无效的 JWT */
    UNAUTHORIZED("ENG-API-003", 401, "未授权访问"),

    /** 已登录但无权限执行该动作 */
    FORBIDDEN("ENG-API-004", 403, "无权限执行该操作"),

    /** 资源不存在 */
    NOT_FOUND("ENG-API-005", 404, "资源不存在"),

    /** HTTP 方法不允许 */
    METHOD_NOT_ALLOWED("ENG-API-006", 405, "方法不允许"),

    /** 资源冲突（如重复唯一键、状态机不允许的跳转） */
    CONFLICT("ENG-API-007", 409, "资源冲突"),

    /** 请求过于频繁 */
    TOO_MANY_REQUESTS("ENG-API-008", 429, "请求过于频繁，请稍后重试"),

    /** 不支持的媒体类型 */
    UNSUPPORTED_MEDIA_TYPE("ENG-API-009", 415, "不支持的请求媒体类型"),

    /** 租户上下文缺失（请求未携带租户 / JWT claim 无 tenant_id） */
    TENANT_CONTEXT_MISSING("ENG-BASE-001", 400, "租户上下文缺失"),

    /** 跨租户访问被拒（数据范围策略校验失败） */
    TENANT_FORBIDDEN("ENG-BASE-002", 403, "无权访问该租户数据"),

    /** 组织范围不足（如 medicine 角色访问质控驾驶舱） */
    DATA_SCOPE_DENIED("ENG-BASE-003", 403, "数据范围权限不足"),

    /** 服务内部错误（未捕获异常） */
    INTERNAL_ERROR("ENG-SYS-001", 500, "服务内部错误"),

    /** 下游服务不可用（数据库、模型网关、Dify、外部 HIS 等） */
    DOWNSTREAM_UNAVAILABLE("ENG-SYS-002", 503, "下游服务不可用"),

    /** 模型能力降级（B0 基线在用） */
    MODEL_DEGRADED("ENG-SYS-003", 503, "AI 模型不可用，已降级到无模型基线"),
    ;

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, int httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
