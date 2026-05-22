package com.medkernel.common.dataclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link DataMaskingService} 单测：6 种 MaskPolicy 边界覆盖。
 */
class DataMaskingServiceTest {

    private DataMaskingService masking;

    @BeforeEach
    void setUp() {
        masking = new DataMaskingService();
    }

    @Nested
    @DisplayName("身份证脱敏 — GB 11643-1999 风格 4+4")
    class IdCard {

        @Test
        @DisplayName("18 位身份证保留前 4 + 后 4")
        void mask18digit() {
            assertEquals("3301**********1234",
                    masking.mask("330106199001011234", Encrypted.MaskPolicy.ID_CARD));
        }

        @Test
        @DisplayName("15 位旧版身份证")
        void mask15digit() {
            assertEquals("3301*******1234",
                    masking.mask("330106900101234", Encrypted.MaskPolicy.ID_CARD));
        }

        @Test
        @DisplayName("过短输入全部 *")
        void shortInputFullyMasked() {
            assertEquals("***", masking.mask("123", Encrypted.MaskPolicy.ID_CARD));
        }
    }

    @Nested
    @DisplayName("手机号脱敏 — 3+4")
    class Phone {

        @Test
        @DisplayName("11 位手机号")
        void mask11digit() {
            assertEquals("138****8000", masking.mask("13800138000", Encrypted.MaskPolicy.PHONE));
        }

        @Test
        @DisplayName("座机号兼容")
        void maskLandline() {
            assertEquals("010****1234", masking.mask("01087651234", Encrypted.MaskPolicy.PHONE));
        }
    }

    @Nested
    @DisplayName("邮箱脱敏 — 首字母 + ***@domain")
    class Email {

        @Test
        void normalEmail() {
            assertEquals("z******@medkernel.com",
                    masking.mask("zhangsan@medkernel.com", Encrypted.MaskPolicy.EMAIL));
        }

        @Test
        void singleCharLocal() {
            assertEquals("*@x.cn", masking.mask("a@x.cn", Encrypted.MaskPolicy.EMAIL));
        }

        @Test
        void noAtSignFullyMasked() {
            // "not-an-email" 长度 12 → 全部替换为 12 个 *
            assertEquals("************", masking.mask("not-an-email", Encrypted.MaskPolicy.EMAIL));
        }
    }

    @Nested
    @DisplayName("姓名脱敏 — 保留首字")
    class Name {

        @Test
        void twoCharName() {
            assertEquals("张*", masking.mask("张三", Encrypted.MaskPolicy.NAME));
        }

        @Test
        void threeCharName() {
            assertEquals("王**", masking.mask("王小明", Encrypted.MaskPolicy.NAME));
        }

        @Test
        void singleCharNameKept() {
            assertEquals("张", masking.mask("张", Encrypted.MaskPolicy.NAME));
        }
    }

    @Nested
    @DisplayName("地址脱敏 — 保留前 6 字符")
    class Address {

        @Test
        void longAddress() {
            assertEquals("北京市朝阳区***",
                    masking.mask("北京市朝阳区建国门外大街 100 号", Encrypted.MaskPolicy.ADDRESS));
        }

        @Test
        void shortAddressFullyMasked() {
            assertEquals("****", masking.mask("北京市朝", Encrypted.MaskPolicy.ADDRESS));
        }
    }

    @Nested
    @DisplayName("FULL / NONE / null 边界")
    class Misc {

        @Test
        void fullPolicy() {
            assertEquals("******", masking.mask("secret", Encrypted.MaskPolicy.FULL));
        }

        @Test
        void nonePolicyKeepsValue() {
            assertEquals("any", masking.mask("any", Encrypted.MaskPolicy.NONE));
        }

        @Test
        void nullInputReturnsNull() {
            assertNull(masking.mask(null, Encrypted.MaskPolicy.PHONE));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", masking.mask("", Encrypted.MaskPolicy.PHONE));
        }
    }

    @Nested
    @DisplayName("entity 全量脱敏")
    class EntityMasking {

        @Test
        void maskEntity_appliesPerFieldPolicy() {
            SampleEntity e = new SampleEntity();
            e.setIdCard("330106199001011234");
            e.setPhone("13800138000");
            e.setName("张三");

            masking.maskEntity(e);

            assertEquals("3301**********1234", e.getIdCard());
            assertEquals("138****8000", e.getPhone());
            assertEquals("张*", e.getName());
        }
    }

    @DataClass(DataClassification.HEALTH_DATA)
    static class SampleEntity {
        @Encrypted(maskPolicy = Encrypted.MaskPolicy.ID_CARD)
        private String idCard;
        @Encrypted(maskPolicy = Encrypted.MaskPolicy.PHONE)
        private String phone;
        @Encrypted(maskPolicy = Encrypted.MaskPolicy.NAME)
        private String name;

        public String getIdCard() { return idCard; }
        public void setIdCard(String v) { this.idCard = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { this.phone = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
    }
}
