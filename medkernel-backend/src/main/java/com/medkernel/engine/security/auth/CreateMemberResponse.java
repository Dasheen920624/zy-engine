package com.medkernel.engine.security.auth;

/**
 * 开通成员出参：用户标识与登录名；tempPassword 仅当系统生成临时密码时一次性返回（否则为 null）。
 */
public record CreateMemberResponse(String userId, String username, String tempPassword) {}
