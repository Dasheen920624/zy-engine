package com.medkernel.compliance.masking;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.medkernel.shared.crypto.SmCryptoService;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingServiceTest {

    private final SmCryptoService crypto = create();
    private final MaskingService masking = new MaskingService(crypto);

    private static SmCryptoService create() {
        // BC Provider 由 SmCryptoService 静态块自动注册，无需手动 init()
        return new SmCryptoService();
    }

    @Test
    void devProfileSemiMasksName() {
        Map<String, String> out = masking.maskRecord(Map.of("name", "张三丰", "phone", "13812345678"), MaskingProfile.DEV);
        assertThat(out.get("name")).isEqualTo("张**");
        assertThat(out.get("phone")).isEqualTo("138****5678");
    }

    @Test
    void exportProfileFullyPseudonymizes() {
        Map<String, String> out = masking.maskRecord(
            Map.of("idCard", "110101199001011234", "phone", "13812345678", "name", "张三丰"),
            MaskingProfile.EXPORT
        );
        assertThat(out.get("idCard")).startsWith("p-");
        assertThat(out.get("phone")).startsWith("p-");
        assertThat(out.get("name")).startsWith("p-");
    }
}
