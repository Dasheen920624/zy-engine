package com.medkernel.engine.security.auth;

/**
 * 重置密码出参：一次性返回的临时密码，成员须首登改密。
 */
public record ResetPasswordResponse(String tempPassword) {}
