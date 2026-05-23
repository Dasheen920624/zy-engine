package com.medkernel.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识来源查询请求 DTO：用于 KnowledgeController.listSources 查询参数。
 */
@Schema(description = "知识来源查询请求")
public class SourceQueryRequest {

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "审核状态")
    private String reviewStatus;

    @Schema(description = "权威等级")
    private String authorityLevel;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(String authorityLevel) { this.authorityLevel = authorityLevel; }
}
