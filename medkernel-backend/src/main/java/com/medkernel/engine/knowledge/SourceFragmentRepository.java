package com.medkernel.engine.knowledge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceFragmentRepository extends ListCrudRepository<SourceFragment, Long> {

    Optional<SourceFragment> findByTenantIdAndId(String tenantId, Long id);

    List<SourceFragment> findByTenantIdAndSourceVersionIdOrderByAnchorPathAsc(String tenantId, Long sourceVersionId);
}
