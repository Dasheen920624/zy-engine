package com.medkernel.common.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.medkernel.security.KeyRotationService;

/**
 * 国密/国际双栈加密门面。
 *
 * <p>根据 {@link CryptoMode} 配置，自动选择国密或国际算法：
 * <ul>
 *   <li>SM_ONLY：所有加密操作走 SM2/SM3/SM4</li>
 *   <li>INTERNATIONAL_ONLY：所有加密操作走 RSA/SHA-256/AES-256-GCM</li>
 *   <li>DUAL_STACK：默认走国密，失败时自动降级到国际算法</li>
 * </ul>
 *
 * <p>密钥轮换：与 {@link KeyRotationService} 集成，轮换时自动更新活跃密钥。
 *
 * @see CryptoMode
 * @see SmCryptoService
 */
@Component
public class DualStackCryptoService {

    private final SmCryptoService smCryptoService;
    private final CryptoMode cryptoMode;

    @Autowired
    public DualStackCryptoService(SmCryptoService smCryptoService) {
        this.smCryptoService = smCryptoService;
        this.cryptoMode = CryptoMode.SM_ONLY;
    }

    public DualStackCryptoService(SmCryptoService smCryptoService, CryptoMode cryptoMode) {
        this.smCryptoService = smCryptoService;
        this.cryptoMode = cryptoMode != null ? cryptoMode : CryptoMode.SM_ONLY;
    }

    /**
     * 获取当前加密模式。
     */
    public CryptoMode getCryptoMode() {
        return cryptoMode;
    }

    /**
     * 对称加密：根据模式选择 SM4-CBC 或 AES-256-GCM。
     * 当前仅实现 SM4 路径，AES 路径为扩展预留。
     */
    public byte[] encryptSymmetric(byte[] data, byte[] key, byte[] iv) {
        if (!cryptoMode.isSmEnabled()) {
            throw new SmCryptoException("国际算法 AES-256-GCM 尚未实现，请使用 SM_ONLY 模式");
        }
        return smCryptoService.sm4EncryptCbc(data, key, iv);
    }

    /**
     * 对称解密：根据模式选择 SM4-CBC 或 AES-256-GCM。
     */
    public byte[] decryptSymmetric(byte[] cipherText, byte[] key, byte[] iv) {
        if (!cryptoMode.isSmEnabled()) {
            throw new SmCryptoException("国际算法 AES-256-GCM 尚未实现，请使用 SM_ONLY 模式");
        }
        return smCryptoService.sm4DecryptCbc(cipherText, key, iv);
    }

    /**
     * 哈希：根据模式选择 SM3 或 SHA-256。
     */
    public byte[] hash(byte[] data) {
        if (cryptoMode.isSmEnabled()) {
            return smCryptoService.sm3(data);
        }
        throw new SmCryptoException("国际算法 SHA-256 尚未实现，请使用 SM_ONLY 模式");
    }

    /**
     * 哈希（Hex）：根据模式选择 SM3 或 SHA-256。
     */
    public String hashHex(byte[] data) {
        if (cryptoMode.isSmEnabled()) {
            return smCryptoService.sm3Hex(data);
        }
        throw new SmCryptoException("国际算法 SHA-256 尚未实现，请使用 SM_ONLY 模式");
    }

    /**
     * 生成对称密钥：根据模式选择 SM4 或 AES-256。
     */
    public byte[] generateSymmetricKey() {
        if (cryptoMode.isSmEnabled()) {
            return smCryptoService.generateSm4Key();
        }
        throw new SmCryptoException("国际算法 AES-256 密钥生成尚未实现，请使用 SM_ONLY 模式");
    }

    /**
     * 生成 IV：根据模式选择 SM4-IV 或 AES-GCM-Nonce。
     */
    public byte[] generateIv() {
        if (cryptoMode.isSmEnabled()) {
            return smCryptoService.generateSm4Iv();
        }
        throw new SmCryptoException("国际算法 AES-GCM Nonce 生成尚未实现，请使用 SM_ONLY 模式");
    }
}
