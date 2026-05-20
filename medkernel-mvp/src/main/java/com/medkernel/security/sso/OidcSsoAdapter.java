package com.medkernel.security.sso;

import java.util.HashMap;
import java.util.Map;

/**
 * OIDC 协议适配器。
 * OIDC Authorization Code Flow：跳转授权端点 -> 回调带 code -> 后端用 code 换 token -> 验证 ID Token。
 */
public class OidcSsoAdapter implements SsoAdapter {

    @Override
    public String getProtocolType() {
        return "OIDC";
    }

    @Override
    public String buildLoginUrl(Map<String, String> config, String redirectUri, String state) {
        String authorizationEndpoint = config.get("authorization_endpoint");
        String clientId = config.get("client_id");
        if (authorizationEndpoint == null || clientId == null) {
            throw new IllegalArgumentException("OIDC authorization_endpoint and client_id are required");
        }
        StringBuilder url = new StringBuilder(authorizationEndpoint);
        url.append("?response_type=code");
        url.append("&client_id=").append(clientId);
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&scope=openid+profile+email");
        if (state != null) {
            url.append("&state=").append(state);
        }
        return url.toString();
    }

    @Override
    public SsoVerifyResult verify(Map<String, String> config, String credential, String redirectUri) {
        String tokenEndpoint = config.get("token_endpoint");
        String clientId = config.get("client_id");
        String clientSecret = config.get("client_secret");
        if (tokenEndpoint == null || clientId == null) {
            return SsoVerifyResult.failure("OIDC token_endpoint and client_id are required");
        }
        if (credential == null || credential.isEmpty()) {
            return SsoVerifyResult.failure("OIDC authorization code is required");
        }

        // MVP: 模拟 OIDC code 换 token
        // 生产环境应 POST tokenEndpoint 用 code 换 access_token + id_token
        // 解析 ID Token JWT 获取 sub / name / email
        String externalSubject = "oidc-user-" + credential.hashCode();
        String displayName = config.get("default_display_name");
        if (displayName == null) displayName = externalSubject;

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("code", credential);
        attributes.put("issuer", config.getOrDefault("issuer", ""));
        attributes.put("client_id", clientId);

        return SsoVerifyResult.success(externalSubject, displayName, null, attributes);
    }
}
