package com.medkernel.engine.knowledge;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 知识资产异步导出服务。
 *
 * <p>对外契约：
 * <ul>
 *   <li>{@code submit}：写入 PENDING 作业 + 事务提交后投递后台执行</li>
 *   <li>{@code get}：按 {@code jobCode} 查询当前状态</li>
 *   <li>{@code listRecent}：当前租户最近 100 个作业</li>
 *   <li>{@code cancel}：标记 PENDING/RUNNING 作业为 CANCELLED</li>
 * </ul>
 *
 * <p>实现策略：
 * <ul>
 *   <li>Job 持久化在 {@code knowledge_export_job}，对外可见 ID 是 {@code job_code}（UUID）</li>
 *   <li>{@code worker} 在线程池执行：PENDING → RUNNING → SUCCEEDED/FAILED</li>
 *   <li>当前实现是 stub：仅计数命中条目，不输出真实文件 URI（后续 GA-ENG-PKG-01 接入对象存储）</li>
 *   <li>结果 TTL 默认 7 天（{@code expires_at}），由 GA-ENG-PKG-01 清理任务 sweep</li>
 * </ul>
 */
@Service
public class KnowledgeExportService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeExportService.class);
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final KnowledgeExportJobRepository jobRepository;
    private final KnowledgeIdentityRepository identityRepository;
    private final Executor knowledgeExportExecutor;

    public KnowledgeExportService(KnowledgeExportJobRepository jobRepository,
                                  KnowledgeIdentityRepository identityRepository,
                                  @Qualifier("knowledgeExportExecutor") Executor knowledgeExportExecutor) {
        this.jobRepository = jobRepository;
        this.identityRepository = identityRepository;
        this.knowledgeExportExecutor = knowledgeExportExecutor;
    }

    /**
     * 提交异步导出作业。立即返回 PENDING；调用方需轮询 {@link #get(String)} 或订阅事件。
     */
    @Transactional
    public KnowledgeExportJob submit(ExportType type, String filterJson) {
        String tenantId = requireCurrentTenant();
        String actor = currentActor();
        Instant now = Instant.now();
        KnowledgeExportJob job = new KnowledgeExportJob(
            null, tenantId,
            UUID.randomUUID().toString(),
            actor, type, filterJson,
            ExportStatus.PENDING, 0,
            null, null, null,
            now, null, null, null
        );
        KnowledgeExportJob saved = jobRepository.save(job);
        // 等提交事务成功后再投递 worker；snapshot 让 worker 在线程池中恢复租户上下文。
        RequestContext.Snapshot snapshot = RequestContext.snapshot();
        dispatchAfterCommit(saved.jobCode(), snapshot);
        return saved;
    }

    public KnowledgeExportJob get(String jobCode) {
        String tenantId = requireCurrentTenant();
        return jobRepository.findByTenantIdAndJobCode(tenantId, jobCode)
            .orElseThrow(() -> ApiException.notFound("导出作业 jobCode=" + jobCode));
    }

    public List<KnowledgeExportJob> listRecent() {
        String tenantId = requireCurrentTenant();
        return jobRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public KnowledgeExportJob cancel(String jobCode) {
        KnowledgeExportJob job = get(jobCode);
        if (job.isTerminal()) {
            throw new ApiException(ErrorCode.CONFLICT, "作业已终态（" + job.status() + "），无法取消");
        }
        return updateStatus(job, ExportStatus.CANCELLED, null, null, "用户取消");
    }

    // ─── 内部 worker ────────────────────────────────────────

    private void dispatchAfterCommit(String jobCode, RequestContext.Snapshot snapshot) {
        Runnable worker = () -> RequestContext.runWith(snapshot, () -> {
            try {
                executeJob(jobCode);
            } catch (Exception e) {
                log.error("Knowledge export job {} failed", jobCode, e);
                markFailed(jobCode, e.getMessage());
            }
        });

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
    }

    void executeJob(String jobCode) {
        String tenantId = requireCurrentTenant();
        KnowledgeExportJob job = jobRepository.findByTenantIdAndJobCode(tenantId, jobCode)
            .orElseThrow(() -> new IllegalStateException("Job " + jobCode + " missing in worker"));
        if (job.status() != ExportStatus.PENDING) {
            log.warn("Skip job {} in status {}", jobCode, job.status());
            return;
        }
        Instant startedAt = Instant.now();
        jobRepository.save(rebuild(job, b -> {
            b.status = ExportStatus.RUNNING;
            b.startedAt = startedAt;
            b.progress = 10;
        }));

        long count;
        switch (job.exportType()) {
            case IDENTITIES -> count = identityRepository.countByTenantId(tenantId);
            case VERSIONS, LINEAGE, CITATIONS, FULL_TENANT -> {
                // 本 PR 用 identity 数量做粗略 placeholder；真实导出由 GA-ENG-PKG-01 接管
                count = identityRepository.countByTenantId(tenantId);
            }
            default -> count = 0L;
        }

        Instant completedAt = Instant.now();
        Instant expiresAt = completedAt.plus(DEFAULT_TTL);
        // result_uri 是占位（实施时由 GA-ENG-PKG-01 改为对象存储签名 URL）
        String resultUri = "memory://knowledge-export/" + jobCode + ".jsonl";
        long finalCount = count;
        KnowledgeExportJob refreshed = jobRepository.findByTenantIdAndJobCode(tenantId, jobCode).orElseThrow();
        jobRepository.save(rebuild(refreshed, b -> {
            b.status = ExportStatus.SUCCEEDED;
            b.startedAt = startedAt;
            b.completedAt = completedAt;
            b.progress = 100;
            b.itemCount = finalCount;
            b.resultUri = resultUri;
            b.expiresAt = expiresAt;
        }));
        log.info("Knowledge export job {} succeeded (type={}, count={})", jobCode, job.exportType(), count);
    }

    @Transactional
    void markFailed(String jobCode, String errorMessage) {
        String tenantId = requireCurrentTenant();
        jobRepository.findByTenantIdAndJobCode(tenantId, jobCode).ifPresent(job ->
            jobRepository.save(rebuild(job, b -> {
                b.status = ExportStatus.FAILED;
                if (b.startedAt == null) b.startedAt = Instant.now();
                b.completedAt = Instant.now();
                b.errorMessage = errorMessage;
            }))
        );
    }

    private KnowledgeExportJob updateStatus(KnowledgeExportJob job, ExportStatus newStatus,
                                            Instant startedAt, Instant completedAt, String errorMessage) {
        Instant effStarted = startedAt == null ? job.startedAt() : startedAt;
        Instant effCompleted = completedAt == null ? Instant.now() : completedAt;
        return jobRepository.save(rebuild(job, b -> {
            b.status = newStatus;
            b.startedAt = effStarted;
            b.completedAt = effCompleted;
            b.errorMessage = errorMessage;
        }));
    }

    /**
     * 用 mutator 在 record 上做"字段拷贝 + 局部修改"，避免每次都拼 14 个参数。
     */
    private static KnowledgeExportJob rebuild(KnowledgeExportJob src, java.util.function.Consumer<JobBuilder> mutator) {
        JobBuilder b = new JobBuilder(src);
        mutator.accept(b);
        return new KnowledgeExportJob(
            b.id, b.tenantId, b.jobCode, b.requestedBy,
            b.exportType, b.filterJson,
            b.status, b.progress,
            b.resultUri, b.itemCount, b.errorMessage,
            b.createdAt, b.startedAt, b.completedAt, b.expiresAt
        );
    }

    private static final class JobBuilder {
        Long id;
        String tenantId;
        String jobCode;
        String requestedBy;
        ExportType exportType;
        String filterJson;
        ExportStatus status;
        Integer progress;
        String resultUri;
        Long itemCount;
        String errorMessage;
        Instant createdAt;
        Instant startedAt;
        Instant completedAt;
        Instant expiresAt;

        JobBuilder(KnowledgeExportJob j) {
            this.id = j.id();
            this.tenantId = j.tenantId();
            this.jobCode = j.jobCode();
            this.requestedBy = j.requestedBy();
            this.exportType = j.exportType();
            this.filterJson = j.filterJson();
            this.status = j.status();
            this.progress = j.progress();
            this.resultUri = j.resultUri();
            this.itemCount = j.itemCount();
            this.errorMessage = j.errorMessage();
            this.createdAt = j.createdAt();
            this.startedAt = j.startedAt();
            this.completedAt = j.completedAt();
            this.expiresAt = j.expiresAt();
        }
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
}
