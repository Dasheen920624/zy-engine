package com.medkernel.common.crypto;

import java.util.Objects;

/**
 * SM2 密钥对（不可变 value object）。
 *
 * <p>持有 X.509 编码的公钥与 PKCS#8 编码的私钥字节数组，
 * 跨进程 / 持久化时序列化为 Base64 即可。
 *
 * <p>由 {@link SmCryptoService#generateSm2KeyPair()} 生成；
 * 调用方负责安全保管私钥（HSM / 加密存储 / 内存最小驻留期）。
 *
 * <p>线程安全：字节数组是 defensive copy，外部修改不影响实例状态。
 */
public final class SmKeyPair {

    private final byte[] publicKey;
    private final byte[] privateKey;

    public SmKeyPair(byte[] publicKey, byte[] privateKey) {
        Objects.requireNonNull(publicKey, "publicKey 不能为 null");
        Objects.requireNonNull(privateKey, "privateKey 不能为 null");
        this.publicKey = publicKey.clone();
        this.privateKey = privateKey.clone();
    }

    /**
     * 公钥字节（X.509 SubjectPublicKeyInfo 编码）。
     *
     * <p>每次调用返回新副本，防止外部篡改。
     */
    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    /**
     * 私钥字节（PKCS#8 PrivateKeyInfo 编码）。
     *
     * <p>每次调用返回新副本，防止外部篡改。
     */
    public byte[] getPrivateKey() {
        return privateKey.clone();
    }
}
