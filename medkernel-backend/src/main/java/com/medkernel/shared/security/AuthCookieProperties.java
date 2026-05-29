package com.medkernel.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权 cookie 配置。dev(http) 必须 secure=false，否则浏览器不存 cookie；生产 / govcloud 覆盖为 true。
 */
@ConfigurationProperties(prefix = "medkernel.auth.cookie")
public record AuthCookieProperties(
    String name,
    boolean secure,
    String sameSite,
    String path,
    long maxAgeSeconds
) {
    public AuthCookieProperties {
        if (name == null || name.isBlank()) name = "mk_access";
        if (sameSite == null || sameSite.isBlank()) sameSite = "Strict";
        if (path == null || path.isBlank()) path = "/medkernel";
        if (maxAgeSeconds <= 0) maxAgeSeconds = 28800;
    }
}
