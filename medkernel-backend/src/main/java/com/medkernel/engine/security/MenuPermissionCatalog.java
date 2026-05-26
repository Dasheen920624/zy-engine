package com.medkernel.engine.security;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 后端给前端的菜单可见性收敛表。
 *
 * <p>这里只输出一级业务域 key；具体路由仍由前端路由元数据决定。
 */
final class MenuPermissionCatalog {

    private MenuPermissionCatalog() {
    }

    static List<String> menuKeysFor(Set<PermissionCode> permissions) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        keys.add("workbench");
        addIfAny(keys, permissions, "pilot-setup",
            PermissionCode.ORG_WRITE,
            PermissionCode.TENANT_READ,
            PermissionCode.PACKAGE_READ,
            PermissionCode.TERM_WRITE,
            PermissionCode.RULE_WRITE,
            PermissionCode.PATHWAY_WRITE);
        addIfAny(keys, permissions, "clinical-run",
            PermissionCode.RECOMMENDATION_READ,
            PermissionCode.RECOMMENDATION_ACCEPT,
            PermissionCode.PATHWAY_READ,
            PermissionCode.RULE_READ);
        addIfAny(keys, permissions, "quality-improve",
            PermissionCode.EVALUATION_READ,
            PermissionCode.EVALUATION_WRITE,
            PermissionCode.EVALUATION_PUBLISH);
        addIfAny(keys, permissions, "compliance-ops",
            PermissionCode.AUDIT_READ,
            PermissionCode.AUDIT_EXPORT,
            PermissionCode.SYSTEM_READ,
            PermissionCode.SYSTEM_MANAGE);
        addIfAny(keys, permissions, "advanced-tools",
            PermissionCode.KNOWLEDGE_REVIEW,
            PermissionCode.SYSTEM_READ);
        return List.copyOf(keys);
    }

    private static void addIfAny(LinkedHashSet<String> keys,
                                 Set<PermissionCode> permissions,
                                 String menuKey,
                                 PermissionCode... candidates) {
        EnumSet<PermissionCode> candidateSet = EnumSet.noneOf(PermissionCode.class);
        candidateSet.addAll(List.of(candidates));
        if (candidateSet.stream().anyMatch(permissions::contains)) {
            keys.add(menuKey);
        }
    }
}
