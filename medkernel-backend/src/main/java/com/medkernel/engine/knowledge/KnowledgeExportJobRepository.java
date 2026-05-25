package com.medkernel.engine.knowledge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeExportJobRepository extends ListCrudRepository<KnowledgeExportJob, Long> {

    Optional<KnowledgeExportJob> findByTenantIdAndJobCode(String tenantId, String jobCode);

    List<KnowledgeExportJob> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, ExportStatus status);

    List<KnowledgeExportJob> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
