package com.medkernel.adapter;

import com.medkernel.adapter.dto.AdapterCallLogCleanupResponse;
import com.medkernel.adapter.dto.AdapterCallLogResponse;
import com.medkernel.adapter.dto.AdapterCallLogSummaryResponse;
import com.medkernel.adapter.entity.AdapterCallLogEntity;
import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 适配器执行日志控制器
 * 提供适配器调用日志查询和管理的 REST API
 */
@Tag(name = "Adapter Execution Log")
@RestController
@RequestMapping("/api/adapters/execution-logs")
public class AdapterExecutionLogController {

    private final AdapterExecutionLogService executionLogService;
    private final OrganizationContextService organizationContextService;

    public AdapterExecutionLogController(AdapterExecutionLogService executionLogService,
                                         OrganizationContextService organizationContextService) {
        this.executionLogService = executionLogService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询执行日志列表
     */
    @Operation(summary = "List adapter execution logs")
    @GetMapping
    public ApiResult<List<AdapterCallLogResponse>> listCallLogs(@Valid AdapterCallLogQueryRequest queryRequest,
                                                              HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        if (queryRequest.getAdapterCode() != null) {
            filters.put("adapterCode", queryRequest.getAdapterCode());
        }
        if (queryRequest.getQueryCode() != null) {
            filters.put("queryCode", queryRequest.getQueryCode());
        }
        if (queryRequest.getTraceId() != null) {
            filters.put("traceId", queryRequest.getTraceId());
        }
        if (queryRequest.getStatus() != null) {
            filters.put("status", queryRequest.getStatus());
        }
        if (queryRequest.getPatientId() != null) {
            filters.put("patientId", queryRequest.getPatientId());
        }
        if (queryRequest.getLimit() != null) {
            filters.put("limit", String.valueOf(queryRequest.getLimit()));
        }
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        List<AdapterCallLogEntity> logs = executionLogService.listCallLogs(filters);
        return ApiResult.success(logs.stream()
                .map(AdapterCallLogResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * 获取单条执行日志
     */
    @Operation(summary = "Get adapter execution log by traceId")
    @GetMapping("/{traceId}")
    public ApiResult<AdapterCallLogResponse> getCallLog(@PathVariable String traceId,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        AdapterCallLogEntity log = executionLogService.getCallLog(traceId);
        if (log == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "适配器执行日志不存在: " + traceId);
        }
        return ApiResult.success(AdapterCallLogResponse.fromEntity(log));
    }

    /**
     * 获取执行日志统计
     */
    @Operation(summary = "Summarize adapter execution logs")
    @GetMapping("/summary")
    public ApiResult<AdapterCallLogSummaryResponse> summarizeCallLogs(@Valid AdapterCallLogQueryRequest queryRequest,
                                                            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        if (queryRequest.getAdapterCode() != null) {
            filters.put("adapterCode", queryRequest.getAdapterCode());
        }
        if (queryRequest.getQueryCode() != null) {
            filters.put("queryCode", queryRequest.getQueryCode());
        }
        if (queryRequest.getTraceId() != null) {
            filters.put("traceId", queryRequest.getTraceId());
        }
        if (queryRequest.getStatus() != null) {
            filters.put("status", queryRequest.getStatus());
        }
        if (queryRequest.getPatientId() != null) {
            filters.put("patientId", queryRequest.getPatientId());
        }
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(AdapterCallLogSummaryResponse.fromMap(
                executionLogService.summarizeCallLogs(filters)));
    }

    /**
     * 清理旧日志
     */
    @Operation(summary = "Cleanup old adapter execution logs")
    @PostMapping("/cleanup")
    public ApiResult<AdapterCallLogCleanupResponse> cleanupOldLogs(@Valid @RequestBody AdapterCallLogCleanupRequest cleanupRequest,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        int removed = executionLogService.cleanupOldLogs(cleanupRequest.getMaxAgeHours());
        return ApiResult.success(AdapterCallLogCleanupResponse.of(removed, cleanupRequest.getMaxAgeHours()));
    }
}
