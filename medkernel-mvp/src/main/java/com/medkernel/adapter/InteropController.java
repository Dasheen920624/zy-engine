package com.medkernel.adapter;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 互联互通标准适配器REST接口
 * 暴露HL7 v2、FHIR、CDA、IHE、CDS Hooks、SMART on FHIR、DICOM等标准的查询接口
 */
@RestController
@RequestMapping("/api/interop")
public class InteropController {
    private final InteropAdapterService interopAdapterService;
    private final OrganizationContextService organizationContextService;

    public InteropController(InteropAdapterService interopAdapterService,
                             OrganizationContextService organizationContextService) {
        this.interopAdapterService = interopAdapterService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询互联互通适配器（HL7/FHIR/CDA/DICOM/IHE/REST）
     */
    @PostMapping("/query")
    public ApiResult<Map<String, Object>> query(@RequestBody Map<String, Object> request,
                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(interopAdapterService.query(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 查询CDS Hooks服务
     */
    @PostMapping("/cds-hooks")
    public ApiResult<Map<String, Object>> queryCdsHooks(@RequestBody Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(interopAdapterService.queryCdsHooks(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 查询SMART on FHIR应用
     */
    @PostMapping("/smart-apps")
    public ApiResult<Map<String, Object>> querySmartApp(@RequestBody Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(interopAdapterService.querySmartApp(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 列出所有互联互通适配器定义
     */
    @GetMapping("/adapters")
    public ApiResult<List<Map<String, Object>>> listInteropAdapters(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(interopAdapterService.listInteropAdapters(orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 列出所有CDS Hooks服务定义
     */
    @GetMapping("/cds-hooks")
    public ApiResult<List<Map<String, Object>>> listCdsHooksServices(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(interopAdapterService.listCdsHooksServices(orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    /**
     * 列出所有SMART on FHIR应用定义
     */
    @GetMapping("/smart-apps")
    public ApiResult<List<Map<String, Object>>> listSmartApps(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(interopAdapterService.listSmartApps(orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }
}
