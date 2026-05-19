package com.medkernel.common;

/**
 * 统一错误码枚举。
 *
 * <p>每个错误码包含：
 * <ul>
 *   <li>code — 机器可读错误码（大写下划线风格）</li>
 *   <li>message — 英文默认消息</li>
 *   <li>messageKey — i18n 消息键，前端可据此查找本地化文案</li>
 * </ul>
 *
 * <p>错误码分类：
 * <ul>
 *   <li>0 — 成功</li>
 *   <li>VALIDATION_* — 请求校验错误（400）</li>
 *   <li>AUTH_*/FORBIDDEN — 认证授权错误（401/403/423）</li>
 *   <li>NOT_FOUND — 资源不存在（404）</li>
 *   <li>CONFLICT — 资源冲突/幂等重复（409）</li>
 *   <li>SOURCE_* — 来源校验错误（400）</li>
 *   <li>TENANT_* — 租户错误（400/403）</li>
 *   <li>*_TIMEOUT — 超时错误（504）</li>
 *   <li>DB_* — 数据库错误（500）</li>
 *   <li>UNKNOWN — 未知错误（500）</li>
 * </ul>
 */
public enum ErrorCode {
    // ─── 成功 ────────────────────────────────────────────────────────
    SUCCESS("0", "success", null),

    // ─── 请求校验（400） ─────────────────────────────────────────────
    VALIDATION_ERROR("VALIDATION_ERROR", "request validation failed", "error.validation"),
    DATA_MISSING("DATA_MISSING", "required clinical data is missing", "error.data_missing"),
    PAGE_OUT_OF_RANGE("PAGE_OUT_OF_RANGE", "page number out of range", "error.page_out_of_range"),
    IDEMPOTENT_DUPLICATE("IDEMPOTENT_DUPLICATE", "duplicate request detected", "error.idempotent_duplicate"),

    // ─── 认证授权（401/403/423） ─────────────────────────────────────
    UNAUTHORIZED("UNAUTHORIZED", "authentication required", "error.auth.unauthorized"),
    FORBIDDEN("FORBIDDEN", "access denied", "error.auth.forbidden"),
    LOGIN_FAILED("LOGIN_FAILED", "invalid username or password", "error.auth.login_failed"),
    USER_LOCKED("USER_LOCKED", "account is temporarily locked", "error.auth.user_locked"),
    ROLE_NOT_ALLOWED("ROLE_NOT_ALLOWED", "role not allowed for this operation", "error.auth.role_not_allowed"),
    PERMISSION_DENIED("PERMISSION_DENIED", "insufficient permissions for this resource", "error.auth.permission_denied"),

    // ─── 资源不存在（404） ───────────────────────────────────────────
    CONFIG_NOT_FOUND("CONFIG_NOT_FOUND", "engine configuration was not found", "error.config_not_found"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "requested resource was not found", "error.resource_not_found"),

    // ─── 资源冲突（409） ─────────────────────────────────────────────
    CONFLICT("CONFLICT", "resource conflict", "error.conflict"),
    VERSION_CONFLICT("VERSION_CONFLICT", "version conflict, please refresh and retry", "error.version_conflict"),

    // ─── 来源校验（400） ─────────────────────────────────────────────
    MISSING_SOURCE("MISSING_SOURCE", "asset has no valid source document binding", "error.source.missing"),
    SOURCE_EXPIRED("SOURCE_EXPIRED", "source document has expired", "error.source.expired"),
    SOURCE_NOT_REVIEWED("SOURCE_NOT_REVIEWED", "source document has not been reviewed", "error.source.not_reviewed"),

    // ─── 租户（400/403） ─────────────────────────────────────────────
    TENANT_NOT_FOUND("TENANT_NOT_FOUND", "tenant not found", "error.tenant.not_found"),
    TENANT_SUSPENDED("TENANT_SUSPENDED", "tenant is suspended", "error.tenant.suspended"),
    TENANT_QUOTA_EXCEEDED("TENANT_QUOTA_EXCEEDED", "tenant quota exceeded", "error.tenant.quota_exceeded"),

    // ─── 超时（504） ─────────────────────────────────────────────────
    ENGINE_TIMEOUT("ENGINE_TIMEOUT", "engine execution timed out", "error.timeout.engine"),
    ADAPTER_TIMEOUT("ADAPTER_TIMEOUT", "adapter execution timed out", "error.timeout.adapter"),
    DIFY_TIMEOUT("DIFY_TIMEOUT", "dify workflow timed out", "error.timeout.dify"),

    // ─── 数据库（500） ───────────────────────────────────────────────
    DB_ERROR("DB_ERROR", "database error", "error.db"),

    // ─── 未知（500） ─────────────────────────────────────────────────
    UNKNOWN_ERROR("UNKNOWN_ERROR", "unknown error", "error.unknown");

    private final String code;
    private final String message;
    private final String messageKey;

    ErrorCode(String code, String message, String messageKey) {
        this.code = code;
        this.message = message;
        this.messageKey = messageKey;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
