package com.medkernel.engine.knowledge;

/**
 * 知识异步导出作业状态机。对应 {@code knowledge_export_job.status} CHECK 约束。
 *
 * <pre>
 *   PENDING ──worker 领取──&gt; RUNNING ─┬─完成─&gt; SUCCEEDED
 *                                     ├─失败─&gt; FAILED
 *                                     └─超时/取消─&gt; CANCELLED
 *   SUCCEEDED ──TTL 到期──&gt; EXPIRED
 * </pre>
 */
public enum ExportStatus {
    /** 已提交，等待 worker 领取 */
    PENDING,
    /** 正在执行 */
    RUNNING,
    /** 成功，result_uri 可下载 */
    SUCCEEDED,
    /** 失败，error_message 含原因 */
    FAILED,
    /** 结果文件 TTL 到期，已清理（仍可重发起新作业） */
    EXPIRED,
    /** 用户取消 / 系统超时取消 */
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
}
