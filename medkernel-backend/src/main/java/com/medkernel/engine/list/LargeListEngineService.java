package com.medkernel.engine.list;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.audit.persistence.AuditEventQuery;
import com.medkernel.shared.audit.persistence.AuditEventRecord;
import com.medkernel.shared.audit.persistence.AuditEventRepository;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 大规模数据列表检索与异步批量导出核心服务引擎。
 *
 * <p>提供高性能的列表检索（含游标分页、Total Estimate 行数近似优化）以及分批异步 CSV 导出。
 */
@Service
public class LargeListEngineService {

    private static final Logger log = LoggerFactory.getLogger(LargeListEngineService.class);

    private final LargeListExportJobRepository jobRepository;
    private final AuditEventRepository auditRepository;
    private final AuditEventPublisher auditPublisher;
    private final IsolatedAuditPublisher isolatedAudit;
    private final JdbcTemplate jdbc;
    private final Executor knowledgeExportExecutor;

    public LargeListEngineService(
        LargeListExportJobRepository jobRepository,
        AuditEventRepository auditRepository,
        AuditEventPublisher auditPublisher,
        IsolatedAuditPublisher isolatedAudit,
        JdbcTemplate jdbc,
        @Qualifier("knowledgeExportExecutor") Executor knowledgeExportExecutor
    ) {
        this.jobRepository = jobRepository;
        this.auditRepository = auditRepository;
        this.auditPublisher = auditPublisher;
        this.isolatedAudit = isolatedAudit;
        this.jdbc = jdbc;
        this.knowledgeExportExecutor = knowledgeExportExecutor;
    }

