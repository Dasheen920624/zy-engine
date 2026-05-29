package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 开通成员入参：登录名必填；用户标识/角色/初始密码可选。
 * 初始密码留空则由系统生成随机临时密码并一次性返回。
 */
public record CreateMemberRequest(
    @NotBlank String username,
    String userId,
    String roleCode,
    String initialPassword
) {
    public String userIdOrUsername() {
        return (userId == null || userId.isBlank()) ? username : userId;
    }
}
