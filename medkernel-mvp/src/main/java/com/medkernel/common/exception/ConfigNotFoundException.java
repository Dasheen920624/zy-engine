package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 配置/版本不存在。GlobalExceptionHandler → 404 CONFIG_NOT_FOUND。
 */
public class ConfigNotFoundException extends BusinessException {

    public ConfigNotFoundException(String message) {
        super(ErrorCode.CONFIG_NOT_FOUND, message);
    }

    public ConfigNotFoundException(String packageCode, String version) {
        super(ErrorCode.CONFIG_NOT_FOUND,
                "config package not found: code=" + packageCode + ", version=" + version);
    }
}
