package com.medkernel.shared.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 标准服务端分页请求参数。
 *
 * <p>大规模列表（知识、字典、规则、日志、证据等）必须使用服务端分页，不得前端全量加载，
 * 详见 [产品体验固定规范]。游标分页另见 {@link CursorRequest}。
 *
 * @param page 页码，从 1 起
 * @param size 每页条数，默认 20，最大 200
 * @param sort 排序表达式，如 "updatedAt,desc"
 */
public record PageRequest(
    @Min(1) Integer page,
    @Min(1) @Max(200) Integer size,
    String sort
) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public static PageRequest defaults() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE, null);
    }

    public int safePage() {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public int safeSize() {
        return size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public int offset() {
        return (safePage() - 1) * safeSize();
    }
}
