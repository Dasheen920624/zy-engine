package com.medkernel.common.dataclass;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link DataClassification} 单测：确保枚举顺序 + 加密 / 脱敏判断稳定。
 */
class DataClassificationTest {

    @Test
    @DisplayName("HEALTH_DATA 强制加密")
    void healthDataRequiresEncryption() {
        assertTrue(DataClassification.HEALTH_DATA.requiresEncryption());
        assertFalse(DataClassification.SENSITIVE.requiresEncryption());
        assertFalse(DataClassification.INTERNAL.requiresEncryption());
        assertFalse(DataClassification.PUBLIC.requiresEncryption());
    }

    @Test
    @DisplayName("SENSITIVE 及以上需脱敏")
    void sensitiveAndAboveRequireMasking() {
        assertTrue(DataClassification.HEALTH_DATA.requiresMasking());
        assertTrue(DataClassification.SENSITIVE.requiresMasking());
        assertFalse(DataClassification.INTERNAL.requiresMasking());
        assertFalse(DataClassification.PUBLIC.requiresMasking());
    }

    @Test
    @DisplayName("枚举 ordinal 顺序固定")
    void ordinalOrderFixed() {
        assertTrue(DataClassification.PUBLIC.ordinal() < DataClassification.INTERNAL.ordinal());
        assertTrue(DataClassification.INTERNAL.ordinal() < DataClassification.SENSITIVE.ordinal());
        assertTrue(DataClassification.SENSITIVE.ordinal() < DataClassification.HEALTH_DATA.ordinal());
    }
}
