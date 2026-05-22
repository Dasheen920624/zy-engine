package com.medkernel.quality;

import java.time.LocalDateTime;

public class HallucinationDetection {
    private Long id;
    private Long tenantId;
    private String detectionCode;
    private String modelCode;
    private String modelVersion;
    private String promptTemplateCode;
    private String inputContent;      // 输入内容
    private String outputContent;     // 输出内容
    private String detectionType;     // FACTUAL/LOGICAL/CONTEXTUAL/REFERENTIAL
    private Double confidenceScore;   // 置信度分数
    private String verdict;           // HALLUCINATION/LIKELY_HALLUCINATION/UNCERTAIN/SAFE
    private String evidence;          // 证据
    private String protectionAction;  // BLOCK/DEGRADE/HUMAN_REVIEW/PASS
    private String reviewer;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private String status;            // DETECTED/REVIEWING/RESOLVED/DISMISSED
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getDetectionCode() {
        return detectionCode;
    }

    public void setDetectionCode(String detectionCode) {
        this.detectionCode = detectionCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getPromptTemplateCode() {
        return promptTemplateCode;
    }

    public void setPromptTemplateCode(String promptTemplateCode) {
        this.promptTemplateCode = promptTemplateCode;
    }

    public String getInputContent() {
        return inputContent;
    }

    public void setInputContent(String inputContent) {
        this.inputContent = inputContent;
    }

    public String getOutputContent() {
        return outputContent;
    }

    public void setOutputContent(String outputContent) {
        this.outputContent = outputContent;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getProtectionAction() {
        return protectionAction;
    }

    public void setProtectionAction(String protectionAction) {
        this.protectionAction = protectionAction;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
