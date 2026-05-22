package com.medkernel.security.sso;

public class SsoUserInfo {
    private String subject;
    private String name;
    private String email;
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}
