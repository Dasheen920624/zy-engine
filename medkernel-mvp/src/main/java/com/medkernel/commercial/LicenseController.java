package com.medkernel.commercial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * License 与用量报告 API。
 */
@RestController
@RequestMapping("/api/commercial")
@Tag(name = "商业授权", description = "License 管理、用量报告和功能开关")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Operation(summary = "获取当前 License 信息")
    @GetMapping("/license")
    public ResponseEntity<LicenseDto.LicenseInfoResponse> getLicenseInfo(HttpServletRequest request) {
        LicenseInfo license = licenseService.getCurrentLicense();
        LicenseDto.LicenseInfoResponse resp = new LicenseDto.LicenseInfoResponse();
        if (license != null) {
            resp.setLicensee(license.getLicensee());
            resp.setType(license.getLicenseType().name());
            resp.setTier(license.getTier().name());
            resp.setIssuedAt(license.getIssuedAt());
            resp.setExpiresAt(license.getExpiresAt());
            resp.setDaysRemaining(license.getDaysRemaining());
            resp.setMaxUsers(license.getMaxUsers());
            resp.setMaxSites(license.getMaxSites());
            resp.setFeatures(license.getFeatures());
            resp.setTrial(license.isTrial());
            resp.setValid(licenseService.isLicenseValid());
            resp.setExpiringSoon(license.isExpiringSoon());
        }
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "获取 License 状态")
    @GetMapping("/license/status")
    public ResponseEntity<LicenseDto.LicenseStatusResponse> getLicenseStatus(HttpServletRequest request) {
        LicenseService.LicenseStatus status = licenseService.getLicenseStatus();
        LicenseInfo license = licenseService.getCurrentLicense();

        LicenseDto.LicenseStatusResponse resp = new LicenseDto.LicenseStatusResponse();
        resp.setStatus(status.name());
        resp.setDegradedMode(licenseService.isDegradedMode());
        resp.setDegradationInfo(licenseService.getDegradationInfo());

        if (license != null) {
            resp.setDaysRemaining(license.getDaysRemaining());
            resp.setExpiresAt(license.getExpiresAt());
        }

        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "获取降级模式信息")
    @GetMapping("/license/degradation")
    public ResponseEntity<Map<String, Object>> getDegradationInfo(HttpServletRequest request) {
        return ResponseEntity.ok(licenseService.getDegradationInfo());
    }

    @Operation(summary = "更新 License")
    @PostMapping("/license")
    public ResponseEntity<Map<String, Object>> updateLicense(
            @Valid @RequestBody LicenseDto.UpdateLicenseRequest licenseRequest, HttpServletRequest request) {
        LicenseInfo license = new LicenseInfo();
        license.setLicenseKey(licenseRequest.getLicenseKey());
        license.setLicensee(licenseRequest.getLicensee());
        if (licenseRequest.getLicenseType() != null) {
            license.setLicenseType(LicenseInfo.LicenseType.valueOf(licenseRequest.getLicenseType()));
        }
        if (licenseRequest.getTier() != null) {
            license.setTier(LicenseInfo.LicenseTier.valueOf(licenseRequest.getTier()));
        }
        license.setExpiresAt(licenseRequest.getExpiresAt());
        if (licenseRequest.getMaxUsers() != null) {
            license.setMaxUsers(licenseRequest.getMaxUsers());
        }
        if (licenseRequest.getMaxSites() != null) {
            license.setMaxSites(licenseRequest.getMaxSites());
        }
        license.setFeatures(licenseRequest.getFeatures());
        if (licenseRequest.getTrial() != null) {
            license.setTrial(licenseRequest.getTrial());
        }
        license.setSignature(licenseRequest.getSignature());

        licenseService.updateLicense(license);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "updated");
        result.put("licensee", license.getLicensee());
        result.put("expires_at", license.getExpiresAt());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取用量报告")
    @GetMapping("/usage")
    public ResponseEntity<UsageReport> getUsageReport(HttpServletRequest request) {
        return ResponseEntity.ok(licenseService.getUsageReport());
    }

    @Operation(summary = "导出用量报告 CSV")
    @PostMapping("/usage/export")
    public ResponseEntity<byte[]> exportUsageReport(HttpServletRequest request) {
        UsageReport report = licenseService.getUsageReport();
        byte[] csvBytes = generateCsv(report);

        String filename = "usage_report_" + report.getReportDate() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .contentLength(csvBytes.length)
                .body(csvBytes);
    }

    @Operation(summary = "检查功能是否可用")
    @GetMapping("/feature/{code}")
    public ResponseEntity<LicenseDto.FeatureCheckResponse> checkFeature(
            @PathVariable String code, HttpServletRequest request) {
        boolean available = licenseService.isFeatureAvailable(code);
        LicenseDto.FeatureCheckResponse resp = new LicenseDto.FeatureCheckResponse();
        resp.setFeature(code);
        resp.setAvailable(available);
        if (!available) {
            if (licenseService.isDegradedMode()) {
                resp.setReason("License 已过期，系统处于只读降级模式");
            } else {
                resp.setReason("当前 License 不包含此功能");
            }
        }
        return ResponseEntity.ok(resp);
    }

    private byte[] generateCsv(UsageReport report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // BOM for Excel compatibility
        writer.write('\uFEFF');

        writer.println("报告日期,授权持有人,授权类型,剩余天数,活跃用户数,最大用户数");
        writer.printf("%s,%s,%s,%d,%d,%d%n",
                report.getReportDate(),
                report.getLicensee(),
                report.getLicenseType(),
                report.getDaysRemaining(),
                report.getActiveUserCount(),
                report.getMaxUsers());

        writer.println();
        writer.println("API路径,调用次数");
        if (report.getApiCallCounts() != null) {
            for (Map.Entry<String, Long> entry : report.getApiCallCounts().entrySet()) {
                writer.printf("%s,%d%n", entry.getKey(), entry.getValue());
            }
        }

        writer.println();
        writer.println("功能编码,使用次数");
        if (report.getFeatureUsageCounts() != null) {
            for (Map.Entry<String, Long> entry : report.getFeatureUsageCounts().entrySet()) {
                writer.printf("%s,%d%n", entry.getKey(), entry.getValue());
            }
        }

        writer.flush();
        return baos.toByteArray();
    }
}
