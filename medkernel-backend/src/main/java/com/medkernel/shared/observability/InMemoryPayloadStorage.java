package com.medkernel.shared.observability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 payload 存储默认 in-memory 实现。
 *
 * <p>使用 ConcurrentHashMap 持久化在 JVM 内存，仅供 dev/test 与 OBS-01 单独验收。
 * 第三层 API-02 引入 {@code DbPayloadStorage} 后通过 {@code @Primary} 自动覆盖此默认实现。
 *
 * <p>digest 算法：SHA-256（GA 阶段；后续可切 SM3）。
 */
public class InMemoryPayloadStorage implements PayloadStoragePort {

    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public PayloadRef put(PayloadDescriptor descriptor, byte[] payload) {
        String digest = sha256(payload);
        String uri = String.format("inmem://%s/%s/%s",
            descriptor.tenantId(), descriptor.entityType(), descriptor.entityId());
        store.put(uri, payload.clone());
        return new PayloadRef(PayloadRef.STORAGE_INLINE, digest, uri, payload.length);
    }

    @Override
    public byte[] get(PayloadRef ref) {
        byte[] payload = store.get(ref.uri());
        if (payload == null) {
            throw new ApiException(ErrorCode.ENG_OBS_001,
                "payload 不存在: " + ref.uri());
        }
        return payload.clone();
    }

    @Override
    public void delete(PayloadRef ref) {
        store.remove(ref.uri());
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
