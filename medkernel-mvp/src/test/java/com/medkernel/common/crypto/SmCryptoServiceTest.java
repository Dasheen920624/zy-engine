package com.medkernel.common.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link SmCryptoService} 单元测试。
 *
 * <p>包含 GB/T 32905-2016 SM3 国标测试向量，确保 BouncyCastle 实现合规。
 * 不引入 SpringBootTest 以保持极快的运行时（< 1 秒）。
 */
class SmCryptoServiceTest {

    private static SmCryptoService service;

    @BeforeAll
    static void registerBouncyCastle() {
        // 模拟 SmCryptoConfig 行为，独立于 Spring 上下文：
        // 1) 解除 JCE 强度限制（兼容 JDK < 8u151）
        // 2) 注册 BouncyCastle Provider
        JceUnlimitedStrengthEnabler.enable();
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        service = new SmCryptoService();
    }

    // ============================================================
    // SM3 国标向量（GB/T 32905-2016 附录 A）
    // ============================================================

    @Nested
    @DisplayName("SM3 杂凑算法 - GB/T 32905-2016")
    class Sm3 {

        /** 标准测试向量 1：输入 ASCII "abc"。 */
        @Test
        @DisplayName("国标向量 1：abc → 66c7f0f4...")
        void gbVector1_abc() {
            String actual = service.sm3Hex("abc".getBytes(StandardCharsets.US_ASCII));
            assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", actual);
        }

        /** 标准测试向量 2：64 字节 "abcdabcd...abcd"。 */
        @Test
        @DisplayName("国标向量 2：64-byte abcdabcd... → debe9ff9...")
        void gbVector2_64byteAbcd() {
            byte[] input = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd"
                    .getBytes(StandardCharsets.US_ASCII);
            assertEquals(64, input.length, "测试输入必须为 64 字节");
            String actual = service.sm3Hex(input);
            assertEquals("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732", actual);
        }

        @Test
        @DisplayName("空输入产生确定性摘要")
        void emptyInputProducesDeterministicDigest() {
            String hex = service.sm3Hex(new byte[0]);
            // GB/T 32905-2016 空消息向量
            assertEquals("1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b", hex);
        }

        @Test
        @DisplayName("null 输入抛 NPE")
        void nullInputThrowsNpe() {
            assertThrows(NullPointerException.class, () -> service.sm3(null));
        }
    }

    // ============================================================
    // SM4 对称加密
    // ============================================================

    @Nested
    @DisplayName("SM4 对称加密 - GB/T 32907-2016")
    class Sm4 {

        @Test
        @DisplayName("ECB 加密解密往返")
        void ecbRoundTrip() {
            byte[] key = service.generateSm4Key();
            byte[] plain = "国密 SM4 测试数据 / health record block".getBytes(StandardCharsets.UTF_8);

            byte[] cipher = service.sm4EncryptEcb(plain, key);
            byte[] decrypted = service.sm4DecryptEcb(cipher, key);

            assertArrayEquals(plain, decrypted);
            assertNotEquals(0, cipher.length);
        }

        @Test
        @DisplayName("CBC 加密解密往返")
        void cbcRoundTrip() {
            byte[] key = service.generateSm4Key();
            byte[] iv = service.generateSm4Iv();
            byte[] plain = "患者敏感数据".getBytes(StandardCharsets.UTF_8);

            byte[] cipher = service.sm4EncryptCbc(plain, key, iv);
            byte[] decrypted = service.sm4DecryptCbc(cipher, key, iv);

            assertArrayEquals(plain, decrypted);
        }

        @Test
        @DisplayName("CBC：不同 IV 产生不同密文（同明文同密钥）")
        void cbcDifferentIvProducesDifferentCipher() {
            byte[] key = service.generateSm4Key();
            byte[] iv1 = service.generateSm4Iv();
            byte[] iv2 = service.generateSm4Iv();
            byte[] plain = "same plaintext".getBytes(StandardCharsets.UTF_8);

            byte[] c1 = service.sm4EncryptCbc(plain, key, iv1);
            byte[] c2 = service.sm4EncryptCbc(plain, key, iv2);

            assertFalse(java.util.Arrays.equals(c1, c2), "IV 不同时 CBC 必须产生不同密文");
        }

        @Test
        @DisplayName("密钥长度错误抛 SmCryptoException")
        void wrongKeyLengthThrows() {
            byte[] shortKey = new byte[8]; // 应为 16
            assertThrows(SmCryptoException.class,
                    () -> service.sm4EncryptEcb(new byte[16], shortKey));
        }

