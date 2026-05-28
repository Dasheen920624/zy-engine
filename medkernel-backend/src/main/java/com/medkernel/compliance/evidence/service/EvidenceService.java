package com.medkernel.compliance.evidence.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.compliance.evidence.domain.EvidenceSnapshot;
import com.medkernel.compliance.evidence.dto.EvidenceCreateDto;
import com.medkernel.compliance.evidence.dto.EvidenceResponse;
import com.medkernel.compliance.evidence.dto.EvidenceVerifyResult;
import com.medkernel.compliance.evidence.repository.EvidenceSnapshotRepository;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.datascope.DataScope;

/**
 * 医疗合规可信存证证据中枢服务层（强租户隔离与防篡改）。
 */
@Service
@DataScope(requireTenant = true)
public class EvidenceService {

    private final EvidenceSnapshotRepository repository;
    private final IsolatedAuditPublisher isolatedAudit;

    public EvidenceService(EvidenceSnapshotRepository repository, IsolatedAuditPublisher isolatedAudit) {
        this.repository = repository;
        this.isolatedAudit = isolatedAudit;
    }

    /**
     * 创建并在子事务中安全存证一条证据快照。
     */
    @Transactional
    public EvidenceResponse createSnapshot(String tenantId, EvidenceCreateDto dto) {
        // 判断是否存在相同证据 ID
        Optional<EvidenceSnapshot> existing = repository.findByEvidenceId(dto.evidenceId());
        if (existing.isPresent()) {
            throw new ApiException(ErrorCode.ENG_EVID_003, "证据快照已存在: " + dto.evidenceId());
        }

        // 构建无签名指纹的临时实体以辅助算哈希
        EvidenceSnapshot temp = new EvidenceSnapshot(
            null,
            dto.evidenceId(),
            tenantId,
            dto.traceId(),
            dto.evidenceType(),
            dto.action(),
            dto.subjectType(),
            dto.subjectId(),
            dto.evidenceSummary(),
            dto.payloadSnapshot(),
            "", // 待填入指纹
            Instant.now(),
            "system",
            Instant.now(),
            "system"
        );

        // 自动提取要素计算防伪 SHA-256 指纹
        String calculatedHash = temp.calculateHash();

        // 正式创建完整存证的实体
        EvidenceSnapshot entity = new EvidenceSnapshot(
            null,
            temp.evidenceId(),
            temp.tenantId(),
            temp.traceId(),
            temp.evidenceType(),
            temp.action(),
            temp.subjectType(),
            temp.subjectId(),
            temp.evidenceSummary(),
            temp.payloadSnapshot(),
            calculatedHash,
            temp.createdAt(),
            temp.createdBy(),
            temp.updatedAt(),
            temp.updatedBy()
        );

        EvidenceSnapshot saved = repository.save(entity);
        return EvidenceResponse.fromEntity(saved);
    }

    /**
     * 强租户物理隔离的分页检索。
     */
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidences(String tenantId, String keyword, String evidenceType, int page, int size) {
        int limit = size <= 0 ? 20 : size;
        int offset = (page <= 0 ? 0 : page - 1) * limit;

        List<EvidenceSnapshot> list = repository.findEvidencesPage(tenantId, evidenceType, keyword, limit, offset);
        return list.stream()
            .map(EvidenceResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 过滤查询的总数。
     */
    @Transactional(readOnly = true)
    public long countEvidences(String tenantId, String keyword, String evidenceType) {
        return repository.countEvidences(tenantId, evidenceType, keyword);
    }

    /**
     * 根据全局唯一证据 ID 检索证据详情。
     */
    @Transactional(readOnly = true)
    public EvidenceResponse getEvidenceById(String tenantId, String evidenceId) {
        EvidenceSnapshot entity = repository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVID_001, "未找到指定的证据快照: " + evidenceId));

        if (!tenantId.equals(entity.tenantId())) {
            throw new ApiException(ErrorCode.TENANT_FORBIDDEN);
        }

        return EvidenceResponse.fromEntity(entity);
    }

    /**
     * 双向哈希比对验签服务（若篡改则发布隔离级别的高危入侵审计日志）。
     */
    @Transactional
    public EvidenceVerifyResult verifyEvidence(String tenantId, String evidenceId) {
        EvidenceSnapshot entity = repository.findByEvidenceId(evidenceId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVID_001, "未找到指定的证据快照: " + evidenceId));

        if (!tenantId.equals(entity.tenantId())) {
            throw new ApiException(ErrorCode.TENANT_FORBIDDEN);
        }

        String calculated = entity.calculateHash();
        boolean isValid = entity.isValid();

        // 强审计留痕：如果被篡改，自动以隔离的子事务向 audit_event 物理存入 outcome=FAILED 高危警告
        if (!isValid) {
            isolatedAudit.publishInNewTx(AuditEvent.failure(
                AuditAction.REVIEW,
                "evidence_snapshot",
                evidenceId,
                "ENG-EVID-002",
                "防伪数字指纹哈希校验失败！快照原始数据遭恶意修改！"
            ));
        } else {
            // 校验成功，正常留痕
            isolatedAudit.publishInNewTx(AuditEvent.of(
                AuditAction.REVIEW,
                "evidence_snapshot",
                evidenceId,
                "防伪数字指纹哈希校验成功，数据完好无损"
            ));
        }

        return new EvidenceVerifyResult(evidenceId, isValid, calculated, entity.payloadHash());
    }

    /**
     * 模拟生成打包合规大导出（打包防伪哈希输出并在后台记录安全审计）。
     */
    @Transactional
    public String exportEvidences(String tenantId, String evidenceType) {
        // 生成防伪包的特征哈希
        String archiveZipHash = "sha256-archive-" + UUID.randomUUID().toString().replace("-", "") + "-proof";

        // 记录大导出行为审计日志
        isolatedAudit.publishInNewTx(AuditEvent.of(
            AuditAction.EXPORT,
            "evidence_snapshot",
            "bulk-export-" + (evidenceType == null ? "ALL" : evidenceType),
            "审计合规数据包大规模异步打包导出成功，防伪指纹为：" + archiveZipHash
        ));

        return archiveZipHash;
    }
}
