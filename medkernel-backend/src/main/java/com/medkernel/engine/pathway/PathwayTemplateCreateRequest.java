package com.medkernel.engine.pathway;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 创建路径模板请求。
 *
 * <p>一次性携带模板主数据、节点、边和指标绑定，保存为可发布前校验的草稿资产。
 */
public record PathwayTemplateCreateRequest(
    @NotBlank String packageId,
    @NotBlank String templateCode,
    @NotBlank String name,
    @NotBlank String diseaseCode,
    @NotNull Integer templateVersion,
    @NotNull PathwayTemplateLevel templateLevel,
    @NotBlank String startNodeCode,
    @NotBlank String sourceRef,
    String description,
    JsonNode entryCriteria,
    JsonNode exitCriteria,
    @NotEmpty List<@Valid PathwayNodeRequest> nodes,
    List<@Valid PathwayEdgeRequest> edges,
    List<@Valid SpecialtyMetricBindingRequest> metricBindings
) {}
