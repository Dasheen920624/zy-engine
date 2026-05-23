package com.medkernel.knowledge.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审核汇总响应 DTO。
 */
@Schema(description = "审核汇总响应")
public class ReviewSummaryResponse {

    @Schema(description = "总候选数")
    private int totalCandidates;

    @Schema(description = "待审核数")
    private int pendingCount;

    @Schema(description = "已通过数")
    private int approvedCount;

    @Schema(description = "已拒绝数")
    private int rejectedCount;

    @Schema(description = "按候选类型统计")
    private Map<String, Integer> byCandidateType;

    @SuppressWarnings("unchecked")
    public static ReviewSummaryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        ReviewSummaryResponse resp = new ReviewSummaryResponse();
        resp.totalCandidates = toInt(map.get("total_candidates"));
        resp.pendingCount = toInt(map.get("pending_count"));
        resp.approvedCount = toInt(map.get("approved_count"));
        resp.rejectedCount = toInt(map.get("rejected_count"));
        resp.byCandidateType = (Map<String, Integer>) map.get("by_candidate_type");
        return resp;
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    // Getters and Setters
    public int getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(int totalCandidates) { this.totalCandidates = totalCandidates; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getApprovedCount() { return approvedCount; }
    public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }

    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

    public Map<String, Integer> getByCandidateType() { return byCandidateType; }
    public void setByCandidateType(Map<String, Integer> byCandidateType) { this.byCandidateType = byCandidateType; }
}
