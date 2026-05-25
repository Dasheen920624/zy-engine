package com.medkernel.engine.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * MedKernel v1.0 GA · 权限评估器，供 SpEL {@code @PreAuthorize("@perm.has('rule.publish')")} 使用。
 *
 * <p>评估流程：
 * <ol>
 *   <li>从 {@link SecurityContextHolder} 取出当前 {@link Authentication} 的所有 {@link GrantedAuthority}</li>
 *   <li>过滤出 {@code ROLE_*} 形式，反查为 {@link RoleCode}</li>
 *   <li>取每个角色的默认权限集合并集（来自 {@link DefaultPermissionPolicy}）</li>
 *   <li>判断指定 {@link PermissionCode} 是否在集合中</li>
 * </ol>
 *
 * <p>Bean 名是 {@code perm}（由 {@link Component} value 指定），让 SpEL 写起来短：
 * <pre>{@code
 * @PreAuthorize("@perm.has('rule.publish')")
 * public ApiResult<Rule> publish(...) { ... }
 * }</pre>
 *
 * <p>未来 GA-ENG-BASE-02 Phase 2 引入 {@code role_permission} DB 表后，
 * 本类增加一层 {@code RolePermissionOverrideRepository} 读取，叠加在默认策略之上。
 */
@Component("perm")
public class PermissionEvaluator {

    /**
     * 当前线程的 Authentication 是否拥有指定权限码。
     */
    public boolean has(String permissionCode) {
        return PermissionCode.fromCode(permissionCode)
            .map(this::currentHas)
            .orElse(false);
    }

    /**
     * 当前线程的 Authentication 是否拥有指定权限码（强类型）。
     */
    public boolean has(PermissionCode permission) {
        return currentHas(permission);
    }

    /**
     * 当前线程的 Authentication 是否拥有列出权限中的任意一个。
     */
    public boolean hasAny(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        Set<PermissionCode> effective = effectivePermissions();
        for (String code : permissionCodes) {
            if (PermissionCode.fromCode(code).map(effective::contains).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前线程的 Authentication 是否拥有列出的全部权限。
     */
    public boolean hasAll(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        Set<PermissionCode> effective = effectivePermissions();
        for (String code : permissionCodes) {
            if (!PermissionCode.fromCode(code).map(effective::contains).orElse(false)) {
                return false;
            }
        }
        return true;
    }

    private boolean currentHas(PermissionCode permission) {
        return effectivePermissions().contains(permission);
    }

    /**
     * 当前线程所有有效权限集合（按当前 Authentication 的 roles 聚合）。
     */
    public Set<PermissionCode> effectivePermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return EnumSet.noneOf(PermissionCode.class);
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return EnumSet.noneOf(PermissionCode.class);
        }
        EnumSet<PermissionCode> effective = EnumSet.noneOf(PermissionCode.class);
        for (GrantedAuthority a : authorities) {
            RoleCode.fromAuthority(a.getAuthority())
                .ifPresent(role -> effective.addAll(DefaultPermissionPolicy.permissionsOf(role)));
        }
        return effective;
    }
}
