package com.medkernel.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ID 生成器：生成唯一 ID，格式为 prefix-timestamp-random。
 * 符合开发规范 §8.1：应用层生成，禁止依赖 AUTO_INCREMENT / SEQUENCE。
 */
public final class IdGenerator {

    private IdGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成唯一 ID。
     *
     * @param prefix 前缀，如 PKG、SEC、BIND 等
     * @return 唯一 ID
     */
    public static Long next(String prefix) {
        // 使用当前时间戳（毫秒）的低 32 位，避免 Long 溢出
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFL;
        // 随机数 0-65535
        int random = ThreadLocalRandom.current().nextInt(0x10000);
        // 组合：时间戳左移 16 位 + 随机数
        return (timestamp << 16) | random;
    }

    /**
     * 生成带前缀的字符串 ID（用于日志或显示）。
     *
     * @param prefix 前缀
     * @return 带前缀的 ID 字符串
     */
    public static String nextString(String prefix) {
        return prefix + "-" + next(prefix);
    }
}