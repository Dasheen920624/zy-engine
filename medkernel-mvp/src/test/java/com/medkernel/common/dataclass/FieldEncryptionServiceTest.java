package com.medkernel.common.dataclass;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Security;
import java.util.Base64;

import com.medkernel.common.crypto.SmCryptoException;
import com.medkernel.common.crypto.SmCryptoService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link FieldEncryptionService} 单测：覆盖 entity 透明加解密、密文格式、密钥派生、
 * 严格模式回归、null/空值边界。
 */
class FieldEncryptionServiceTest {

    private SmCryptoService smCrypto;
    private FieldEncryptionProperties properties;
    private FieldEncryptionService service;

    @BeforeAll
    static void registerBc() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void initService() {
        smCrypto = new SmCryptoService();
        properties = new FieldEncryptionProperties();
        properties.setEnabled(true);
        properties.setMasterKeyBase64(Base64.getEncoder().encodeToString(new byte[]{
                (byte) 0xA0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
                (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB,
                (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF
        }));
        properties.setDerivationSalt("test-salt-v1");
        properties.setStrictDecrypt(true);
        service = new FieldEncryptionService(smCrypto, properties);
    }

    @Nested
    @DisplayName("字符串加解密往返")
    class StringRoundTrip {

        @Test
        @DisplayName("基础往返 — UTF-8 中文")
        void roundTrip_chinese() {
            String plain = "身份证 GB 11643-1999 18 位编号";
            String cipher = service.encryptString(plain, "md_patient");
            assertNotEquals(plain, cipher);
            assertTrue(service.isCipherText(cipher));
            assertEquals(plain, service.decryptString(cipher, "md_patient"));
        }

        @Test
        @DisplayName("同一明文每次加密结果不同（IV 随机）")
        void cipherTextsDiffer_dueToRandomIv() {
            String plain = "13800138000";
            String c1 = service.encryptString(plain, "md_patient");
            String c2 = service.encryptString(plain, "md_patient");
            assertNotEquals(c1, c2);
            assertEquals(plain, service.decryptString(c1, "md_patient"));
            assertEquals(plain, service.decryptString(c2, "md_patient"));
        }

        @Test
        @DisplayName("不同表名派生不同密钥 — 跨表密文不可互换")
        void crossTableCipherNotInterchangeable() {
            String plain = "shared-value";
            String c = service.encryptString(plain, "table_a");
            assertThrows(SmCryptoException.class,
                    () -> service.decryptString(c, "table_b"));
        }

        @Test
        @DisplayName("密文带版本头 SM4:v1:")
        void cipherTextHasVersionHeader() {
            String c = service.encryptString("x", "md_patient");
            byte[] raw = Base64.getDecoder().decode(c);
            String header = new String(raw, 0, 7, java.nio.charset.StandardCharsets.US_ASCII);
            assertEquals("SM4:v1:", header);
        }
    }

    @Nested
    @DisplayName("Entity 反射加解密")
    class EntityRoundTrip {

        @Test
        @DisplayName("@Encrypted 字段就地加密 + 解密")
        void entityRoundTrip() {
            SampleEntity e = new SampleEntity();
            e.setNonEncrypted("public-id");
            e.setSecret("330106199001011234");
            e.setPhone("13800138000");

            service.encryptEntity(e);
            assertEquals("public-id", e.getNonEncrypted());
            assertNotEquals("330106199001011234", e.getSecret());
            assertNotEquals("13800138000", e.getPhone());
            assertTrue(service.isCipherText(e.getSecret()));
            assertTrue(service.isCipherText(e.getPhone()));

            service.decryptEntity(e);
            assertEquals("330106199001011234", e.getSecret());
            assertEquals("13800138000", e.getPhone());
        }

        @Test
        @DisplayName("null 字段保持 null")
        void nullFieldsKeepNull() {
            SampleEntity e = new SampleEntity();
            e.setSecret(null);
            service.encryptEntity(e);
            assertNull(e.getSecret());
            service.decryptEntity(e);
            assertNull(e.getSecret());
        }

        @Test
        @DisplayName("重复加密幂等 — 已加密字段跳过")
        void encryptIsIdempotent() {
            SampleEntity e = new SampleEntity();
            e.setSecret("plain");
            service.encryptEntity(e);
            String onceEncrypted = e.getSecret();
            service.encryptEntity(e);
            assertEquals(onceEncrypted, e.getSecret(), "已加密的字段第二次调用应保持不变");
        }

        @Test
        @DisplayName("enabled=false 时 no-op")
        void disabledIsNoOp() {
            properties.setEnabled(false);
            service = new FieldEncryptionService(smCrypto, properties);
            SampleEntity e = new SampleEntity();
            e.setSecret("plain");
            service.encryptEntity(e);
            assertEquals("plain", e.getSecret());
        }

        @Test
        @DisplayName("严格模式下解密非密文抛异常")
        void strictDecrypt_throwsOnPlaintext() {
            SampleEntity e = new SampleEntity();
            e.setSecret("not-an-encrypted-value");
            assertThrows(SmCryptoException.class, () -> service.decryptEntity(e));
        }

        @Test
        @DisplayName("非严格模式下解密非密文保留原值")
        void looseDecrypt_keepsPlaintext() {
            properties.setStrictDecrypt(false);
            service = new FieldEncryptionService(smCrypto, properties);
            SampleEntity e = new SampleEntity();
            e.setSecret("legacy-not-encrypted");
            service.decryptEntity(e);
            assertEquals("legacy-not-encrypted", e.getSecret());
        }

        @Test
        @DisplayName("继承字段 — 父类标 @Encrypted 也加密")
        void inheritedEncryptedFields() {
            ChildEntity child = new ChildEntity();
            child.setSecret("base-secret");
            child.setChildOnly("child-secret");

            service.encryptEntity(child);
            assertTrue(service.isCipherText(child.getSecret()));
            assertTrue(service.isCipherText(child.getChildOnly()));

            service.decryptEntity(child);
            assertEquals("base-secret", child.getSecret());
            assertEquals("child-secret", child.getChildOnly());
        }
    }

    @Nested
    @DisplayName("密钥派生")
    class KeyDerivation {

        @Test
        @DisplayName("同 master+salt+table 派生固定 16 字节 dek")
        void deriveStableKey() {
            byte[] k1 = service.deriveTableKey("md_patient");
            byte[] k2 = service.deriveTableKey("md_patient");
            assertArrayEquals(k1, k2);
            assertEquals(16, k1.length);
        }

        @Test
        @DisplayName("不同表名派生不同 dek")
        void differentTablesDifferKeys() {
            byte[] kA = service.deriveTableKey("table_a");
            byte[] kB = service.deriveTableKey("table_b");
            assertFalse(java.util.Arrays.equals(kA, kB));
        }

        @Test
        @DisplayName("不同 master key 派生不同 dek")
        void differentMasterKeysDifferDek() {
            byte[] firstDek = service.deriveTableKey("t");

            FieldEncryptionProperties other = new FieldEncryptionProperties();
            other.setMasterKeyBase64(Base64.getEncoder().encodeToString(new byte[16]));
            other.setDerivationSalt(properties.getDerivationSalt());
            FieldEncryptionService otherSvc = new FieldEncryptionService(smCrypto, other);
            byte[] secondDek = otherSvc.deriveTableKey("t");

            assertFalse(java.util.Arrays.equals(firstDek, secondDek));
        }
    }

    @Nested
    @DisplayName("master key 校验")
    class MasterKeyValidation {

        @Test
        @DisplayName("master key 长度不为 16 字节抛异常")
        void wrongKeyLengthThrows() {
            properties.setMasterKeyBase64(Base64.getEncoder().encodeToString(new byte[8]));
            assertThrows(SmCryptoException.class,
                    () -> new FieldEncryptionService(smCrypto, properties));
        }

        @Test
        @DisplayName("master key 为 null 抛异常")
        void nullKeyThrows() {
            properties.setMasterKeyBase64(null);
            assertThrows(SmCryptoException.class,
                    () -> new FieldEncryptionService(smCrypto, properties));
        }
    }

    @Nested
    @DisplayName("isCipherText 边界")
    class IsCipherText {

        @Test
        void detectsRealCipherText() {
            String c = service.encryptString("hello", "t");
            assertTrue(service.isCipherText(c));
        }

        @Test
        void rejectsRawPlaintext() {
            assertFalse(service.isCipherText("hello"));
            assertFalse(service.isCipherText(""));
            assertFalse(service.isCipherText("not-base64-@#"));
        }

        @Test
        void rejectsBase64WithoutHeader() {
            String fakeBase64 = Base64.getEncoder().encodeToString("random-content".getBytes());
            assertFalse(service.isCipherText(fakeBase64));
        }

        @Test
        void rejectsNull() {
            assertFalse(service.isCipherText(null));
        }
    }

    // ============================================================
    // 测试用 fixture entity
    // ============================================================

    @DataClass(DataClassification.HEALTH_DATA)
    static class SampleEntity {
        private String nonEncrypted;
        @Encrypted
        private String secret;
        @Encrypted(maskPolicy = Encrypted.MaskPolicy.PHONE)
        private String phone;

        public String getNonEncrypted() { return nonEncrypted; }
        public void setNonEncrypted(String v) { this.nonEncrypted = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { this.phone = v; }
    }

    @DataClass(DataClassification.HEALTH_DATA)
    static class ChildEntity extends SampleEntity {
        @Encrypted
        private String childOnly;
        public String getChildOnly() { return childOnly; }
        public void setChildOnly(String v) { this.childOnly = v; }
    }
}
