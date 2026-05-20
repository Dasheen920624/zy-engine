package com.medkernel.security.sso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CAS 协议适配器。
 * CAS 2.0/3.0 协议：跳转 CAS 登录页 -> CAS 回调带 ticket -> 后端验证 ticket。
 */
public class CasSsoAdapter implements SsoAdapter {

    @Override
    public String getProtocolType() {
        return "CAS";
    }

    @Override
    public String buildLoginUrl(Map<String, String> config, String redirectUri, String state) {
        String casLoginUrl = config.get("login_url");
        if (casLoginUrl == null || casLoginUrl.isEmpty()) {
            throw new IllegalArgumentException("CAS login_url is required in config");
        }
        // CAS 协议：/cas/login?service=<callback>
        StringBuilder url = new StringBuilder(casLoginUrl);
        url.append(url.indexOf("?") > 0 ? "&" : "?");
        url.append("service=").append(redirectUri);
        if (state != null) {
            url.append("&state=").append(state);
        }
        return url.toString();
    }

    @Override
    public SsoVerifyResult verify(Map<String, String> config, String credential, String redirectUri) {
        String casValidateUrl = config.get("validate_url");
        if (casValidateUrl == null || casValidateUrl.isEmpty()) {
            return SsoVerifyResult.failure("CAS validate_url is required");
        }
        if (credential == null || credential.isEmpty()) {
            return SsoVerifyResult.failure("CAS ticket is required");
        }

        // MVP: 模拟 CAS ticket 验证
        // 生产环境应调用 casValidateUrl + "?service=" + redirectUri + "&ticket=" + credential
        // 解析 CAS XML/JSON 响应获取 user
        String externalSubject = "cas-user-" + credential.hashCode();
        String displayName = config.get("default_display_name");
        if (displayName == null) displayName = externalSubject;

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("ticket", credential);
        attributes.put("cas_version", config.getOrDefault("cas_version", "2.0"));

        return SsoVerifyResult.success(externalSubject, displayName, null, attributes);
    }
}
