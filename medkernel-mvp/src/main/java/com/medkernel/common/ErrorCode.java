package com.medkernel.common;

public enum ErrorCode {
    SUCCESS("0", "success"),
    VALIDATION_ERROR("VALIDATION_ERROR", "request validation failed"),
    DATA_MISSING("DATA_MISSING", "required clinical data is missing"),
    CONFIG_NOT_FOUND("CONFIG_NOT_FOUND", "engine configuration was not found"),
    ENGINE_TIMEOUT("ENGINE_TIMEOUT", "engine execution timed out"),
    ADAPTER_TIMEOUT("ADAPTER_TIMEOUT", "adapter execution timed out"),
    DIFY_TIMEOUT("DIFY_TIMEOUT", "dify workflow timed out"),
    DB_ERROR("DB_ERROR", "database error"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "unknown error"),
    UNAUTHORIZED("UNAUTHORIZED", "authentication required"),
    FORBIDDEN("FORBIDDEN", "access denied"),
    LOGIN_FAILED("LOGIN_FAILED", "invalid username or password"),
    USER_LOCKED("USER_LOCKED", "account is temporarily locked"),
    MISSING_SOURCE("MISSING_SOURCE", "required source citation or document binding is missing"),
    SOURCE_EXPIRED("SOURCE_EXPIRED", "source document has expired"),
    SOURCE_UNREVIEWED("SOURCE_UNREVIEWED", "source document has not been reviewed"),
    PUBLISH_BLOCKED("PUBLISH_BLOCKED", "publish is blocked by gate check"),
    NO_RULES_MATCHED("NO_RULES_MATCHED", "no matching published rules found for the scenario");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

