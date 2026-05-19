package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 必要业务数据缺失（如 patient.facts 没有 arrival_time）。
 * GlobalExceptionHandler → 400 DATA_MISSING。
 */
public class DataMissingException extends BusinessException {

    public DataMissingException(String message) {
        super(ErrorCode.DATA_MISSING, message);
    }

    public DataMissingException(String message, Throwable cause) {
        super(ErrorCode.DATA_MISSING, message, cause);
    }
}
