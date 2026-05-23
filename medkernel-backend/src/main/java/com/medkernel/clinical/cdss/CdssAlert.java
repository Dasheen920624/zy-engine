package com.medkernel.clinical.cdss;

public record CdssAlert(
    String id,
    String text,
    String source,
    Double adoptionRate,
    String status,
    String doctor
) {}
