package com.medkernel.security.sso;

import com.medkernel.security.SecurityUser;

public class SsoLoginResult {
    private String token;
    private SecurityUser user;
    private SsoSession session;

    public SsoLoginResult(String token, SecurityUser user, SsoSession session) {
        this.token = token;
        this.user = user;
        this.session = session;
    }

    public String getToken() { return token; }
    public SecurityUser getUser() { return user; }
    public SsoSession getSession() { return session; }
}
