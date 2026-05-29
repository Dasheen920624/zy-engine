package com.medkernel.engine.security;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformCredentialRepository extends ListCrudRepository<PlatformCredential, Long> {

    Optional<PlatformCredential> findByTenantIdAndUsername(String tenantId, String username);

    Optional<PlatformCredential> findByCredentialId(String credentialId);
}
