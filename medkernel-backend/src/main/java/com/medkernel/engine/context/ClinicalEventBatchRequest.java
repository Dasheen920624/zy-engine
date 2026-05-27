package com.medkernel.engine.context;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 临床事件批量接收请求。
 */
public record ClinicalEventBatchRequest(
    @NotEmpty @Size(max = 100) List<@Valid ClinicalEventRequest> events
) {}
