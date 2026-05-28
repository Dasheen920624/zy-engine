package com.medkernel.engine.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 外部第三方对接适配器创建 DTO Record。
 *
 * <p>用于封装注册异构系统适配器时的业务字段及 JSR-380 输入校验规则。
 */
public record AdapterCreateDto(
    @NotBlank(message = "适配器ID不能为空")
    String adapterId,

    @NotBlank(message = "系统名称不能为空")
    String name,

    @NotBlank(message = "协议类型不能为空")
    @Pattern(regexp = "HL7|FHIR|Webhook|REST|WebService", message = "协议类型必须为 HL7, FHIR, Webhook, REST, WebService 之一")
    String protocolType,

    String configJson
) {}
