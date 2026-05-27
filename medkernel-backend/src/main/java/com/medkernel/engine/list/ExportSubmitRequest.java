package com.medkernel.engine.list;

import java.util.Map;

/**
 * 异步批量导出任务提交请求。
 *
 * @param resourceType  导出的列表资源类型（如 AUDIT_EVENT）
 * @param filters       多字段动态筛选条件字典
 */
public record ExportSubmitRequest(
    String resourceType,
    Map<String, String> filters
) {}
