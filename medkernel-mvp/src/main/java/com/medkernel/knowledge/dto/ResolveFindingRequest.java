package com.medkernel.knowledge.dto;

/**
 * 解决质检发现请求 DTO。
 */
public class ResolveFindingRequest {

    private String resolvedBy;
    private String resolutionNote;

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
