package com.medkernel.compliance.evidence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medkernel.compliance.evidence.domain.EvidenceSnapshot;
import com.medkernel.compliance.evidence.dto.EvidenceCreateDto;
import com.medkernel.compliance.evidence.dto.EvidenceResponse;
import com.medkernel.compliance.evidence.dto.EvidenceVerifyResult;
import com.medkernel.compliance.evidence.repository.EvidenceSnapshotRepository;
import com.medkernel.compliance.evidence.service.EvidenceService;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.IsolatedAuditPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 证据链服务层纯单元测试（无 Spring 上下文，Mockito 驱动）。
 *
 * <p>覆盖核心业务场景：
 * <ul>
 *   <li>创建证据快照（含自动 SHA-256 指纹计算）</li>
 *   <li>重复创建冲突检测（ENG-EVID-003）</li>
 *   <li>按租户隔离检索</li>
 *   <li>防伪哈希碰撞验签（正常与篡改场景）</li>
 *   <li>跨租户越权访问拒绝</li>
 *   <li>异步导出审计留痕</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    EvidenceSnapshotRepository repository;

    @Mock
    IsolatedAuditPublisher isolatedAudit;

    @InjectMocks
    EvidenceService service;

    private static final String TENANT_ID = "tenant-hospital-01";
    private static final String EVIDENCE_ID = "evd-stroke-001";
    private static final String PAYLOAD = "{\"systolicBP\":180,\"diagnosis\":\"急性缺血性脑卒中\"}";

    private EvidenceSnapshot validSnapshot;

    @BeforeEach
    void setUp() {
        // 构建一个合法的证据快照实体（先算哈希再填充）
        EvidenceSnapshot temp = new EvidenceSnapshot(
            null, EVIDENCE_ID, TENANT_ID, "trace-001",
            "KNOWLEDGE_SOURCE", "CREATE", "guideline", "guideline-stroke-v3",
            "脑卒中临床指南知识来源存证", PAYLOAD,
            "", Instant.now(), "system", Instant.now(), "system"
        );
        String hash = temp.calculateHash();
        validSnapshot = new EvidenceSnapshot(
            1L, EVIDENCE_ID, TENANT_ID, "trace-001",
            "KNOWLEDGE_SOURCE", "CREATE", "guideline", "guideline-stroke-v3",
            "脑卒中临床指南知识来源存证", PAYLOAD,
            hash, temp.createdAt(), "system", temp.updatedAt(), "system"
        );
    }

    // ── 创建存证 ──────────────────────────────────────────────

    @Test
    @DisplayName("创建证据快照：自动计算 SHA-256 指纹并入库")
    void createSnapshot_success() {
        EvidenceCreateDto dto = new EvidenceCreateDto(
            EVIDENCE_ID, "trace-001", "KNOWLEDGE_SOURCE", "CREATE",
            "guideline", "guideline-stroke-v3",
            "脑卒中临床指南知识来源存证", PAYLOAD
        );

        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.empty());
        when(repository.save(any(EvidenceSnapshot.class))).thenAnswer(inv -> {
            EvidenceSnapshot arg = inv.getArgument(0);
            return new EvidenceSnapshot(
                1L, arg.evidenceId(), arg.tenantId(), arg.traceId(),
                arg.evidenceType(), arg.action(), arg.subjectType(), arg.subjectId(),
                arg.evidenceSummary(), arg.payloadSnapshot(), arg.payloadHash(),
                arg.createdAt(), arg.createdBy(), arg.updatedAt(), arg.updatedBy()
            );
        });

        EvidenceResponse resp = service.createSnapshot(TENANT_ID, dto);

        assertThat(resp).isNotNull();
        assertThat(resp.evidenceId()).isEqualTo(EVIDENCE_ID);
        assertThat(resp.payloadHash()).isNotBlank();
        assertThat(resp.isValid()).isTrue();

        // 确认保存时 payloadHash 非空
        ArgumentCaptor<EvidenceSnapshot> captor = ArgumentCaptor.forClass(EvidenceSnapshot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().payloadHash()).isNotBlank();
    }

    @Test
    @DisplayName("创建证据快照：重复 evidenceId 抛出 ENG-EVID-003 冲突异常")
    void createSnapshot_duplicateId_throwsConflict() {
        EvidenceCreateDto dto = new EvidenceCreateDto(
            EVIDENCE_ID, "trace-001", "KNOWLEDGE_SOURCE", "CREATE",
            "guideline", "guideline-stroke-v3",
            "脑卒中临床指南知识来源存证", PAYLOAD
        );
        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.of(validSnapshot));

        assertThatThrownBy(() -> service.createSnapshot(TENANT_ID, dto))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("已存在");
    }

    // ── 分页检索 ──────────────────────────────────────────────

    @Test
    @DisplayName("分页检索：返回当前租户的证据列表")
    void getEvidences_returnsList() {
        when(repository.findEvidencesPage(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(List.of(validSnapshot));

        List<EvidenceResponse> result = service.getEvidences(TENANT_ID, null, "KNOWLEDGE_SOURCE", 1, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).evidenceId()).isEqualTo(EVIDENCE_ID);
    }

    @Test
    @DisplayName("计数查询：返回过滤后的总数")
    void countEvidences_returnsTotal() {
        when(repository.countEvidences(TENANT_ID, "KNOWLEDGE_SOURCE", null)).thenReturn(42L);

        long total = service.countEvidences(TENANT_ID, null, "KNOWLEDGE_SOURCE");
        assertThat(total).isEqualTo(42L);
    }

    // ── 详情查询 ──────────────────────────────────────────────

    @Test
    @DisplayName("详情查询：按 evidenceId 返回完整响应")
    void getEvidenceById_success() {
        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.of(validSnapshot));

        EvidenceResponse resp = service.getEvidenceById(TENANT_ID, EVIDENCE_ID);
        assertThat(resp.evidenceId()).isEqualTo(EVIDENCE_ID);
        assertThat(resp.payloadSnapshot()).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("详情查询：不存在的 evidenceId 抛出 ENG-EVID-001")
    void getEvidenceById_notFound_throws() {
        when(repository.findByEvidenceId("evd-not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEvidenceById(TENANT_ID, "evd-not-exist"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("未找到");
    }

    @Test
    @DisplayName("详情查询：跨租户越权访问抛出 TENANT_FORBIDDEN")
    void getEvidenceById_wrongTenant_throws() {
        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.of(validSnapshot));

        assertThatThrownBy(() -> service.getEvidenceById("other-tenant", EVIDENCE_ID))
            .isInstanceOf(ApiException.class);
    }

    // ── 防伪验签 ──────────────────────────────────────────────

    @Test
    @DisplayName("验签成功：合法快照通过双向哈希碰撞验证，记录成功审计")
    void verifyEvidence_validHash_succeeds() {
        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.of(validSnapshot));

        EvidenceVerifyResult result = service.verifyEvidence(TENANT_ID, EVIDENCE_ID);

        assertThat(result.isValid()).isTrue();
        assertThat(result.calculatedHash()).isEqualTo(result.storedHash());

        // 验证发布了成功审计事件
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(isolatedAudit).publishInNewTx(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.OUTCOME_SUCCESS);
    }

    @Test
    @DisplayName("验签失败：篡改后的快照触发高危入侵审计（outcome=FAILED）")
    void verifyEvidence_tamperedData_triggersFailureAudit() {
        // 构建一个被篡改的快照：payload 被修改但 hash 保持原值
        EvidenceSnapshot tampered = new EvidenceSnapshot(
            1L, EVIDENCE_ID, TENANT_ID, "trace-001",
            "KNOWLEDGE_SOURCE", "CREATE", "guideline", "guideline-stroke-v3",
            "脑卒中临床指南知识来源存证",
            "{\"systolicBP\":120,\"diagnosis\":\"数据已被恶意篡改\"}", // 篡改 payload
            validSnapshot.payloadHash(), // 保持原哈希
            validSnapshot.createdAt(), "system", validSnapshot.updatedAt(), "system"
        );

        when(repository.findByEvidenceId(EVIDENCE_ID)).thenReturn(Optional.of(tampered));

        EvidenceVerifyResult result = service.verifyEvidence(TENANT_ID, EVIDENCE_ID);

        assertThat(result.isValid()).isFalse();
        assertThat(result.calculatedHash()).isNotEqualTo(result.storedHash());

        // 验证发布了失败审计事件
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(isolatedAudit).publishInNewTx(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditEvent.OUTCOME_FAILED);
        assertThat(captor.getValue().errorCode()).isEqualTo("ENG-EVID-002");
    }

    // ── 异步导出 ──────────────────────────────────────────────

    @Test
    @DisplayName("导出证据：对真实快照生成 64 位十六进制归档指纹并发布成功审计")
    void exportEvidences_returnsHashAndPublishesAudit() {
        when(repository.countEvidences(TENANT_ID, "KNOWLEDGE_SOURCE", null)).thenReturn(1L);
        when(repository.findEvidencesPage(eq(TENANT_ID), eq("KNOWLEDGE_SOURCE"), isNull(), anyInt(), anyInt()))
            .thenReturn(List.of(validSnapshot));

        String hash = service.exportEvidences(TENANT_ID, "KNOWLEDGE_SOURCE");

        assertThat(hash).matches("[0-9a-f]{64}");

        // 验证审计记录已发布
        verify(isolatedAudit).publishInNewTx(any(AuditEvent.class));
    }

    @Test
    @DisplayName("导出全量证据（无类型过滤）：审计标记 ALL 并生成真实归档指纹")
    void exportEvidences_noTypeFilter_usesAllMarker() {
        when(repository.countEvidences(TENANT_ID, null, null)).thenReturn(1L);
        when(repository.findEvidencesPage(eq(TENANT_ID), isNull(), isNull(), anyInt(), anyInt()))
            .thenReturn(List.of(validSnapshot));

        String hash = service.exportEvidences(TENANT_ID, null);

        assertThat(hash).matches("[0-9a-f]{64}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(isolatedAudit).publishInNewTx(captor.capture());
        assertThat(captor.getValue().resourceId()).contains("ALL");
    }

    @Test
    @DisplayName("导出证据：范围内无快照时拒绝导出（不生成伪造指纹、不留导出审计）")
    void exportEvidences_emptySet_throws() {
        when(repository.countEvidences(TENANT_ID, "RULE_DEFINITION", null)).thenReturn(0L);

        assertThatThrownBy(() -> service.exportEvidences(TENANT_ID, "RULE_DEFINITION"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("无可导出");

        verify(isolatedAudit, never()).publishInNewTx(any(AuditEvent.class));
    }

    @Test
    @DisplayName("导出证据：对真实快照集合计算确定性真 SHA-256（非随机假串）")
    void exportEvidences_computesDeterministicRealHash() {
        EvidenceSnapshot tempB = new EvidenceSnapshot(
            null, "evd-ami-002", TENANT_ID, "trace-002",
            "KNOWLEDGE_SOURCE", "CREATE", "guideline", "guideline-ami-v2",
            "心梗临床指南知识来源存证", "{\"door2balloon\":90}",
            "", Instant.now(), "system", Instant.now(), "system"
        );
        EvidenceSnapshot second = new EvidenceSnapshot(
            2L, tempB.evidenceId(), TENANT_ID, tempB.traceId(),
            tempB.evidenceType(), tempB.action(), tempB.subjectType(), tempB.subjectId(),
            tempB.evidenceSummary(), tempB.payloadSnapshot(), tempB.calculateHash(),
            tempB.createdAt(), "system", tempB.updatedAt(), "system"
        );

        when(repository.countEvidences(TENANT_ID, "KNOWLEDGE_SOURCE", null)).thenReturn(2L);
        when(repository.findEvidencesPage(eq(TENANT_ID), eq("KNOWLEDGE_SOURCE"), isNull(), anyInt(), anyInt()))
            .thenReturn(List.of(validSnapshot, second));

        String hash1 = service.exportEvidences(TENANT_ID, "KNOWLEDGE_SOURCE");
        String hash2 = service.exportEvidences(TENANT_ID, "KNOWLEDGE_SOURCE");

        assertThat(hash1).matches("[0-9a-f]{64}");   // 真 SHA-256：64 位十六进制
        assertThat(hash1).isEqualTo(hash2);            // 确定性：同数据同指纹（旧实现用随机 UUID 每次都变）
        assertThat(hash1).doesNotContain("proof");     // 杜绝旧 "sha256-archive-...-proof" 假格式
        verify(isolatedAudit, atLeastOnce()).publishInNewTx(any(AuditEvent.class));
    }
}
