package com.medkernel.knowledge.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 质量汇总响应 DTO。
 */
@Schema(description = "质量汇总响应")
public class QualitySummaryResponse {

    @Schema(description = "总发现数")
    private int totalFindings;

    @Schema(description = "严重数量")
    private int criticalCount;

    @Schema(description = "警告数量")
    private int warningCount;

    @Schema(description = "信息数量")
    private int infoCount;

    @Schema(description = "已解决数量")
    private int resolvedCount;

    @Schema(description = "未解决数量")
    private int unresolvedCount;

    public static QualitySummaryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        QualitySummaryResponse resp = new QualitySummaryResponse();
        resp.totalFindings = toInt(map.get("total_findings"));
        resp.criticalCount = toInt(map.get("critical_count"));
        resp.warningCount = toInt(map.get("warning_count"));
        resp.infoCount = toInt(map.get("info_count"));
        resp.resolvedCount = toInt(map.get("resolved_count"));
        resp.unresolvedCount = toInt(map.get("unresolved_count"));
        return resp;
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    // Getters and Setters
    public int getTotalFindings() { return totalFindings; }
    public void setTotalFindings(int totalFindings) { this.totalFindings = totalFindings; }

    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }

    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }

    public int getInfoCount() { return infoCount; }
    public void setInfoCount(int infoCount) { this.infoCount = infoCount; }

    public int getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(int resolvedCount) { this.resolvedCount = resolvedCount; }

    public int getUnresolvedCount() { return unresolvedCount; }
    public void setUnresolvedCount(int unresolvedCount) { this.unresolvedCount = unresolvedCount; }
}
