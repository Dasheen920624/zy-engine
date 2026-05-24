package com.medkernel.shared.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 游标分页请求。适用于按时间序滚动、审计日志、证据流、大规模搜索结果等场景。
 *
 * @param cursor 游标，首次请求传 null；服务端用上一次响应的 nextCursor 续翻
 * @param size   每页条数，默认 50，最大 500（游标模式比 offset 模式宽松）
 */
public record CursorRequest(
    String cursor,
    @Min(1) @Max(500) Integer size
) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 500;

    public static CursorRequest first() {
        return new CursorRequest(null, DEFAULT_SIZE);
    }

    public int safeSize() {
        return size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
