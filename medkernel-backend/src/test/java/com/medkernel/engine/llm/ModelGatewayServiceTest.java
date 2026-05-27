package com.medkernel.engine.llm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class ModelGatewayServiceTest {

    private ModelCapabilityTaskRepository taskRepo;
    private ModelCapabilityPolicyRepository policyRepo;
    private AuditEventPublisher auditPublisher;
    private IsolatedAuditPublisher isolatedAudit;
    private ModelGatewayService service;

    @BeforeEach
    void setUp() {
        taskRepo = mock(ModelCapabilityTaskRepository.class);
        policyRepo = mock(ModelCapabilityPolicyRepository.class);
        auditPublisher = mock(AuditEventPublisher.class);
        isolatedAudit = mock(IsolatedAuditPublisher.class);
        service = new ModelGatewayService(taskRepo, policyRepo, auditPublisher, isolatedAudit);

        // 设置当前线程的租户组织上下文
        RequestContext.restore(new RequestContext.Snapshot("trace-123", OrgScope.tenant("tenant-1"), "DOCTOR-001"));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void getStatus_NoConfig_ReturnsDefaultBaseplay() {
        when(policyRepo.findByTenantIdAndCapabilityCode(eq("tenant-1"), any())).thenReturn(Optional.empty());

        List<ModelCapabilityStatusResponse> list = service.getStatus();
        assertNotNull(list);
        assertEquals(8, list.size());
        
        ModelCapabilityStatusResponse discovery = list.stream()
            .filter(r -> "knowledge.discovery".equals(r.capabilityCode()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(discovery);
        assertEquals("BASEPLAY", discovery.routeStrategy());
        assertTrue(discovery.fallbackAvailable());
    }

    @Test
    void submitTask_DisabledStrategy_ThrowsException() {
        ModelCapabilityPolicy disabledPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "DISABLED", "DEFAULT", null,
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(disabledPolicy));

        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "测试输入", null, null, 60);

        ApiException ex = assertThrows(ApiException.class, () -> service.submitTask(req));
        assertEquals("ENG-LLM-001", ex.errorCode().code());
        verify(isolatedAudit).publishInNewTx(any(AuditEvent.class));
    }

    @Test
    void submitTask_BaseplayStrategy_ReturnsB0Fallback() {
        ModelCapabilityPolicy baseplayPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "BASEPLAY", "DEFAULT", null,
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(baseplayPolicy));

        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "提取高血压病历信息", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("DEGRADED", resp.status());
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.fallbackUsed());
        assertTrue(resp.outputContent().contains("高血压"));

        verify(taskRepo).save(any(ModelCapabilityTask.class));
        verify(isolatedAudit).publishInNewTx(any(AuditEvent.class));
    }

    @Test
    void submitTask_TimeoutForceFallback_ReturnsB0Fallback() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", null,
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        // 手机号与敏感词脱敏
        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "张医生的手机是13988888888, 强制超时FORCE_TIMEOUT", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("DEGRADED", resp.status());
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.fallbackUsed());
        // 验证摘要脱敏过滤
        verify(taskRepo).save(argThat(task -> task.inputSummary().contains("138****8888") && !task.inputSummary().contains("13988888888")));
    }

    @Test
    void submitTask_SchemaConstraintFailFallback_ReturnsB0Fallback() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", "required: [entity]",
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        // 强行输出不包含 entity 字段的文本以破坏 Schema 校验，触发 B0 Fallback 降级
        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "FORCE_FAIL_SCHEMA", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("DEGRADED", resp.status());
        assertTrue(resp.fallbackUsed());
        assertTrue(resp.outputContent().contains("entity")); // Fallback 包含了 entity
    }

    @Test
    void validatePolicy_InvalidSchema_ReturnsInvalid() {
        ModelPolicyValidateRequest req = new ModelPolicyValidateRequest(
            "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", "invalid_non_json_schema"
        );

        ModelPolicyValidateResponse resp = service.validatePolicy(req);
        assertNotNull(resp);
        assertFalse(resp.valid());
    }

    @Test
    void validatePolicy_Valid_ReturnsOk() {
        ModelPolicyValidateRequest req = new ModelPolicyValidateRequest(
            "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", "{\"required\":[\"entity\"]}"
        );

        ModelPolicyValidateResponse resp = service.validatePolicy(req);
        assertNotNull(resp);
        assertTrue(resp.valid());
    }
}
