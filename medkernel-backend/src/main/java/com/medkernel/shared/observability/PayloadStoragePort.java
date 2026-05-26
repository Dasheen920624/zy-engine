package com.medkernel.shared.observability;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 payload 存储端口。
 *
 * <p>所有大字段 payload 的持久化与读取一律走本接口，与底层介质解耦。
 *
 * <p>当前默认实现：{@code InMemoryPayloadStorage}（ConcurrentHashMap）；
 * 第三层 API-02 引入 {@code DbPayloadStorage} 写入 clinical_event_payload 旁路表；
 * 未来切 OSS 时新增 {@code OssPayloadStorage} + 配置切换即可，主表零改动。
 */
public interface PayloadStoragePort {

    /**
     * 持久化 payload，返回引用。
     *
     * @param descriptor 元信息
     * @param payload    原始字节
     * @return           {@link PayloadRef}，含 SM3 digest 与定位信息
     */
    PayloadRef put(PayloadDescriptor descriptor, byte[] payload);

    /**
     * 按 PayloadRef 取 payload；不存在或已归档时抛 ApiException(ENG-OBS-001)。
     */
    byte[] get(PayloadRef ref);

    /**
     * 软删除（标记 deleted_at）；GA 阶段不真正物理删除，由归档任务统一处理。
     */
    void delete(PayloadRef ref);
}
