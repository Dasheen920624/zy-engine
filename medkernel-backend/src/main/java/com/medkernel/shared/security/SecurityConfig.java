package com.medkernel.shared.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * GA-CORE-02 / W1-G3 闸门：Spring Security 6 OAuth2 Resource Server + JWT。
 *
 * <p>v1.0 GA 默认安全口径（与 docs/CONSTITUTION.md §1 第 8 条对齐）：
 * <ul>
 *   <li>JWT bearer token 由身份服务（OIDC / SAML / 国密 CA）签发；本应用作为 Resource Server 验签
 *   <li>无状态会话（不使用 HttpSession）
 *   <li>CSRF 关闭（前后端分离 + Bearer token）
 *   <li>白名单：/api/v1/system/** + /actuator/health + /actuator/prometheus + Swagger
 *   <li>dev profile 用 HS256 + 共享密钥简化本地开发；prod 必须用 RS256 + JWKS
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/system/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

    /**
     * dev profile：用 HS256 + 配置文件密钥，便于本地不依赖外部 OIDC。
     * prod 必须替换为基于 JWKS 的 RS256 / ES256（GA-CORE-02 详细实装时补）。
     */
    @Bean
    @Profile({"dev", "test"})
    JwtDecoder devJwtDecoder(@Value("${medkernel.jwt.dev-secret:medkernel-dev-secret-please-change-at-least-32-bytes}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
