package com.medkernel.commercial;

import java.time.LocalDate;
import java.util.List;

/**
 * License 信息模型。
 */
public class LicenseInfo {

    private String licenseKey;
    private String licensee;
    private LicenseType licenseType;
    private LicenseTier tier;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private int maxUsers;
    private int maxSites;
    private List<String> features;
    private boolean trial;
    private String signature;

    public enum LicenseType {
        PERPETUAL, ANNUAL, TRIAL
    }

    public enum LicenseTier {
        STANDARD, PROFESSIONAL, FLAGSHIP
    }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public String getLicensee() { return licensee; }
    public void setLicensee(String licensee) { this.licensee = licensee; }

    public LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseType licenseType) { this.licenseType = licenseType; }

    public LicenseTier getTier() { return tier; }
    public void setTier(LicenseTier tier) { this.tier = tier; }

    public LocalDate getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDate issuedAt) { this.issuedAt = issuedAt; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public int getMaxSites() { return maxSites; }
    public void setMaxSites(int maxSites) { this.maxSites = maxSites; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public boolean isTrial() { return trial; }
    public void setTrial(boolean trial) { this.trial = trial; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    /**
     * 判断 License 是否已过期。
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDate.now().isAfter(expiresAt);
    }

    /**
     * 判断 License 是否即将过期（30 天内）。
     */
    public boolean isExpiringSoon() {
        return expiresAt != null && !isExpired()
                && LocalDate.now().plusDays(30).isAfter(expiresAt);
    }

    /**
     * 获取剩余天数。
     */
    public long getDaysRemaining() {
        if (expiresAt == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiresAt);
    }
}
