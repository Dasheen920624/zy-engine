package com.medkernel.quality;

import java.time.LocalDateTime;

public class AcceptanceEvidence {
    private Long id;
    private Long tenantId;
    private String evidenceCode;
    private String resultCode;
    private String caseCode;
    private String evidenceType;
    private String description;
    private String filePath;
    private String fileHash;
    private long fileSize;
    private String mimeType;
    private String capturedBy;
    private LocalDateTime capturedTime;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getEvidenceCode() { return evidenceCode; }
    public void setEvidenceCode(String evidenceCode) { this.evidenceCode = evidenceCode; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getCapturedBy() { return capturedBy; }
    public void setCapturedBy(String capturedBy) { this.capturedBy = capturedBy; }
    public LocalDateTime getCapturedTime() { return capturedTime; }
    public void setCapturedTime(LocalDateTime capturedTime) { this.capturedTime = capturedTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getContent() { return description; }
    public void setContent(String content) { this.description = content; }
}
