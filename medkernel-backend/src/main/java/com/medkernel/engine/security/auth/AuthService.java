package com.medkernel.engine.security.auth;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.medkernel.engine.security.PlatformCredential;
import com.medkernel.engine.security.PlatformCredentialRepository;
import com.medkernel.engine.security.UserRoleAssignment;
import com.medkernel.engine.security.UserRoleAssignmentRepository;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.IsolatedAuditPublisher;

/** 平台账号登录：BCrypt 校验 → 取角色 → 签发 JWT；成功/失败均隔离审计。 */
@Service
public class AuthService {

    private final PlatformCredentialRepository credentials;
    private final UserRoleAssignmentRepository roleAssignments;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final IsolatedAuditPublisher isolatedAudit;

    public AuthService(PlatformCredentialRepository credentials,
                       UserRoleAssignmentRepository roleAssignments,
                       PasswordEncoder passwordEncoder,
                       JwtIssuer jwtIssuer,
                       IsolatedAuditPublisher isolatedAudit) {
        this.credentials = credentials;
        this.roleAssignments = roleAssignments;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
        this.isolatedAudit = isolatedAudit;
    }

    public AuthResult login(String tenantId, String username, String rawPassword) {
        PlatformCredential cred = credentials.findByTenantIdAndUsername(tenantId, username).orElse(null);
        if (cred == null || !passwordEncoder.matches(rawPassword, cred.passwordHash())) {
            isolatedAudit.publishInNewTx(AuditEvent.failure(
                AuditAction.LOGIN, "platform_credential", username,
                ErrorCode.ENG_AUTH_001.code(), "登录失败：用户名或密码不正确 username=" + username));
            throw new ApiException(ErrorCode.ENG_AUTH_001);
        }
        if (!cred.active()) {
            isolatedAudit.publishInNewTx(AuditEvent.failure(
                AuditAction.LOGIN, "platform_credential", cred.userId(),
                ErrorCode.ENG_AUTH_002.code(), "登录失败：账号禁用/锁定 status=" + cred.status()));
            throw new ApiException(ErrorCode.ENG_AUTH_002);
        }
        List<String> roles = roleAssignments
            .findActiveByTenantIdAndUserId(tenantId, cred.userId())
            .stream().map(UserRoleAssignment::roleCode).distinct().toList();
        String jwt = jwtIssuer.issue(cred.userId(), tenantId, roles);
        isolatedAudit.publishInNewTx(AuditEvent.of(
            AuditAction.LOGIN, "platform_credential", cred.userId(),
            "登录成功 username=" + username + " roles=" + roles));
        return new AuthResult(jwt,
            new LoginResponse(cred.userId(), tenantId, roles, "Y".equalsIgnoreCase(cred.mustChangePwd())));
    }

    public void logout(String userId) {
        isolatedAudit.publishInNewTx(AuditEvent.of(
            AuditAction.LOGOUT, "platform_credential", userId == null ? "anonymous" : userId, "登出"));
    }

    public record AuthResult(String jwt, LoginResponse response) {}
}
