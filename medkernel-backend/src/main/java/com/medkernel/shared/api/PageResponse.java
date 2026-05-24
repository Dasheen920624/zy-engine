package com.medkernel.shared.api;

import java.util.Collections;
import java.util.List;

/**
 * 服务端分页响应。
 *
 * @param items     当前页数据
 * @param page      当前页码（从 1 起）
 * @param size      每页条数
 * @param total     总条数（如果 totalEstimated=true，则为估算值）
 * @param hasNext   是否有下一页
 * @param totalEstimated 是否为估算总数；超大数据集允许估算以避免昂贵 count 查询
 */
public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long total,
    boolean hasNext,
    boolean totalEstimated
) {

    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResponse<T> empty(PageRequest req) {
        return new PageResponse<>(Collections.emptyList(),
            req.safePage(), req.safeSize(), 0L, false, false);
    }

    public static <T> PageResponse<T> of(List<T> items, PageRequest req, long total) {
        int page = req.safePage();
        int size = req.safeSize();
        boolean hasNext = (long) page * size < total;
        return new PageResponse<>(items, page, size, total, hasNext, false);
    }

    public static <T> PageResponse<T> ofEstimated(List<T> items, PageRequest req, long estimatedTotal, boolean hasNext) {
        return new PageResponse<>(items, req.safePage(), req.safeSize(), estimatedTotal, hasNext, true);
    }
}
