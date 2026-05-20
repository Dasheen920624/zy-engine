package com.medkernel.security.sso;

import java.util.HashMap;
import java.util.Map;

/**
 * SAML 协议适配器。
 * SAML SP 发起：跳转 IdP SSO URL -> IdP 回调带 SAML Assertion -> 后端验证 Assertion。
 */
public class SamlSsoAdapter implements SsoAdapter {

    @Override
    public String getProtocolType() {
        return "SAML";
    }

    @Override
    public String buildLoginUrl(Map<String, String> config, String redirectUri, String state) {
        String idpSsoUrl = config.get("idp_sso_url");
        String spEntityId = config.get("sp_entity_id");
        if (idpSsoUrl == null || spEntityId == null) {
            throw new IllegalArgumentException("SAML idp_sso_url and sp_entity_id are required");
        }
        // SAML SP 发起：构建 AuthnRequest 并重定向到 IdP
        StringBuilder url = new StringBuilder(idpSsoUrl);
        url.append("?SAMLRequest=PLACEHOLDER");
        url.append("&RelayState=").append(state != null ? state : redirectUri);
        url.append("&sp_entity_id=").append(spEntityId);
        return url.toString();
    }

    @Override
    public SsoVerifyResult verify(Map<String, String> config, String credential, String redirectUri) {
        String idpCertificate = config.get("idp_certificate");
        String spEntityId = config.get("sp_entity_id");
        if (idpCertificate == null || spEntityId == null) {
            return SsoVerifyResult.failure("SAML idp_certificate and sp_entity_id are required");
        }
        if (credential == null || credential.isEmpty()) {
            return SsoVerifyResult.failure("SAML assertion is required");
        }

        // MVP: 模拟 SAML Assertion 验证
        // 生产环境应解析 SAML Response XML，验证签名，提取 NameID
        String externalSubject = "saml-user-" + credential.hashCode();
        String displayName = config.get("default_display_name");
        if (displayName == null) displayName = externalSubject;

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("assertion_ref", credential);
        attributes.put("sp_entity_id", spEntityId);

        return SsoVerifyResult.success(externalSubject, displayName, null, attributes);
    }
}
