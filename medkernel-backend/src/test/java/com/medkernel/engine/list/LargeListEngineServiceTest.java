package com.medkernel.engine.list;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.audit.persistence.AuditEventQuery;
import com.medkernel.shared.audit.persistence.AuditEventRecord;
import com.medkernel.shared.audit.persistence.AuditEventRepository;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class LargeListEngineServiceTest {

    private LargeListExportJobRepository jobRepo;
    private AuditEventRepository auditRepo;
    private AuditEventPublisher auditPublisher;
    private IsolatedAuditPublisher isolatedAudit;
    private JdbcTemplate jdbc;
    private Executor syncExecutor; // 单元测试中同步执行以方便验证

    private LargeListEngineService service;

    @BeforeEach
    void setUp() {
        jobRepo = mock(LargeListExportJobRepository.class);
        auditRepo = mock(AuditEventRepository.class);
        auditPublisher = mock(AuditEventPublisher.class);
        isolatedAudit = mock(IsolatedAuditPublisher.class);
        jdbc = mock(JdbcTemplate.class);
        // 让异步 Executor 同步跑以通过 worker 内部测试
        syncExecutor = Runnable::run;

        service = new LargeListEngineService(
            jobRepo,
            auditRepo,
            auditPublisher,
            isolatedAudit,
            jdbc,
            syncExecutor
        );

        RequestContext.restore(new RequestContext.Snapshot("trace-123", OrgScope.tenant("tenant-1"), "IT-OPS-001"));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void queryList_InvalidResourceType_ThrowsException() {
        ListQueryRequest req = new ListQueryRequest("INVALID_TYPE", 10, null, null, null, null, Map.of());
        ApiException ex = assertThrows(ApiException.class, () -> service.queryList(req));
        assertEquals("ENG-LIST-001", ex.errorCode().code());
    }

    @Test
    void queryList_ValidQuery_PerformsCursorMappingAndReturnsEstimate() {
        // 模拟 queryForObject 运行估算 count，返回 15000 条数据
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(15000L);

        // 模拟返回 3 条数据 (比 pageSize 大 1 以触发 hasMore)
        AuditEventRecord rec1 = mockAuditEvent(100L);
        AuditEventRecord rec2 = mockAuditEvent(99L);
        AuditEventRecord rec3 = mockAuditEvent(98L);
        when(auditRepo.findPage(eq("tenant-1"), any(AuditEventQuery.class)))
            .thenReturn(List.of(rec1, rec2, rec3));

        ListQueryRequest req = new ListQueryRequest("AUDIT_EVENT", 2, null, null, null, null, Map.of());
        ListQueryResponse<AuditEventRecord> resp = service.queryList(req);

        assertNotNull(resp);
        assertEquals(2, resp.records().size());
        assertTrue(resp.hasMore());
        assertEquals(10000L, resp.totalEstimate()); // 15000L 被截断为 10000L

        // 验证游标编码为第 2 条数据 (99L) 的 Base64
        String expectedCursor = Base64.getEncoder().encodeToString("99".getBytes());
        assertEquals(expectedCursor, resp.nextCursor());
    }

    @Test
    void queryList_InvalidCursorFormat_ThrowsBadRequest() {
        ListQueryRequest req = new ListQueryRequest("AUDIT_EVENT", 10, null, "invalid-base64", null, null, Map.of());
        ApiException ex = assertThrows(ApiException.class, () -> service.queryList(req));
        assertEquals("ENG-API-001", ex.errorCode().code());
    }

    @Test
    void submitExportTask_PendingJobPersisted() {
        ExportSubmitRequest req = new ExportSubmitRequest("AUDIT_EVENT", Map.of());

        LargeListExportJob pendingJob = new LargeListExportJob(
            1L, "job-1", "tenant-1", "AUDIT_EVENT", "{}",
            "PENDING", null, null, 0L, null, 0L, "trace-123",
            Instant.now(), "IT-OPS-001", Instant.now(), "IT-OPS-001"
        );

        when(jobRepo.save(any(LargeListExportJob.class))).thenReturn(pendingJob);
        // 让 executeExport 在保存后不触发异常（Mock 正常执行）
        when(jobRepo.findByJobId("job-1")).thenReturn(Optional.of(pendingJob));

        ExportSubmitResponse resp = service.submitExportTask(req);
        assertNotNull(resp);
        assertEquals("job-1", resp.jobId());
        assertEquals("PENDING", resp.status());

        verify(jobRepo, atLeastOnce()).save(any(LargeListExportJob.class));
    }

    @Test
    void downloadFile_JobNotFinished_ThrowsConflictException() {
        LargeListExportJob runningJob = new LargeListExportJob(
            1L, "job-1", "tenant-1", "AUDIT_EVENT", "{}",
            "RUNNING", null, null, 0L, null, 0L, "trace-123",
            Instant.now(), "IT-OPS-001", Instant.now(), "IT-OPS-001"
        );
        when(jobRepo.findByJobId("job-1")).thenReturn(Optional.of(runningJob));

        ApiException ex = assertThrows(ApiException.class, () -> service.downloadFile("job-1"));
        assertEquals("ENG-LIST-003", ex.errorCode().code());
    }

    @Test
    void downloadFile_JobFailed_ThrowsInternalException() {
        LargeListExportJob failedJob = new LargeListExportJob(
            1L, "job-1", "tenant-1", "AUDIT_EVENT", "{}",
            "FAILED", null, null, 0L, "导出中磁盘占满", 0L, "trace-123",
            Instant.now(), "IT-OPS-001", Instant.now(), "IT-OPS-001"
        );
        when(jobRepo.findByJobId("job-1")).thenReturn(Optional.of(failedJob));

        ApiException ex = assertThrows(ApiException.class, () -> service.downloadFile("job-1"));
        assertEquals("ENG-LIST-004", ex.errorCode().code());
        assertTrue(ex.getMessage().contains("导出任务执行失败"));
    }

    private AuditEventRecord mockAuditEvent(Long id) {
        return new AuditEventRecord(
            id, "evt-" + id, "trace-123", Instant.now(), "IT-OPS-001",
            "LOGIN", "USER", "IT-OPS-001", "用户登录", null,
            "tenant-1", null, null, null, null, null, "SIGNED",
            "SUCCESS", null, Instant.now()
        );
    }
}
