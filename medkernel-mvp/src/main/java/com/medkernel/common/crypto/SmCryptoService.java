package com.medkernel.common.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

/**
 * 国密 SM2 / SM3 / SM4 统一服务。
 *
 * <p>封装 BouncyCastle 标准国密 API，为「合规上线」场景（数据加密 / 审计签名 / SSO 国密登录）
 * 提供纯函数式接口。
 *
 * <h2>算法标准</h2>
 * <ul>
 *   <li><b>SM2</b>（GB/T 32918-2017）：椭圆曲线 {@code sm2p256v1}；加密 C1C3C2 ASN.1；签名摘要 SM3。</li>
 *   <li><b>SM3</b>（GB/T 32905-2016）：杂凑算法，输出 256 bit。</li>
 *   <li><b>SM4</b>（GB/T 32907-2016）：分组密码，密钥 / 块 128 bit，模式 ECB / CBC + PKCS5Padding。</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>纯函数 stateless，{@link SecureRandom} 是唯一字段，线程安全。</li>
 *   <li>所有底层 {@link GeneralSecurityException} 归一包装为 {@link SmCryptoException}。</li>
 *   <li>算法变体固化为常量，运行时不可配置（避免换 padding 致密文不兼容）。</li>
 *   <li>所有 {@code getInstance} 显式指定 {@code "BC"} Provider，杜绝 SunEC 替换。</li>
 * </ul>
 *
 * @see SmCryptoConfig
 */
@Service
public class SmCryptoService {

    /** JCA 内部算法标识。 */
    private static final String SM3_ALGORITHM = "SM3";
    private static final String SM4_KEY_ALGORITHM = "SM4";
    private static final String SM4_ECB_TRANSFORMATION = "SM4/ECB/PKCS5Padding";
    private static final String SM4_CBC_TRANSFORMATION = "SM4/CBC/PKCS5Padding";
    private static final String SM2_KEY_ALGORITHM = "EC";
    private static final String SM2_CURVE_NAME = "sm2p256v1";
    private static final String SM2_CIPHER_TRANSFORMATION = "SM2";
    private static final String SM2_SIGN_ALGORITHM = "SM3withSM2";
    private static final String BC_PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    /** SM4 密钥与 IV 长度均为 128 bit = 16 字节。 */
    private static final int SM4_KEY_LENGTH_BYTES = 16;
    private static final int SM4_IV_LENGTH_BYTES = 16;

    /** Hex 编码字符表（小写，符合 GB/T 32905-2016 测试向量约定）。 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom;

    public SmCryptoService() {
        this.secureRandom = new SecureRandom();
    }

    // ============================================================
    // SM3 杂凑（GB/T 32905-2016）
    // ============================================================

    /**
     * SM3 哈希，返回 32 字节摘要。
     */
    public byte[] sm3(byte[] data) {
        Objects.requireNonNull(data, "data 不能为 null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SM3_ALGORITHM, BC_PROVIDER);
            return digest.digest(data);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM3 算法不可用，请确认 BouncyCastle Provider 已注册", e);
        }
    }

    /**
     * SM3 哈希，返回 64 字符小写 Hex 字符串。
     */
    public String sm3Hex(byte[] data) {
        return toHex(sm3(data));
    }

    // ============================================================
    // SM4 对称加密（GB/T 32907-2016）
    // ============================================================

    /** 生成符合 SM4 长度的随机密钥（16 字节）。 */
    public byte[] generateSm4Key() {
        byte[] key = new byte[SM4_KEY_LENGTH_BYTES];
        secureRandom.nextBytes(key);
        return key;
    }

