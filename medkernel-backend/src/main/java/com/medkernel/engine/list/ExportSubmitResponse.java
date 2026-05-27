package com.medkernel.engine.list;

/**
 * 异步批量导出任务提交响应。
 *
 * @param jobId   生成的唯一任务ID，用于后续轮询状态和物理下载
 * @param status  当前任务处理状态（PENDING, RUNNING, SUCCESS, FAILED）
 * @param message 友好操作状态提示
 */
public record ExportSubmitResponse(
    String jobId,
    String status,
    String message
) {}
