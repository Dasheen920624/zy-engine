package com.medkernel.security.sso;

import java.util.Map;

/**
 * SSO 适配器接口。
 * 统一 CAS/OIDC/SAML/LDAP-AD 协议差异。
 */
public interface SsoAdapter {

    /**
     * 获取适配器支持的协议类型。
     */
    String getProtocolType();

    /**
     * 构建外部 SSO 系统的登录跳转 URL。
     *
     * @param config     身份源配置（含 endpoint、clientId 等）
     * @param redirectUri 回调地址
     * @param state      防重放状态参数
     * @return 外部 SSO 系统的登录 URL
     */
    String buildLoginUrl(Map<String, String> config, String redirectUri, String state);

    /**
     * 验证外部 SSO 系统返回的身份断言（ticket/code/assertion）。
     *
     * @param config     身份源配置
     * @param credential 外部返回的凭据（CAS ticket / OIDC code / SAML assertion / LDAP DN）
     * @param redirectUri 回调地址（部分协议需要）
     * @return 验证结果，包含 externalSubject 和可选的属性
     */
    SsoVerifyResult verify(Map<String, String> config, String credential, String redirectUri);

    /**
     * SSO 验证结果。
     */
    class SsoVerifyResult {
        private boolean success;
        private String externalSubject;
        private String externalDisplayName;
        private String externalEmail;
        private Map<String, String> attributes;
        private String errorMessage;

        public static SsoVerifyResult success(String externalSubject, String displayName,
                                               String email, Map<String, String> attributes) {
            SsoVerifyResult result = new SsoVerifyResult();
            result.success = true;
            result.externalSubject = externalSubject;
            result.externalDisplayName = displayName;
            result.externalEmail = email;
            result.attributes = attributes;
            return result;
        }

        public static SsoVerifyResult failure(String errorMessage) {
            SsoVerifyResult result = new SsoVerifyResult();
            result.success = false;
            result.errorMessage = errorMessage;
            return result;
        }

        public boolean isSuccess() { return success; }
        public String getExternalSubject() { return externalSubject; }
        public String getExternalDisplayName() { return externalDisplayName; }
        public String getExternalEmail() { return externalEmail; }
        public Map<String, String> getAttributes() { return attributes; }
        public String getErrorMessage() { return errorMessage; }
    }
}
