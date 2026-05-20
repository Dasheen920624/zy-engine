package com.medkernel.knowledge;

import java.time.LocalDateTime;

/**
 * AI 候选审核实体。
 * 对应数据库表：ai_candidate_review
 */
public class AiCandidateReview {
    private Long id;
    private Long tenantId;
    private String candidateCode;
    private String candidateType;      // TERMINOLOGY_MAPPING/RULE/KNOWLEDGE_ASSET/PATHWAY
    private String candidateName;
    private String sourceCode;         // 来源编码
    private String sourceName;
    private String modelProvider;
    private String modelName;
    private Double confidence;         // 置信度
    private String candidateContent;   // 候选内容 JSON
    private String reviewStatus;       // PENDING/APPROVED/REJECTED/MODIFIED
    private String reviewedBy;
    private LocalDateTime reviewedTime;
    private String reviewNote;
    private String modifiedContent;    // 修改后内容（MODIFIED时使用）
    private String qualityFindings;    // 质检发现摘要
    private String priority;           // HIGH/MEDIUM/LOW
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCandidateCode() { return candidateCode; }
    public void setCandidateCode(String candidateCode) { this.candidateCode = candidateCode; }

    public String getCandidateType() { return candidateType; }
    public void setCandidateType(String candidateType) { this.candidateType = candidateType; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getCandidateContent() { return candidateContent; }
    public void setCandidateContent(String candidateContent) { this.candidateContent = candidateContent; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(LocalDateTime reviewedTime) { this.reviewedTime = reviewedTime; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public String getModifiedContent() { return modifiedContent; }
    public void setModifiedContent(String modifiedContent) { this.modifiedContent = modifiedContent; }

    public String getQualityFindings() { return qualityFindings; }
    public void setQualityFindings(String qualityFindings) { this.qualityFindings = qualityFindings; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
