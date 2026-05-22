package com.medkernel.common.crypto;

/**
 * 国密算法运行时异常。
 *
 * <p>包装 BouncyCastle 抛出的 {@link java.security.GeneralSecurityException}
 * 等底层异常，使调用方不需要在签名里 throws checked exception。
 *
 * <p>所有 {@link SmCryptoService} 公共方法只抛本异常（或其子类），
 * 调用方按需 catch / 透传。
 */
public class SmCryptoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SmCryptoException(String message) {
        super(message);
    }

    public SmCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
