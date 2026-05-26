package com.medkernel.engine.security;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionOverrideRepository extends ListCrudRepository<RolePermissionOverride, Long> {

    @Query("""
        SELECT * FROM role_permission
        WHERE tenant_id = :tenantId
          AND role_code IN (:roleCodes)
        ORDER BY role_code, permission_code
        """)
    List<RolePermissionOverride> findByTenantIdAndRoleCodes(String tenantId, Collection<String> roleCodes);
}
