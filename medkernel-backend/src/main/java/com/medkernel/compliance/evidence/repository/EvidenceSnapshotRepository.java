package com.medkernel.compliance.evidence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.medkernel.compliance.evidence.domain.EvidenceSnapshot;

/**
 * 医疗合规可信存证证据快照存储库层。
 */
@Repository
public interface EvidenceSnapshotRepository extends ListCrudRepository<EvidenceSnapshot, Long> {

    /**
     * 根据全局唯一证据 ID 查询证据快照。
     */
    Optional<EvidenceSnapshot> findByEvidenceId(String evidenceId);

    /**
     * 强隔离的分页游标过滤查询（兼容五方言标准）。
     */
    @Query("SELECT * FROM evidence_snapshot WHERE tenant_id = :tenantId "
         + "AND (:evidenceType IS NULL OR evidence_type = :evidenceType) "
         + "AND (:keyword IS NULL OR LOWER(evidence_summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
         + "ORDER BY id DESC LIMIT :limit OFFSET :offset")
    List<EvidenceSnapshot> findEvidencesPage(
        @Param("tenantId") String tenantId,
        @Param("evidenceType") String evidenceType,
        @Param("keyword") String keyword,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 按过滤条件查询的总数。
     */
    @Query("SELECT COUNT(*) FROM evidence_snapshot WHERE tenant_id = :tenantId "
         + "AND (:evidenceType IS NULL OR evidence_type = :evidenceType) "
         + "AND (:keyword IS NULL OR LOWER(evidence_summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countEvidences(
        @Param("tenantId") String tenantId,
        @Param("evidenceType") String evidenceType,
        @Param("keyword") String keyword
    );
}
