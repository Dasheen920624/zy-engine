package com.medkernel.security.sso;

import java.util.HashMap;
import java.util.Map;

/**
 * LDAP-AD 协议适配器。
 * LDAP Bind 认证：前端提交用户名+密码 -> 后端用 LDAP Bind 验证 -> 获取用户属性。
 * 注意：LDAP 不是浏览器跳转协议，而是直接认证协议。
 */
public class LdapAdSsoAdapter implements SsoAdapter {

    @Override
    public String getProtocolType() {
        return "LDAP-AD";
    }

    @Override
    public String buildLoginUrl(Map<String, String> config, String redirectUri, String state) {
        // LDAP 不需要浏览器跳转，返回前端登录页即可
        return redirectUri + "?method=ldap&provider=" + config.get("provider_code");
    }

    @Override
    public SsoVerifyResult verify(Map<String, String> config, String credential, String redirectUri) {
        String ldapUrl = config.get("ldap_url");
        String baseDn = config.get("base_dn");
        if (ldapUrl == null || baseDn == null) {
            return SsoVerifyResult.failure("LDAP ldap_url and base_dn are required");
        }
        if (credential == null || credential.isEmpty()) {
            return SsoVerifyResult.failure("LDAP bind DN is required");
        }

        // MVP: 模拟 LDAP Bind 认证
        // 生产环境应使用 JNDI/LDAP 连接池执行 bind + search
        // credential 格式为 "username:password" 或 "bind_dn:password"
        String[] parts = credential.split(":", 2);
        String username = parts[0];
        String externalSubject = username;
        String displayName = config.get("default_display_name");
        if (displayName == null) displayName = username;

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("ldap_url", ldapUrl);
        attributes.put("base_dn", baseDn);
        attributes.put("username", username);

        return SsoVerifyResult.success(externalSubject, displayName, null, attributes);
    }
}
