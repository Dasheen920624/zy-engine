package com.medkernel.patientindex.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.patientindex.service.MpiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 患者主索引（MPI）REST 接口
 * 提供患者标识管理、就诊标识映射和冲突处理接口
 */
@RestController
@RequestMapping("/api/mpi")
public class MpiController {
    private final MpiService mpiService;
    private final OrganizationContextService organizationContextService;

    public MpiController(MpiService mpiService, OrganizationContextService organizationContextService) {
        this.mpiService = mpiService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 创建或更新患者主索引
     */
    @PostMapping("/patients")
    public ApiResult<Map<String, Object>> createOrUpdatePatient(@RequestBody Map<String, Object> request,
                                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(mpiService.createOrUpdatePatient(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 查询患者主索引
     */
    @PostMapping("/patients/query")
    public ApiResult<Map<String, Object>> queryPatient(@RequestBody Map<String, Object> request,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(mpiService.queryPatient(request, orgCtx.getTenantId()));
    }

    /**
     * 列出所有患者主索引
     */
    @GetMapping("/patients")
    public ApiResult<List<Map<String, Object>>> listPatients(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(mpiService.listPatients(orgCtx.getTenantId()));
    }

    /**
     * 添加标识映射
     */
    @PostMapping("/identifiers")
    public ApiResult<Map<String, Object>> addIdentifierMapping(@RequestBody Map<String, Object> request,
                                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(mpiService.addIdentifierMapping(request, orgCtx.getTenantId()));
    }

    /**
     * 创建就诊记录
     */
    @PostMapping("/encounters")
    public ApiResult<Map<String, Object>> createEncounter(@RequestBody Map<String, Object> request,
                                                           HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(mpiService.createEncounter(request, orgCtx.getTenantId()));
    }

    /**
     * 处理标识冲突
     */
    @PostMapping("/conflicts/resolve")
    public ApiResult<Map<String, Object>> resolveConflict(@RequestBody Map<String, Object> request,
                                                            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(mpiService.resolveConflict(request, orgCtx.getTenantId()));
    }

    /**
     * 列出待处理的标识冲突
     */
    @GetMapping("/conflicts")
    public ApiResult<List<Map<String, Object>>> listPendingConflicts(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(mpiService.listPendingConflicts(orgCtx.getTenantId()));
    }
}
