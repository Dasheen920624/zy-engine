package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 记录检测结果请求 DTO：替代 AiSafetyController.recordDetection 的 HallucinationDetection 实体。
 */
@Schema(description = "记录检测结果请求")
public class RecordDetectionRequest {

    @NotBlank(message = "detectionCode 不能为空")
    @Schema(description = "检测编码", example = "DET_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String detectionCode;

    @NotBlank(message = "modelCode 不能为空")
    @Schema(description = "模型编码", example = "GLM_5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelCode;

    @Schema(description = "输入内容")
    private String inputContent;

    @Schema(description = "输出内容")
    private String outputContent;

    @NotBlank(message = "detectionType 不能为空")
    @Schema(description = "检测类型", example = "HALLUCINATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private String detectionType;

    @Schema(description = "判定结果", example = "POSITIVE")
    private String verdict;

    @Schema(description = "置信度", example = "0.95")
    private String confidence;

    @Schema(description = "证据")
    private String evidence;

    public String getDetectionCode() { return detectionCode; }
    public void setDetectionCode(String detectionCode) { this.detectionCode = detectionCode; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getInputContent() { return inputContent; }
    public void setInputContent(String inputContent) { this.inputContent = inputContent; }
    public String getOutputContent() { return outputContent; }
    public void setOutputContent(String outputContent) { this.outputContent = outputContent; }
    public String getDetectionType() { return detectionType; }
    public void setDetectionType(String detectionType) { this.detectionType = detectionType; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}
