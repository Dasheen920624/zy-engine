package com.medkernel.engine.knowledge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceVersionRepository extends ListCrudRepository<SourceVersion, Long> {

    Optional<SourceVersion> findByTenantIdAndId(String tenantId, Long id);

    List<SourceVersion> findByTenantIdAndSourceDocumentIdOrderByPublishedAtDescIdDesc(String tenantId, Long sourceDocumentId);
}
