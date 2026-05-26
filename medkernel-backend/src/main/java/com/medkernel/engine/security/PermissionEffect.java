package com.medkernel.engine.security;

/**
 * 租户级角色权限覆盖效果。
 *
 * <p>默认权限来自 {@link DefaultPermissionPolicy}；租户覆盖只允许在已登记权限上做显式允许或拒绝。
 */
public enum PermissionEffect {
    ALLOW,
    DENY
}