    /** 生成符合 SM4 长度的随机 IV（16 字节）。 */
    public byte[] generateSm4Iv() {
        byte[] iv = new byte[SM4_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * SM4 / ECB / PKCS5Padding 加密。
     *
     * <p>注意：ECB 不安全（相同明文块产生相同密文块），
     * 仅适用于单块加密（如 KEK 包装）。常规数据加密请使用 {@link #sm4EncryptCbc}。
     */
    public byte[] sm4EncryptEcb(byte[] data, byte[] key) {
        return sm4Ecb(data, key, Cipher.ENCRYPT_MODE);
    }

    /** SM4 / ECB / PKCS5Padding 解密。 */
    public byte[] sm4DecryptEcb(byte[] cipherText, byte[] key) {
        return sm4Ecb(cipherText, key, Cipher.DECRYPT_MODE);
    }

    /** SM4 / CBC / PKCS5Padding 加密。 */
    public byte[] sm4EncryptCbc(byte[] data, byte[] key, byte[] iv) {
        return sm4Cbc(data, key, iv, Cipher.ENCRYPT_MODE);
    }

    /** SM4 / CBC / PKCS5Padding 解密。 */
    public byte[] sm4DecryptCbc(byte[] cipherText, byte[] key, byte[] iv) {
        return sm4Cbc(cipherText, key, iv, Cipher.DECRYPT_MODE);
    }

    private byte[] sm4Ecb(byte[] data, byte[] key, int mode) {
        Objects.requireNonNull(data, "data 不能为 null");
        validateSm4Key(key);
        try {
            Cipher cipher = Cipher.getInstance(SM4_ECB_TRANSFORMATION, BC_PROVIDER);
            cipher.init(mode, new SecretKeySpec(key, SM4_KEY_ALGORITHM));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM4/ECB 运算失败", e);
        }
    }

    private byte[] sm4Cbc(byte[] data, byte[] key, byte[] iv, int mode) {
        Objects.requireNonNull(data, "data 不能为 null");
        validateSm4Key(key);
        validateSm4Iv(iv);
        try {
            Cipher cipher = Cipher.getInstance(SM4_CBC_TRANSFORMATION, BC_PROVIDER);
            cipher.init(mode, new SecretKeySpec(key, SM4_KEY_ALGORITHM), new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM4/CBC 运算失败", e);
        }
    }

    private static void validateSm4Key(byte[] key) {
        Objects.requireNonNull(key, "key 不能为 null");
        if (key.length != SM4_KEY_LENGTH_BYTES) {
            throw new SmCryptoException(
                    "SM4 密钥长度必须为 " + SM4_KEY_LENGTH_BYTES + " 字节，实际：" + key.length);
        }
    }

    private static void validateSm4Iv(byte[] iv) {
        Objects.requireNonNull(iv, "iv 不能为 null");
        if (iv.length != SM4_IV_LENGTH_BYTES) {
            throw new SmCryptoException(
                    "SM4 IV 长度必须为 " + SM4_IV_LENGTH_BYTES + " 字节，实际：" + iv.length);
        }
    }

    // ============================================================
    // SM2 非对称加密 / 签名（GB/T 32918-2017）
    // ============================================================

    /**
     * 生成 SM2 密钥对。
     *
     * <p>公钥编码：X.509 SubjectPublicKeyInfo（含曲线 OID）。
     * <p>私钥编码：PKCS#8 PrivateKeyInfo。
     * <p>两者均可直接 Base64 序列化跨进程传输。
     */
    public SmKeyPair generateSm2KeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(SM2_KEY_ALGORITHM, BC_PROVIDER);
            gen.initialize(new ECGenParameterSpec(SM2_CURVE_NAME), secureRandom);
            KeyPair keyPair = gen.generateKeyPair();
            return new SmKeyPair(
                    keyPair.getPublic().getEncoded(),
                    keyPair.getPrivate().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM2 密钥对生成失败", e);
        }
    }

    /**
     * SM2 公钥加密。
     *
     * <p>密文格式：C1C3C2（GB/T 32918.4-2017 §6.1），由 BouncyCastle 默认输出。
     */
    public byte[] sm2Encrypt(byte[] data, byte[] publicKey) {
        Objects.requireNonNull(data, "data 不能为 null");
        Objects.requireNonNull(publicKey, "publicKey 不能为 null");
        try {
            PublicKey pubKey = KeyFactory.getInstance(SM2_KEY_ALGORITHM, BC_PROVIDER)
                    .generatePublic(new X509EncodedKeySpec(publicKey));
            Cipher cipher = Cipher.getInstance(SM2_CIPHER_TRANSFORMATION, BC_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey, secureRandom);
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM2 加密失败", e);
        }
    }

    /** SM2 私钥解密。 */
    public byte[] sm2Decrypt(byte[] cipherText, byte[] privateKey) {
        Objects.requireNonNull(cipherText, "cipherText 不能为 null");
        Objects.requireNonNull(privateKey, "privateKey 不能为 null");
        try {
            PrivateKey privKey = KeyFactory.getInstance(SM2_KEY_ALGORITHM, BC_PROVIDER)
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            Cipher cipher = Cipher.getInstance(SM2_CIPHER_TRANSFORMATION, BC_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM2 解密失败", e);
        }
    }

    /**
     * SM2 签名（摘要算法 SM3）。
     *
     * <p>默认使用 BouncyCastle 内置默认 userID（GM/T 0009-2012 规定 ASCII {@code "1234567812345678"}）。
     * 跨实现互操作时双方必须一致，否则验签失败。
     */
    public byte[] sm2Sign(byte[] data, byte[] privateKey) {
        Objects.requireNonNull(data, "data 不能为 null");
        Objects.requireNonNull(privateKey, "privateKey 不能为 null");
        try {
            PrivateKey privKey = KeyFactory.getInstance(SM2_KEY_ALGORITHM, BC_PROVIDER)
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            Signature signature = Signature.getInstance(SM2_SIGN_ALGORITHM, BC_PROVIDER);
            signature.initSign(privKey, secureRandom);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM2 签名失败", e);
        }
    }

    /** SM2 验签。返回 true 表示签名有效。 */
    public boolean sm2Verify(byte[] data, byte[] signatureBytes, byte[] publicKey) {
        Objects.requireNonNull(data, "data 不能为 null");
        Objects.requireNonNull(signatureBytes, "signature 不能为 null");
        Objects.requireNonNull(publicKey, "publicKey 不能为 null");
        try {
            PublicKey pubKey = KeyFactory.getInstance(SM2_KEY_ALGORITHM, BC_PROVIDER)
                    .generatePublic(new X509EncodedKeySpec(publicKey));
            Signature signature = Signature.getInstance(SM2_SIGN_ALGORITHM, BC_PROVIDER);
            signature.initVerify(pubKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException e) {
            throw new SmCryptoException("SM2 验签失败", e);
        }
    }

    // ============================================================
    // 工具
    // ============================================================

    /**
     * 字节数组 → 小写 Hex 字符串。包内可见，主要给 {@link #sm3Hex} 使用。
     */
    static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }
}
