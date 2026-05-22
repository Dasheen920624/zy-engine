package com.medkernel.commercial;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用量报告模型。
 */
public class UsageReport {

    private LocalDate reportDate;
    private String licensee;
    private String licenseType;
    private long daysRemaining;
    private int activeUserCount;
    private int maxUsers;
    private Map<String, Long> apiCallCounts;
    private Map<String, Long> featureUsageCounts;

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String getLicensee() { return licensee; }
    public void setLicensee(String licensee) { this.licensee = licensee; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }

    public int getActiveUserCount() { return activeUserCount; }
    public void setActiveUserCount(int activeUserCount) { this.activeUserCount = activeUserCount; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public Map<String, Long> getApiCallCounts() { return apiCallCounts; }
    public void setApiCallCounts(Map<String, Long> apiCallCounts) { this.apiCallCounts = apiCallCounts; }

    public Map<String, Long> getFeatureUsageCounts() { return featureUsageCounts; }
    public void setFeatureUsageCounts(Map<String, Long> featureUsageCounts) { this.featureUsageCounts = featureUsageCounts; }
}
