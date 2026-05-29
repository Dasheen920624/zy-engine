package com.medkernel.engine.security;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 平台自建身份凭证仓库：按租户 + 用户名查登录凭证，按业务 ID 查单条。
 */
@Repository
public interface PlatformCredentialRepository extends ListCrudRepository<PlatformCredential, Long> {

    Optional<PlatformCredential> findByTenantIdAndUsername(String tenantId, String username);

    Optional<PlatformCredential> findByCredentialId(String credentialId);
}
