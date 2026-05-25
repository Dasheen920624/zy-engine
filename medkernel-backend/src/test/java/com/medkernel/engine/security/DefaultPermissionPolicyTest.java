package com.medkernel.engine.security;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPermissionPolicyTest {

    @Test
    void platformAdminHasAllPermissions() {
        assertThat(DefaultPermissionPolicy.permissionsOf(RoleCode.PLATFORM_ADMIN))
            .containsAll(EnumSet.allOf(PermissionCode.class));
    }

    @Test
    void groupAdminHasAllPermissions() {
        assertThat(DefaultPermissionPolicy.permissionsOf(RoleCode.GROUP_ADMIN))
            .containsAll(EnumSet.allOf(PermissionCode.class));
    }

    @Test
    void hospitalAdminLacksPlatformOps() {
        assertThat(DefaultPermissionPolicy.permissionsOf(RoleCode.HOSPITAL_ADMIN))
            .doesNotContain(PermissionCode.SYSTEM_MANAGE)
            .contains(PermissionCode.RULE_PUBLISH, PermissionCode.PACKAGE_ROLLBACK);
    }

    @Test
    void doctorCanReadAndAcceptButNotPublishRules() {
        var perms = DefaultPermissionPolicy.permissionsOf(RoleCode.DOCTOR);
        assertThat(perms)
            .contains(PermissionCode.RECOMMENDATION_READ, PermissionCode.RECOMMENDATION_ACCEPT,
                      PermissionCode.RULE_READ, PermissionCode.PATHWAY_READ)
            .doesNotContain(PermissionCode.RULE_WRITE, PermissionCode.RULE_PUBLISH,
                            PermissionCode.PATHWAY_PUBLISH, PermissionCode.SYSTEM_MANAGE);
    }

    @Test
    void auditComplianceIsReadOnly() {
        var perms = DefaultPermissionPolicy.permissionsOf(RoleCode.AUDIT_COMPLIANCE);
        for (PermissionCode p : perms) {
            assertThat(p.code())
                .as("合规审计角色仅可读 / 导出，不应有写权限：%s", p.code())
                .matches("(.+\\.read|.+\\.export)");
        }
        assertThat(perms).contains(PermissionCode.AUDIT_READ, PermissionCode.AUDIT_EXPORT);
    }

    @Test
    void medicalAffairsCanPublishKnowledgeAndPathways() {
        var perms = DefaultPermissionPolicy.permissionsOf(RoleCode.MEDICAL_AFFAIRS);
        assertThat(perms).contains(
            PermissionCode.KNOWLEDGE_REVIEW, PermissionCode.KNOWLEDGE_PUBLISH,
            PermissionCode.PATHWAY_PUBLISH, PermissionCode.RULE_PUBLISH);
        assertThat(perms).doesNotContain(PermissionCode.SYSTEM_MANAGE);
    }

    @Test
    void roleCodeRoundtripsThroughAuthority() {
        for (RoleCode role : RoleCode.values()) {
            assertThat(RoleCode.fromAuthority(role.authority())).contains(role);
            assertThat(RoleCode.fromCode(role.code())).contains(role);
        }
    }

    @Test
    void permissionCodeRoundtrip() {
        for (PermissionCode perm : PermissionCode.values()) {
            assertThat(PermissionCode.fromCode(perm.code())).contains(perm);
        }
    }
}
