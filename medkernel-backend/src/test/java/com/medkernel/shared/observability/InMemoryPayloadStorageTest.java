package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

class InMemoryPayloadStorageTest {

    private InMemoryPayloadStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryPayloadStorage();
    }

    @Test
    void putReturnsRefWithDigestAndSize() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        byte[] payload = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref = storage.put(descriptor, payload);

        assertThat(ref.storageType()).isEqualTo(PayloadRef.STORAGE_INLINE);
        assertThat(ref.digest()).isNotBlank();
        assertThat(ref.sizeBytes()).isEqualTo(payload.length);
        assertThat(ref.uri()).startsWith("inmem://");
    }

    @Test
    void getReturnsExactBytes() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        byte[] payload = "hello 你好".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref = storage.put(descriptor, payload);
        byte[] back = storage.get(ref);

        assertThat(back).isEqualTo(payload);
    }

    @Test
    void getReturnsSameDigestOnIdenticalPayload() {
        var descriptor1 = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        var descriptor2 = new PayloadDescriptor("tenant-A", "clinical_event", "evt-2", "application/json");
        byte[] payload = "same content".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref1 = storage.put(descriptor1, payload);
        PayloadRef ref2 = storage.put(descriptor2, payload);

        assertThat(ref1.digest()).isEqualTo(ref2.digest());
        assertThat(ref1.uri()).isNotEqualTo(ref2.uri());  // 但 uri 不同（按 entityId 区分）
    }

    @Test
    void getMissingThrowsApiExceptionWithObs001() {
        PayloadRef bogus = new PayloadRef(PayloadRef.STORAGE_INLINE, "deadbeef", "inmem://nonexistent", 0L);

        assertThatThrownBy(() -> storage.get(bogus))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_OBS_001);
    }

    @Test
    void deleteMarksRefAsRemoved() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        PayloadRef ref = storage.put(descriptor, "data".getBytes(StandardCharsets.UTF_8));

        storage.delete(ref);

        assertThatThrownBy(() -> storage.get(ref))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_OBS_001);
    }
}
