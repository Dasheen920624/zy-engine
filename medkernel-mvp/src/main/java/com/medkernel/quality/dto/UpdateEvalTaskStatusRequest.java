package com.medkernel.quality.dto;

import javax.validation.constraints.NotBlank;

/**
 * 更新评测任务状态请求 DTO。
 */
public class UpdateEvalTaskStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private Double accuracyScore;
    private Double latencyMs;
    private Double passRate;
    private String resultSummary;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Double accuracyScore) { this.accuracyScore = accuracyScore; }

    public Double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Double latencyMs) { this.latencyMs = latencyMs; }

    public Double getPassRate() { return passRate; }
    public void setPassRate(Double passRate) { this.passRate = passRate; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
}
