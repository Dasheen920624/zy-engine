package com.medkernel.engine.list;

import java.util.List;

/**
 * 大大规模列表检索通用返回结果。
 *
 * @param <T>           实体记录类型
 * @param nextCursor    下一个游标标识，由 base64 编码的最后一条记录主键组成。若为空，代表无后续数据。
 * @param records       当前分页返回的实体列表
 * @param totalEstimate 估算总行数（防范大宽表 count(*) 导致的慢 SQL 性能惩罚）
 * @param hasMore       是否含有更多待读取数据
 */
public record ListQueryResponse<T>(
    String nextCursor,
    List<T> records,
    Long totalEstimate,
    Boolean hasMore
) {}
