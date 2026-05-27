package com.medkernel.engine.pathway;

import jakarta.validation.constraints.NotBlank;

/**
 * 患者入径请求。
 *
 * <p>指定患者、就诊、路径模板和可选起始节点，用于创建新的患者路径运行实例。
 */
public record PatientPathwayEnterRequest(
    @NotBlank String patientId,
    String encounterId,
    @NotBlank String templateId,
    String startNodeCode
) {}
