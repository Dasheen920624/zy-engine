package com.medkernel.engine.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.datascope.DataScope;

/**
 * 当前用户权限画像接口。
 *
 * <p>前端只用该接口决定菜单可见、按钮可点、专家模式是否展示；真正授权仍在后端
 * {@code @PreAuthorize("@perm.has(...)")} 和 {@link DataScope} 双门禁内完成。
 */
@RestController
@RequestMapping("/api/v1/security")
@DataScope(requireTenant = true)
public class SecurityMeController {

    private final EffectivePermissionService permissionService;

    public SecurityMeController(EffectivePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/me")
    public ApiResult<EffectivePermissionProfile> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // 在 dev/bypass 模式或未带 JWT Token 时，返回包含全量二级菜单的模拟超级管理员简档
            return ApiResult.ok(new EffectivePermissionProfile(
                "dev-admin",
                List.of(new EffectivePermissionProfile.RoleView(
                    "platform-admin",
                    "平台超级管理员 (Dev)",
                    "MOCK",
                    "TENANT",
                    "TENANT-001"
                )),
                List.of(new EffectivePermissionProfile.PermissionView(
                    "ALL",
                    "全部合规授权",
                    "LOW"
                )),
                List.of("workbench", "pilot-setup", "clinical-run", "quality-improve", "compliance-ops", "advanced-tools"),
                new EffectivePermissionProfile.DataScopeView(
                    "TENANT-001", null, "HOSP-001", null, null, null, null, null
                )
            ));
        }

        String userId = RequestContext.currentUserId().orElse(authentication.getName());
        return ApiResult.ok(permissionService.resolve(
            authentication,
            RequestContext.currentOrgScope(),
            userId
        ));
    }
}
