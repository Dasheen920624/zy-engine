package com.medkernel.shared.observability;

/**
 * 持久化 payload 的引用信息。
 *
 * <p>当前默认实现使用 storageType=INLINE；未来切对象存储时新写入 storageType=URI、
 * uri 填外部地址，老数据保持 INLINE，{@link PayloadStoragePort#get(PayloadRef)} 兼容
 * 两种来源。
 *
 * @param storageType  "INLINE" 或 "URI"
 * @param digest       SM3 摘要
 * @param uri          INLINE 时填 "db://table/id"；URI 时填外部存储 URI（mc://、oss://）
 * @param sizeBytes    字节数
 */
public record PayloadRef(
    String storageType,
    String digest,
    String uri,
    long sizeBytes
) {

    public static final String STORAGE_INLINE = "INLINE";
    public static final String STORAGE_URI = "URI";
}
