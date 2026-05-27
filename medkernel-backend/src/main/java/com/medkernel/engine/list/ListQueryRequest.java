package com.medkernel.engine.list;

import java.util.Map;

/**
 * 大规模列表通用检索请求契约。
 *
 * @param resourceType  导出的列表资源类型（如 AUDIT_EVENT）
 * @param pageSize      每页数据大小
 * @param offset        传统分页偏移量（可选）
 * @param cursor        基于 Base64 编码的主键定位游标（游标分页时必传，解析为记录的唯一主键自增 ID，例如 id = 12345）
 * @param sortBy        排序字段名称（默认 id）
 * @param sortOrder     排序规则（ASC 或 DESC，默认 DESC）
 * @param filters       多字段动态筛选条件字典
 */
public record ListQueryRequest(
    String resourceType,
    Integer pageSize,
    Long offset,
    String cursor,
    String sortBy,
    String sortOrder,
    Map<String, String> filters
) {
    /**
     * 对关键参数进行验证并返回默认补全的值。
     */
    public ListQueryRequest normalize() {
        return new ListQueryRequest(
            resourceType == null ? "" : resourceType.trim(),
            (pageSize == null || pageSize <= 0) ? 10 : Math.min(pageSize, 1000),
            offset == null ? 0L : offset,
            cursor == null ? null : cursor.trim(),
            (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim(),
            (sortOrder == null || sortOrder.isBlank()) ? "DESC" : sortOrder.trim().toUpperCase(),
            filters == null ? Map.of() : filters
        );
    }
}