        @Test
        @DisplayName("IV 长度错误抛 SmCryptoException")
        void wrongIvLengthThrows() {
            byte[] key = service.generateSm4Key();
            byte[] shortIv = new byte[8]; // 应为 16
            assertThrows(SmCryptoException.class,
                    () -> service.sm4EncryptCbc(new byte[16], key, shortIv));
        }

        @Test
        @DisplayName("generateSm4Key 高熵：连续生成不重复（长度由 roundTrip 隐含验证）")
        void generatedKeysAreUnique() {
            byte[] k1 = service.generateSm4Key();
            byte[] k2 = service.generateSm4Key();
            assertFalse(java.util.Arrays.equals(k1, k2),
                    "SecureRandom 不应连续产生相同 16 字节密钥");
        }
    }

    // ============================================================
    // SM2 非对称加密 / 签名
    // ============================================================

    @Nested
    @DisplayName("SM2 非对称加密与签名 - GB/T 32918-2017")
    class Sm2 {

        @Test
        @DisplayName("密钥对生成：公钥非空、私钥非空")
        void generateKeyPair_bothNotEmpty() {
            SmKeyPair kp = service.generateSm2KeyPair();
            assertNotNull(kp.getPublicKey());
            assertNotNull(kp.getPrivateKey());
            assertTrue(kp.getPublicKey().length > 0);
            assertTrue(kp.getPrivateKey().length > 0);
        }

        @Test
        @DisplayName("加密解密往返")
        void encryptDecryptRoundTrip() {
            SmKeyPair kp = service.generateSm2KeyPair();
            byte[] plain = "SM2 国密非对称加密载荷".getBytes(StandardCharsets.UTF_8);

            byte[] cipher = service.sm2Encrypt(plain, kp.getPublicKey());
            byte[] decrypted = service.sm2Decrypt(cipher, kp.getPrivateKey());

            assertArrayEquals(plain, decrypted);
        }

        @Test
        @DisplayName("签名验签往返")
        void signVerifyRoundTrip() {
            SmKeyPair kp = service.generateSm2KeyPair();
            byte[] msg = "审计链头哈希 / signed payload".getBytes(StandardCharsets.UTF_8);

            byte[] sig = service.sm2Sign(msg, kp.getPrivateKey());
            assertTrue(service.sm2Verify(msg, sig, kp.getPublicKey()));
        }

        @Test
        @DisplayName("篡改数据后验签失败")
        void tamperedDataVerifyFails() {
            SmKeyPair kp = service.generateSm2KeyPair();
            byte[] msg = "original".getBytes(StandardCharsets.UTF_8);
            byte[] tampered = "tampered".getBytes(StandardCharsets.UTF_8);

            byte[] sig = service.sm2Sign(msg, kp.getPrivateKey());
            assertFalse(service.sm2Verify(tampered, sig, kp.getPublicKey()));
        }

        @Test
        @DisplayName("用错误公钥验签失败")
        void verifyWithWrongPublicKeyFails() {
            SmKeyPair kp1 = service.generateSm2KeyPair();
            SmKeyPair kp2 = service.generateSm2KeyPair();
            byte[] msg = "msg".getBytes(StandardCharsets.UTF_8);

            byte[] sig = service.sm2Sign(msg, kp1.getPrivateKey());
            assertFalse(service.sm2Verify(msg, sig, kp2.getPublicKey()));
        }

        @Test
        @DisplayName("用错误私钥解密抛 SmCryptoException")
        void decryptWithWrongPrivateKeyThrows() {
            SmKeyPair kp1 = service.generateSm2KeyPair();
            SmKeyPair kp2 = service.generateSm2KeyPair();
            byte[] plain = "msg".getBytes(StandardCharsets.UTF_8);

            byte[] cipher = service.sm2Encrypt(plain, kp1.getPublicKey());
            assertThrows(SmCryptoException.class,
                    () -> service.sm2Decrypt(cipher, kp2.getPrivateKey()));
        }
    }

    // ============================================================
    // SmKeyPair 防御性拷贝
    // ============================================================

    @Test
    @DisplayName("SmKeyPair 对外暴露的字节是 defensive copy")
    void smKeyPairIsDefensivelyCopied() {
        SmKeyPair kp = service.generateSm2KeyPair();
        byte[] pub1 = kp.getPublicKey();
        byte[] pub2 = kp.getPublicKey();
        assertArrayEquals(pub1, pub2);
        // 修改返回值不应影响内部状态
        pub1[0] = (byte) ~pub1[0];
        assertFalse(java.util.Arrays.equals(pub1, kp.getPublicKey()));
    }
}
