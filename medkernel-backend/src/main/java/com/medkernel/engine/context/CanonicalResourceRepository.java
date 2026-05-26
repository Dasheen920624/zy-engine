package com.medkernel.engine.context;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CanonicalResourceRepository extends ListCrudRepository<CanonicalResource, Long> {

    List<CanonicalResource> findBySnapshotIdOrderBySeqNoAsc(String snapshotId);
}
