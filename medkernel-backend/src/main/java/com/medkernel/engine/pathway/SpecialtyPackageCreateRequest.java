package com.medkernel.engine.pathway;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SpecialtyPackageCreateRequest(
    @NotBlank String packageCode,
    @NotBlank String diseaseCode,
    @NotBlank String name,
    @NotBlank String packageVersion,
    @NotBlank String sourceRef,
    String description,
    List<@Valid SpecialtyProfileRequest> profiles
) {}
