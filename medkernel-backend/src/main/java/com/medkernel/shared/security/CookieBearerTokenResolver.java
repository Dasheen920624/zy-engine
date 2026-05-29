package com.medkernel.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * 优先从 mk_access cookie 取 JWT；无则回退标准 Authorization: Bearer（兼容 embed / API 客户端）。
 */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final AuthCookieProperties cookieProps;
    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    public CookieBearerTokenResolver(AuthCookieProperties cookieProps) {
        this.cookieProps = cookieProps;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (cookieProps.name().equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        return headerResolver.resolve(request);
    }
}
