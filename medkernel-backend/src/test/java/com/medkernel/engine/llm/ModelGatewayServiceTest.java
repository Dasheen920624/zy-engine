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

        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "提取结构化临床文本信息", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("DEGRADED", resp.status());
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.fallbackUsed());
        assertTrue(resp.outputContent().contains("临床概念A"));
        assertFalse(resp.outputContent().contains("高血压"));

        verify(taskRepo).save(any(ModelCapabilityTask.class));
        // 成功路径走 AuditEventPublisher（同事务）；isolated 仅用于失败留痕（LLM-M-04）
        verify(auditPublisher).publish(eq(AuditAction.EXECUTE), eq("model_capability_task"), anyString(), anyString());
        verify(isolatedAudit, never()).publishInNewTx(any(AuditEvent.class));
    }

    @Test
    void submitTask_localModelRoute_honestlyDegradesToB0_noFabrication() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "LOCAL_MODEL", "DEFAULT", null,
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "普通脑卒中病历", null, null, 60);
        ModelTaskResponse resp = service.submitTask(req);

        assertNotNull(resp);
        // 未接入真实 provider → 诚实降级 B0，绝不伪造 B1 元数据（宪法 #9/#13）
        assertEquals("DEGRADED", resp.status());
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.fallbackUsed());
        assertEquals("B0-Deterministic-Baseline", resp.modelVersion());
        assertNull(resp.confidence());
        assertEquals("[]", resp.sourceCitations());
        assertTrue(resp.fallbackReason().contains("未接入本地微调模型(B1)"));
        // 反例：杜绝旧实现伪造的本地模型版本与字段
        assertNotEquals("MedKernel-Local-Cognitive-v1", resp.modelVersion());
        assertFalse(resp.outputContent().contains("local_enhanced"));
        verify(taskRepo).save(any(ModelCapabilityTask.class));
    }

    @Test
    void submitTask_externalModelRoute_honestB0_desensitizesInput_neverFabricates() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", null,
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        // EXTERNAL_MODEL 配置但无 provider：诚实降级 B0，并验证手机号脱敏写入摘要
        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "张医生的手机是13988888888", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("DEGRADED", resp.status());
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.fallbackUsed());
        assertNull(resp.confidence());
        assertEquals("[]", resp.sourceCitations());
        assertTrue(resp.fallbackReason().contains("未接入外部大模型/Dify(B2)"));

        // 反例（医疗安全红线）：绝不出现伪造的外部模型版本 / 编造引文 / 编造患者
        assertNotEquals("MedKernel-Cognitive-LLM-v2", resp.modelVersion());
        assertFalse(resp.sourceCitations().contains("溶栓指南"));
        assertFalse(resp.outputContent().contains("李建国"));

        // 验证摘要脱敏过滤（手机号掩码）
        verify(taskRepo).save(argThat(task ->
            task.inputSummary().contains("139****8888") && !task.inputSummary().contains("13988888888")));
    }

    @Test
    void submitTask_withSatisfiedSchema_passesRealJsonValidation() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT", "required: [entity]",
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        // B0 基线输出（含 entity）满足 required: [entity]，真实 JSON 校验通过
        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "提取要素", null, null, 60);

        ModelTaskResponse resp = service.submitTask(req);
        assertNotNull(resp);
        assertEquals("B0", resp.modelMode());
        assertTrue(resp.outputContent().contains("entity"));
        verify(taskRepo).save(any(ModelCapabilityTask.class));
    }

    @Test
    void submitTask_withUnsatisfiableSchema_throwsRealSchemaErrorAndAuditsFailure() {
        ModelCapabilityPolicy modelPolicy = new ModelCapabilityPolicy(
            1L, "tenant-1", "knowledge.extract", "EXTERNAL_MODEL", "DEFAULT",
            "{\"required\":[\"nonexistent_field\"]}",
            Instant.now(), "system", Instant.now(), "system"
        );
        when(policyRepo.findByTenantIdAndCapabilityCode("tenant-1", "knowledge.extract"))
            .thenReturn(Optional.of(modelPolicy));

        // required 字段在 B0 输出中不存在 → 真实 JSON Schema 校验失败抛 ENG-LLM-002
        ModelTaskRequest req = new ModelTaskRequest("knowledge.extract", "提取要素", null, null, 60);

        ApiException ex = assertThrows(ApiException.class, () -> service.submitTask(req));
        assertEquals("ENG-LLM-002", ex.errorCode().code());
        // 失败路径也发 FAILED 审计；且不得落库成功任务
        verify(isolatedAudit).publishInNewTx(any(AuditEvent.class));
        verify(taskRepo, never()).save(any(ModelCapabilityTask.class));
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
