package com.medkernel.shared.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ErrorCode.ErrorClass;

class ErrorCodeTest {

    @Test
    void allErrorCodesHaveErrorClassAndRetryableFlag() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.errorClass())
                .as("ErrorCode %s 必须声明 errorClass", code.name())
                .isNotNull();
        }
    }

    @Test
    void retryableExternalErrorsAreFlagged() {
        assertThat(ErrorCode.DOWNSTREAM_UNAVAILABLE.retryable()).isTrue();
        assertThat(ErrorCode.TOO_MANY_REQUESTS.retryable()).isTrue();
    }

    @Test
    void nonRetryableInputErrors() {
        assertThat(ErrorCode.BAD_REQUEST.retryable()).isFalse();
        assertThat(ErrorCode.VALIDATION_FAILED.retryable()).isFalse();
        assertThat(ErrorCode.NOT_FOUND.retryable()).isFalse();
    }

    @Test
    void errorClassMatchesNaturalGrouping() {
        assertThat(ErrorCode.BAD_REQUEST.errorClass()).isEqualTo(ErrorClass.INPUT);
        assertThat(ErrorCode.UNAUTHORIZED.errorClass()).isEqualTo(ErrorClass.AUTH);
        assertThat(ErrorCode.FORBIDDEN.errorClass()).isEqualTo(ErrorClass.AUTH);
        assertThat(ErrorCode.NOT_FOUND.errorClass()).isEqualTo(ErrorClass.DATA);
        assertThat(ErrorCode.DOWNSTREAM_UNAVAILABLE.errorClass()).isEqualTo(ErrorClass.EXTERNAL);
        assertThat(ErrorCode.INTERNAL_ERROR.errorClass()).isEqualTo(ErrorClass.INTERNAL);
    }

    @Test
    void obsErrorCodesAreRegistered() {
        assertThat(ErrorCode.fromCode("ENG-OBS-001")).hasValueSatisfying(code -> {
            assertThat(code.httpStatus()).isEqualTo(404);
            assertThat(code.errorClass()).isEqualTo(ErrorClass.DATA);
        });
        assertThat(ErrorCode.fromCode("ENG-OBS-002")).hasValueSatisfying(code -> {
            assertThat(code.httpStatus()).isEqualTo(500);
            assertThat(code.errorClass()).isEqualTo(ErrorClass.INTERNAL);
        });
    }
}
