package com.medkernel.shared.api;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 游标分页响应。
 *
 * @param items      当前批次数据
 * @param nextCursor 下一批游标；为 null 表示已到末尾
 * @param hasNext    是否还有下一批
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorResponse<T>(
    List<T> items,
    String nextCursor,
    boolean hasNext
) {

    public CursorResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> CursorResponse<T> empty() {
        return new CursorResponse<>(Collections.emptyList(), null, false);
    }

    public static <T> CursorResponse<T> of(List<T> items, String nextCursor) {
        return new CursorResponse<>(items, nextCursor, nextCursor != null);
    }
}
