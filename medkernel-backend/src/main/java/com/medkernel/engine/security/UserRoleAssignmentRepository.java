package com.medkernel.engine.security;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleAssignmentRepository extends ListCrudRepository<UserRoleAssignment, Long> {

    @Query("""
        SELECT * FROM user_role_assignment
        WHERE tenant_id = :tenantId
          AND user_id = :userId
          AND active_flag = 'Y'
        ORDER BY role_code, scope_level, scope_code
        """)
    List<UserRoleAssignment> findActiveByTenantIdAndUserId(String tenantId, String userId);
}
