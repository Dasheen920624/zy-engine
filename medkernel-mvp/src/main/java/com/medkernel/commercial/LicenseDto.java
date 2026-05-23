package com.medkernel.commercial;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * License 模块 DTO 集合。
 */
public class LicenseDto {

    private LicenseDto() {
    }

    /**
     * License 信息响应。
     */
    @Schema(description = "License信息响应")
    public static class LicenseInfoResponse {

        @Schema(description = "授权持有人")
        private String licensee;

        @Schema(description = "授权类型：PERPETUAL/ANNUAL/TRIAL")
        private String type;

        @Schema(description = "授权层级：STANDARD/PROFESSIONAL/FLAGSHIP")
        private String tier;

        @Schema(description = "签发日期")
        private LocalDate issuedAt;

        @Schema(description = "到期日期")
        private LocalDate expiresAt;

        @Schema(description = "剩余天数")
        private long daysRemaining;

        @Schema(description = "最大用户数")
        private int maxUsers;

        @Schema(description = "最大站点数")
        private int maxSites;

        @Schema(description = "功能列表")
        private List<String> features;

        @Schema(description = "是否试用")
        private boolean trial;

        @Schema(description = "是否有效")
        private boolean valid;

        @Schema(description = "是否即将过期")
        private boolean expiringSoon;

        public String getLicensee() { return licensee; }
        public void setLicensee(String licensee) { this.licensee = licensee; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }

        public LocalDate getIssuedAt() { return issuedAt; }
        public void setIssuedAt(LocalDate issuedAt) { this.issuedAt = issuedAt; }

        public LocalDate getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

        public long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }

        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

        public int getMaxSites() { return maxSites; }
        public void setMaxSites(int maxSites) { this.maxSites = maxSites; }

        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }

        public boolean isTrial() { return trial; }
        public void setTrial(boolean trial) { this.trial = trial; }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public boolean isExpiringSoon() { return expiringSoon; }
        public void setExpiringSoon(boolean expiringSoon) { this.expiringSoon = expiringSoon; }
    }

    /**
     * License 状态响应。
     */
    @Schema(description = "License状态响应")
    public static class LicenseStatusResponse {

        @Schema(description = "License状态：VALID/WARNING/EXPIRED")
        private String status;

        @Schema(description = "剩余天数")
        private long daysRemaining;

        @Schema(description = "到期日期")
        private LocalDate expiresAt;

        @Schema(description = "是否处于降级模式")
        private boolean degradedMode;

        @Schema(description = "降级信息")
        private Map<String, Object> degradationInfo;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }

        public LocalDate getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

        public boolean isDegradedMode() { return degradedMode; }
        public void setDegradedMode(boolean degradedMode) { this.degradedMode = degradedMode; }

        public Map<String, Object> getDegradationInfo() { return degradationInfo; }
        public void setDegradationInfo(Map<String, Object> degradationInfo) { this.degradationInfo = degradationInfo; }
    }

    /**
     * 更新 License 请求。
     */
    @Schema(description = "更新License请求")
    public static class UpdateLicenseRequest {

        @NotBlank(message = "licenseKey 不能为空")
        @Schema(description = "License密钥", requiredMode = Schema.RequiredMode.REQUIRED)
        private String licenseKey;

        @Schema(description = "授权持有人")
        private String licensee;

        @Schema(description = "授权类型：PERPETUAL/ANNUAL/TRIAL")
        private String licenseType;

        @Schema(description = "授权层级：STANDARD/PROFESSIONAL/FLAGSHIP")
        private String tier;

        @Schema(description = "到期日期")
        private LocalDate expiresAt;

        @Schema(description = "最大用户数")
        private Integer maxUsers;

        @Schema(description = "最大站点数")
        private Integer maxSites;

        @Schema(description = "功能列表")
        private List<String> features;

        @Schema(description = "是否试用")
        private Boolean trial;

        @Schema(description = "签名")
        private String signature;

        public String getLicenseKey() { return licenseKey; }
        public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

        public String getLicensee() { return licensee; }
        public void setLicensee(String licensee) { this.licensee = licensee; }

        public String getLicenseType() { return licenseType; }
        public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }

        public LocalDate getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

        public Integer getMaxSites() { return maxSites; }
        public void setMaxSites(Integer maxSites) { this.maxSites = maxSites; }

        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }

        public Boolean getTrial() { return trial; }
        public void setTrial(Boolean trial) { this.trial = trial; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }

    /**
     * 功能检查响应。
     */
    @Schema(description = "功能检查响应")
    public static class FeatureCheckResponse {

        @Schema(description = "功能编码")
        private String feature;

        @Schema(description = "是否可用")
        private boolean available;

        @Schema(description = "不可用原因")
        private String reason;

        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
