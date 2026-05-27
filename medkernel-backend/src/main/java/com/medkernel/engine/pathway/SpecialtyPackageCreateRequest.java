package com.medkernel.engine.pathway;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建专病包请求。
 *
 * <p>包含专病包编码、病种、名称、版本、来源引用、说明和可选专病画像列表。
 */
public record SpecialtyPackageCreateRequest(
    @NotBlank String packageCode,
    @NotBlank String diseaseCode,
    @NotBlank String name,
    @NotBlank String packageVersion,
    @NotBlank String sourceRef,
    String description,
    List<@Valid SpecialtyProfileRequest> profiles
) {}
