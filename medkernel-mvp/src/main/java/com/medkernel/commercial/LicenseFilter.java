package com.medkernel.commercial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * License 降级过滤器。
 *
 * 策略：
 * - 每次请求检查 License 状态
 * - License 有效时：正常放行
 * - License 即将过期（WARNING）：放行但添加响应头提醒
 * - License 已过期（EXPIRED）：GET 请求放行（只读模式），POST/PUT/DELETE 返回 403
 * - 跳过 /api/commercial/** 路径（允许更新 License）
 * - 跳过 /actuator/** 健康检查路径
 * - 所有响应添加 X-License-Status 头：VALID/WARNING/EXPIRED
 */
@Component
@Order(50) // 在安全过滤器之前执行
public class LicenseFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LicenseFilter.class);

    private static final String LICENSE_STATUS_HEADER = "X-License-Status";

    private static final Set<String> READ_METHODS = new HashSet<String>(Arrays.asList(
            "GET", "HEAD", "OPTIONS"
    ));

    private static final Set<String> SKIP_PATH_PREFIXES = new HashSet<String>(Arrays.asList(
            "/api/commercial/",
            "/actuator/"
    ));

    private final LicenseService licenseService;

    public LicenseFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();

        // 跳过 License 管理和健康检查路径
        if (shouldSkip(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        LicenseService.LicenseStatus status = licenseService.getLicenseStatus();

        // 所有响应添加 License 状态头
        httpResponse.setHeader(LICENSE_STATUS_HEADER, status.name());

        switch (status) {
            case VALID:
                chain.doFilter(request, response);
                break;

            case WARNING:
                // 即将过期，仍允许所有操作
                log.debug("[license-filter] license warning, {} days remaining for {}",
                        licenseService.getCurrentLicense() != null ? licenseService.getCurrentLicense().getDaysRemaining() : 0,
                        requestUri);
                chain.doFilter(request, response);
                break;

            case EXPIRED:
                // 只读模式：GET/HEAD/OPTIONS 放行，POST/PUT/DELETE 拒绝
                if (READ_METHODS.contains(httpRequest.getMethod().toUpperCase())) {
                    chain.doFilter(request, response);
                } else {
                    log.warn("[license-filter] blocked write operation in degraded mode: {} {}",
                            httpRequest.getMethod(), requestUri);
                    sendDegradedResponse(httpResponse, requestUri);
                }
                break;

            default:
                chain.doFilter(request, response);
                break;
        }
    }

    private boolean shouldSkip(String requestUri) {
        if (requestUri == null) return false;
        for (String prefix : SKIP_PATH_PREFIXES) {
            if (requestUri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void sendDegradedResponse(HttpServletResponse response, String requestUri) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"error\":\"LICENSE_EXPIRED\","
                + "\"message\":\"License 已过期，系统处于只读降级模式，写入操作已被禁止。请续期 License 后恢复。\","
                + "\"path\":\"" + escapeJson(requestUri) + "\","
                + "\"degraded\":true}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
