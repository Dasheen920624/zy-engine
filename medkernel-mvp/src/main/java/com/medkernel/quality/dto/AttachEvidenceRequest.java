package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 附加证据请求 DTO：替代 AcceptanceTestController.attachEvidence 的 AcceptanceEvidence 实体。
 */
@Schema(description = "附加证据请求")
public class AttachEvidenceRequest {

    @NotBlank(message = "resultCode 不能为空")
    @Schema(description = "测试结果编码", example = "RES_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resultCode;

    @NotBlank(message = "evidenceType 不能为空")
    @Schema(description = "证据类型", example = "SCREENSHOT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String evidenceType;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "描述")
    private String description;

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
