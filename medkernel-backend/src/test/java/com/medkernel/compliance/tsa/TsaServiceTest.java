package com.medkernel.compliance.tsa;

import org.junit.jupiter.api.Test;

import com.medkernel.shared.crypto.SmCryptoService;

import static org.assertj.core.api.Assertions.assertThat;

class TsaServiceTest {

    @Test
    void stampReturnsSerialPlusSm3Hash() {
        SmCryptoService crypto = new SmCryptoService();
        crypto.init();
        TsaService tsa = new TsaService(crypto);
        var token = tsa.stamp("MedKernel · 审计快照 · TSA 测试向量");
        assertThat(token.serial()).startsWith("TSA-");
        assertThat(token.timestamp()).isNotBlank();
        assertThat(token.sm3HashHex()).hasSize(64);
    }
}
