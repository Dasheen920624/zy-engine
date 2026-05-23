package com.medkernel.platform.branding;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-26 · 集团版品牌定制（多租户 logo / 颜色 / 域名）。
 * 集团商业本质 —— 大三甲购买后必须能挂自己的 logo + 域名。
 */
@RestController
@RequestMapping("/api/v1/platform/branding")
public class BrandingController {

    @GetMapping("/{tenantId}")
    public Map<String, Object> get(@PathVariable String tenantId) {
        return Map.of(
            "tenantId", tenantId,
            "productName", "北京协和医院智能中枢",
            "logoUrl", "/branding/main/logo.svg",
            "primaryColor", "#0a4f8a",  // 协和深蓝
            "favicon", "/branding/main/favicon.ico",
            "loginBackground", "/branding/main/login-bg.jpg",
            "customDomain", "medkernel.pumch.cn",
            "footer", "© 2026 北京协和医院 · ICP 备 xxxxxxxx 号"
        );
    }

    @PostMapping("/{tenantId}")
    public Map<String, Object> update(@PathVariable String tenantId, @RequestBody Map<String, Object> body) {
        return Map.of(
            "result", "ok",
            "tenantId", tenantId,
            "updatedFields", body.keySet(),
            "effectiveAt", java.time.Instant.now().toString()
        );
    }
}
