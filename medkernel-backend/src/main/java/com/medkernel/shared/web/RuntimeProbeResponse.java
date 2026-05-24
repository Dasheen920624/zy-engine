package com.medkernel.shared.web;

/**
 * JDK / 运行时探针响应，验证 Virtual Threads 是否生效、JVM 资源情况等。
 *
 * @param javaVersion         JDK 版本（如 "21.0.5"）
 * @param javaVendor          JVM 厂商
 * @param vmName              VM 名称（如 "OpenJDK 64-Bit Server VM"）
 * @param threadName          当前处理线程名
 * @param virtualThread       当前处理是否在虚拟线程上
 * @param availableProcessors 可用逻辑核数
 * @param freeMemoryMb        JVM 当前空闲堆（MB）
 * @param totalMemoryMb       JVM 当前总堆（MB）
 */
public record RuntimeProbeResponse(
    String javaVersion,
    String javaVendor,
    String vmName,
    String threadName,
    boolean virtualThread,
    int availableProcessors,
    long freeMemoryMb,
    long totalMemoryMb
) {
}
