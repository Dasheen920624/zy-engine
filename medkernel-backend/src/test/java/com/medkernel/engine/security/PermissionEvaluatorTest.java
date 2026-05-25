package com.medkernel.engine.security;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionEvaluatorTest {

    private final PermissionEvaluator evaluator = new PermissionEvaluator();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthenticationMeansNoPermissions() {
        assertThat(evaluator.has("rule.publish")).isFalse();
        assertThat(evaluator.has(PermissionCode.RULE_READ)).isFalse();
    }

    @Test
    void doctorCanReadRecommendationsButNotPublishRules() {
        authenticate(RoleCode.DOCTOR);
        assertThat(evaluator.has("recommendation.read")).isTrue();
        assertThat(evaluator.has("recommendation.accept")).isTrue();
        assertThat(evaluator.has("rule.publish")).isFalse();
    }

    @Test
    void medicalAffairsCanPublishRules() {
        authenticate(RoleCode.MEDICAL_AFFAIRS);
        assertThat(evaluator.has("rule.publish")).isTrue();
        assertThat(evaluator.has("knowledge.publish")).isTrue();
        assertThat(evaluator.has("system.manage")).isFalse();
    }

    @Test
    void platformAdminHasEverything() {
        authenticate(RoleCode.PLATFORM_ADMIN);
        for (PermissionCode perm : PermissionCode.values()) {
            assertThat(evaluator.has(perm.code()))
                .as("PLATFORM_ADMIN 应拥有 %s", perm.code())
                .isTrue();
        }
    }

    @Test
    void multipleRolesUnion() {
        authenticate(RoleCode.DOCTOR, RoleCode.QA_MANAGER);
        assertThat(evaluator.has("recommendation.accept")).isTrue(); // DOCTOR
        assertThat(evaluator.has("evaluation.publish")).isTrue();    // QA_MANAGER
        assertThat(evaluator.has("system.manage")).isFalse();        // neither
    }

    @Test
    void hasAnyShortCircuits() {
        authenticate(RoleCode.DOCTOR);
        assertThat(evaluator.hasAny("rule.publish", "recommendation.read")).isTrue();
        assertThat(evaluator.hasAny("rule.publish", "system.manage")).isFalse();
        assertThat(evaluator.hasAny()).isFalse();
    }

    @Test
    void hasAllRequiresEveryCode() {
        authenticate(RoleCode.MEDICAL_AFFAIRS);
        assertThat(evaluator.hasAll("rule.read", "rule.publish")).isTrue();
        assertThat(evaluator.hasAll("rule.read", "system.manage")).isFalse();
        assertThat(evaluator.hasAll()).isTrue();
    }

    @Test
    void unknownPermissionCodeReturnsFalse() {
        authenticate(RoleCode.PLATFORM_ADMIN);
        assertThat(evaluator.has("does.not.exist")).isFalse();
    }

    private void authenticate(RoleCode... roles) {
        var authorities = java.util.Arrays.stream(roles)
            .map(r -> new SimpleGrantedAuthority(r.authority()))
            .toList();
        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("user", "creds", authorities);
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
