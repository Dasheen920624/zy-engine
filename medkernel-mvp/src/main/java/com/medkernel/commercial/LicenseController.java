package com.medkernel.commercial;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * License 与用量报告 API。
 */
@RestController
@RequestMapping("/api/commercial")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * 获取当前 License 信息。
     */
    @GetMapping("/license")
    public ResponseEntity<Map<String, Object>> getLicenseInfo(HttpServletRequest request) {
        LicenseInfo license = licenseService.getCurrentLicense();
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        if (license != null) {
            info.put("licensee", license.getLicensee());
            info.put("type", license.getLicenseType().name());
            info.put("tier", license.getTier().name());
            info.put("issued_at", license.getIssuedAt());
            info.put("expires_at", license.getExpiresAt());
            info.put("days_remaining", license.getDaysRemaining());
            info.put("max_users", license.getMaxUsers());
            info.put("max_sites", license.getMaxSites());
            info.put("features", license.getFeatures());
            info.put("trial", license.isTrial());
            info.put("valid", licenseService.isLicenseValid());
            info.put("expiring_soon", license.isExpiringSoon());
        }
        return ResponseEntity.ok(info);
    }

    /**
     * 更新 License。
     */
    @PostMapping("/license")
    public ResponseEntity<Map<String, Object>> updateLicense(
            @RequestBody LicenseInfo license, HttpServletRequest request) {
        licenseService.updateLicense(license);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "updated");
        result.put("licensee", license.getLicensee());
        result.put("expires_at", license.getExpiresAt());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用量报告。
     */
    @GetMapping("/usage")
    public ResponseEntity<UsageReport> getUsageReport(HttpServletRequest request) {
        return ResponseEntity.ok(licenseService.getUsageReport());
    }

    /**
     * 检查功能是否可用。
     */
    @GetMapping("/feature/{code}")
    public ResponseEntity<Map<String, Object>> checkFeature(
            String code, HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("feature", code);
        result.put("available", licenseService.isFeatureAvailable(code));
        return ResponseEntity.ok(result);
    }
}
