package com.medkernel.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieBearerTokenResolverTest {

    private final CookieBearerTokenResolver resolver =
        new CookieBearerTokenResolver(new AuthCookieProperties("mk_access", false, "Strict", "/medkernel", 28800));

    @Test
    void resolvesTokenFromCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("mk_access", "jwt-from-cookie"));
        assertThat(resolver.resolve(req)).isEqualTo("jwt-from-cookie");
    }

    @Test
    void fallsBackToAuthorizationHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer jwt-from-header");
        assertThat(resolver.resolve(req)).isEqualTo("jwt-from-header");
    }
}
