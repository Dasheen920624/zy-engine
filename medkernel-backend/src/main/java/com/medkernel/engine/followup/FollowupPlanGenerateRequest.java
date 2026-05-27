package com.medkernel.engine.followup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 随访计划智能生成请求数据契约 (GA-ENG-API-09)。
 */
public record FollowupPlanGenerateRequest(
    @NotBlank String patientId,
    @NotBlank String encounterId,
    String pathwayId,
    String diseaseCode,
    String riskLevel,
    @NotNull List<String> taskTypes
) {}
