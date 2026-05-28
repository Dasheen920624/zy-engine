package com.medkernel.compliance.evidence.domain;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 医疗合规可信存证证据快照实体 (EvidenceSnapshot) - Spring Data JDBC Record 风格。
 */
@Table("evidence_snapshot")
public record EvidenceSnapshot(
    @Id Long id,
    @Column("evidence_id") String evidenceId,
    @Column("tenant_id") String tenantId,
    @Column("trace_id") String traceId,
    @Column("evidence_type") String evidenceType,
    @Column("action") String action,
    @Column("subject_type") String subjectType,
    @Column("subject_id") String subjectId,
    @Column("evidence_summary") String evidenceSummary,
    @Column("payload_snapshot") String payloadSnapshot,
    @Column("payload_hash") String payloadHash,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {
    /**
     * 根据当前内容计算 SHA-256 签名数字指纹以防篡改。
     */
    public String calculateHash() {
        try {
            String base = evidenceId + "|" + tenantId + "|" + createdBy + "|" + payloadSnapshot;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hash calculation failed", e);
        }
    }

    /**
     * 校验当前指纹是否未遭篡改。
     */
    public boolean isValid() {
        return calculateHash().equals(payloadHash);
    }
}