    /**
     * 高性能大规模列表检索。
     *
     * <p>支持解析 Base64 主键物理游标，以规避深分页导致的慢 SQL 问题。
     * 同时支持 Total Estimate 近似总行数估算，以防止全表 Count 锁表。
     *
     * @param request 列表查询入参
     * @return 统一列表检索出参
     */
    @Transactional(readOnly = true)
    public ListQueryResponse<AuditEventRecord> queryList(ListQueryRequest request) {
        String tenantId = requireCurrentTenant();
        ListQueryRequest norm = request.normalize();

        // 仅当资源类型为 AUDIT_EVENT 或为空时允许检索
        if (!"AUDIT_EVENT".equalsIgnoreCase(norm.resourceType()) && !norm.resourceType().isBlank()) {
            throw new ApiException(ErrorCode.ENG_LIST_001, "不支持的列表检索资源类型: " + norm.resourceType());
        }

        // 解析 Base64 游标
        Long cursorId = null;
        if (norm.cursor() != null && !norm.cursor().isBlank()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(norm.cursor()));
                cursorId = Long.parseLong(decoded);
            } catch (Exception e) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "非法的 Base64 列表游标格式");
            }
        }

        // 构造底座标准的审计过滤条件
        String actionFilter = norm.filters().get("action");
        String resourceTypeFilter = norm.filters().get("resourceType");
        String actorFilter = norm.filters().get("actorUserId");
        
        AuditEventQuery query = new AuditEventQuery(
            actionFilter,
            resourceTypeFilter,
            actorFilter,
            null,
            null,
            cursorId,
            norm.pageSize()
        );

        // findPage 会查出 pageSize + 1 条，以便判断 hasMore
        List<AuditEventRecord> rows = auditRepository.findPage(tenantId, query);

        boolean hasMore = false;
        String nextCursor = null;
        List<AuditEventRecord> records = rows;

        if (rows.size() > norm.pageSize()) {
            hasMore = true;
            records = rows.subList(0, norm.pageSize());
            // 取最后一条的实际物理 ID 编码为游标
            AuditEventRecord last = records.get(records.size() - 1);
            nextCursor = Base64.getEncoder().encodeToString(String.valueOf(last.id()).getBytes());
        }

        // 计算 Total Estimate 近似总行数 (限流 10000 条以防 count(*) 全表扫描)
        long totalEstimate = estimateCount(tenantId, actionFilter, resourceTypeFilter, actorFilter);

        return new ListQueryResponse<>(nextCursor, records, totalEstimate, hasMore);
    }

    /**
     * 近似总行数估算，使用 LIMIT 限制以提升海量数据 Count 性能。
     */
    private long estimateCount(String tenantId, String action, String resourceType, String actor) {
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM (SELECT 1 FROM audit_event WHERE tenant_id = ?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(tenantId);

        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (actor != null && !actor.isBlank()) {
            sql.append(" AND actor_user_id = ?");
            params.add(actor);
        }
        
        // 限制最多 Count 至 10001 条
        sql.append(" LIMIT 10001) t");

        try {
            Long val = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
            long count = val == null ? 0L : val;
            return count > 10000 ? 10000L : count;
        } catch (Exception e) {
            log.warn("Total estimate count failed, fallback to 0", e);
            return 0L;
        }
    }

    /**
     * 提交异步大规模列表批量导出任务。
     *
     * @param request 异步导出请求参数
     * @return 导出任务提交回执
     */
    @Transactional
    public ExportSubmitResponse submitExportTask(ExportSubmitRequest request) {
        String tenantId = requireCurrentTenant();
        String jobId = UUID.randomUUID().toString();
        String traceId = RequestContext.currentTraceId();
        String creator = currentActor();

        // 资源类型强校验
        if (!"AUDIT_EVENT".equalsIgnoreCase(request.resourceType())) {
            throw new ApiException(ErrorCode.ENG_LIST_001, "仅支持对 AUDIT_EVENT 资源进行大规模异步导出");
        }

        // 解析并存储过滤参数 JSON 占位
        String filterJson = request.filters() == null ? "{}" : request.filters().toString();

        LargeListExportJob job = LargeListExportJob.createPending(
            jobId,
            tenantId,
            request.resourceType(),
            filterJson,
            traceId,
            creator
        );

        LargeListExportJob saved = jobRepository.save(job);
        log.info("Successfully submitted large list export job, jobId={}, tenantId={}", jobId, tenantId);

        // 传递当前请求上下文快照给后台异步线程
        RequestContext.Snapshot snapshot = RequestContext.snapshot();

        Runnable worker = () -> RequestContext.runWith(snapshot, () -> {
            try {
                executeExport(jobId);
            } catch (Exception e) {
                log.error("Failed to execute large list export job: {}", jobId, e);
                markExportFailed(jobId, e.getMessage());
            }
        });

        // 注册事务提交流程以防止幻读或数据未完全落库
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    knowledgeExportExecutor.execute(worker);
                }
            });
        } else {
            knowledgeExportExecutor.execute(worker);
        }

        return new ExportSubmitResponse(saved.jobId(), "PENDING", "导出任务已提交后台处理");
    }

    /**
     * 根据 jobId 获取异步导出作业元数据。
     *
     * @param jobId 任务唯一ID
     * @return 任务详情
     */
    public LargeListExportJob getExportJob(String jobId) {
        String tenantId = requireCurrentTenant();
        return jobRepository.findByJobId(jobId)
            .filter(j -> j.tenantId().equals(tenantId))
            .orElseThrow(() -> ApiException.notFound("指定的异步导出任务不存在: " + jobId));
    }

    /**
     * 后台线程实际执行大规模列表数据的分批拉取与 CSV 文件物理生成。
     */
    void executeExport(String jobId) throws IOException {
        String tenantId = requireCurrentTenant();
        LargeListExportJob job = jobRepository.findByJobId(jobId)
            .filter(j -> j.tenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalStateException("导出任务不存在，jobId=" + jobId));

        if (!"PENDING".equals(job.status())) {
            log.warn("Job {} status is {}, skip running", jobId, job.status());
            return;
        }

        Instant startedAt = Instant.now();

        // 变更状态为 RUNNING
        updateJobStatus(job, "RUNNING", null, null, 0L, null);

        // 提取过滤条件
        Map<String, String> filterMap = Map.of();
        String actionFilter = null;
        String resourceTypeFilter = null;
        String actorFilter = null;

        // 生成本地临时文件
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "medkernel-exports");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File csvFile = new File(tempDir, "export-" + jobId + ".csv");

        long count = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            // 写入 CSV 头部
            writer.write("\uFEFF"); // 写入 UTF-8 BOM，防止 Excel 乱码
            writer.write("自增ID,事件ID,追踪ID,发生时间,操作人ID,操作动作,资源类型,资源ID,摘要, outcome,错误码\n");

            Long cursorId = null;
            boolean hasNext = true;
            int batchSize = 500;

            while (hasNext) {
                AuditEventQuery query = new AuditEventQuery(
                    actionFilter,
                    resourceTypeFilter,
                    actorFilter,
                    null,
                    null,
                    cursorId,
                    batchSize
                );

                List<AuditEventRecord> rows = auditRepository.findPage(tenantId, query);
                if (rows.isEmpty()) {
                    break;
                }

                List<AuditEventRecord> batchList = rows;
                if (rows.size() > batchSize) {
                    batchList = rows.subList(0, batchSize);
                    cursorId = batchList.get(batchList.size() - 1).id();
                } else {
                    hasNext = false;
                }

                for (AuditEventRecord row : batchList) {
                    writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        row.id(),
                        escapeCsv(row.eventId()),
                        escapeCsv(row.traceId()),
                        row.occurredAt() == null ? "" : row.occurredAt().toString(),
                        escapeCsv(row.actorUserId()),
                        row.action() == null ? "" : row.action(),
                        escapeCsv(row.resourceType()),
                        escapeCsv(row.resourceId()),
                        escapeCsv(row.summary()),
                        escapeCsv(row.outcome()),
                        escapeCsv(row.errorCode())
                    ));
                    count++;
                }
            }
            writer.flush();
        }

        Instant completedAt = Instant.now();
        long costMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();

        // 成功状态变更并记录物理文件路径及大小
        updateJobStatus(job, "SUCCESS", csvFile.getName(), csvFile.getAbsolutePath(), csvFile.length(), costMs);

        // 发布成功物理审计事件
        auditPublisher.publish(AuditEvent.of(
            AuditAction.EXPORT,
            "large_list_export",
            jobId,
            "异步导出大规模列表数据至 CSV 成功，共 " + count + " 条记录，耗时 " + costMs + "ms"
        ));

        log.info("Successfully completed list export job: {}, total entries={}", jobId, count);
    }

    /**
     * 标记导出任务为失败。
     */
    void markExportFailed(String jobId, String errorMsg) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            updateJobStatus(job, "FAILED", null, null, 0L, null);
            // 写入失败时包含的具体堆栈错误描述
            LargeListExportJob refreshed = jobRepository.findByJobId(jobId).orElse(job);
            jobRepository.save(new LargeListExportJob(
                refreshed.id(),
                refreshed.jobId(),
                refreshed.tenantId(),
                refreshed.resourceType(),
                refreshed.filterCriteria(),
                "FAILED",
                null,
                null,
                0L,
                errorMsg == null ? "未知异常" : errorMsg.substring(0, Math.min(errorMsg.length(), 500)),
                refreshed.timeCostMs(),
                refreshed.traceId(),
                refreshed.createdAt(),
                refreshed.createdBy(),
                Instant.now(),
                refreshed.updatedBy()
            ));

            // 通过物理子事务发布失败审计记录
            isolatedAudit.publishInNewTx(AuditEvent.failure(
                AuditAction.EXPORT,
                "large_list_export",
                jobId,
                ErrorCode.ENG_LIST_004.code(),
                "大规模列表 CSV 后台导出失败: " + errorMsg
            ));
        });
    }

    /**
     * 获取物理 CSV 文件的输入流以便 Controller 输出下载。
     *
     * @param jobId 任务唯一ID
     * @return 文件的物理输入流
     */
    public FileInputStream downloadFile(String jobId) {
        LargeListExportJob job = getExportJob(jobId);

        if (!"SUCCESS".equals(job.status())) {
            if ("FAILED".equals(job.status())) {
                throw new ApiException(ErrorCode.ENG_LIST_004, "导出任务执行失败，无法下载: " + job.errorMessage());
            }
            throw new ApiException(ErrorCode.ENG_LIST_003, "导出任务尚未完成，无法提供物理下载，当前状态: " + job.status());
        }

        File file = new File(job.filePath());
        if (!file.exists()) {
            throw new ApiException(ErrorCode.ENG_LIST_004, "导出的物理 CSV 文件在服务器上不存在或已被清理");
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            
            // 记录成功下载的物理审计事件
            auditPublisher.publish(AuditEvent.of(
                AuditAction.EXPORT,
                "large_list_export",
                jobId,
                "用户成功下载异步导出文件: " + job.fileName()
            ));

            return fis;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.ENG_LIST_004, "物理文件读取失败: " + e.getMessage());
        }
    }

    private void updateJobStatus(LargeListExportJob src, String newStatus, String fileName, String filePath, Long fileSize, Long costMs) {
        LargeListExportJob refreshed = jobRepository.findByJobId(src.jobId()).orElse(src);
        LargeListExportJob updated = new LargeListExportJob(
            refreshed.id(),
            refreshed.jobId(),
            refreshed.tenantId(),
            refreshed.resourceType(),
            refreshed.filterCriteria(),
            newStatus,
            fileName == null ? refreshed.fileName() : fileName,
            filePath == null ? refreshed.filePath() : filePath,
            fileSize == null ? refreshed.fileSize() : fileSize,
            refreshed.errorMessage(),
            costMs == null ? refreshed.timeCostMs() : costMs,
            refreshed.traceId(),
            refreshed.createdAt(),
            refreshed.createdBy(),
            Instant.now(),
            refreshed.updatedBy()
        );
        jobRepository.save(updated);
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private String currentActor() {
        return RequestContext.currentUserId()
            .filter(s -> !s.isBlank())
            .orElse("system");
    }

    private String escapeCsv(String val) {
        if (val == null) {
            return "";
        }
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
