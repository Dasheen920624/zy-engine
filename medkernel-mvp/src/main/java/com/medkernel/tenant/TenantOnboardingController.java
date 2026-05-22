package com.medkernel.tenant;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户租户开通控制器
 * 提供租户申请、审批、邀请、服务账号管理等接口
 */
@RestController
@RequestMapping("/api/tenant/onboarding")
public class TenantOnboardingController {
    private final TenantOnboardingService tenantOnboardingService;
    private final OrganizationContextService organizationContextService;

    public TenantOnboardingController(TenantOnboardingService tenantOnboardingService,
                                      OrganizationContextService organizationContextService) {
        this.tenantOnboardingService = tenantOnboardingService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 提交租户申请
     */
    @PostMapping("/applications")
    public ApiResult<Map<String, Object>> submitApplication(@RequestBody Map<String, Object> body) {
        Map<String, Object> application = tenantOnboardingService.submitApplication(body);
        return ApiResult.success(application);
    }

    /**
     * 查询申请列表
     */
    @GetMapping("/applications")
    public ApiResult<List<Map<String, Object>>> listApplications(
            @RequestParam(required = false) String status) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", status);
        List<Map<String, Object>> applications = tenantOnboardingService.listApplications(filters);
        return ApiResult.success(applications);
    }

    /**
     * 审批申请
     */
    @PostMapping("/applications/{applicationCode}/review")
    public ApiResult<Map<String, Object>> reviewApplication(
            @PathVariable String applicationCode,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> application = tenantOnboardingService.reviewApplication(applicationCode, body);
        return ApiResult.success(application);
    }

    /**
     * 发送管理员邀请
     */
    @PostMapping("/invitations")
    public ApiResult<Map<String, Object>> sendInvitation(@RequestBody Map<String, Object> body) {
        Map<String, Object> invitation = tenantOnboardingService.sendInvitation(body);
        return ApiResult.success(invitation);
    }

    /**
     * 查询邀请列表
     */
    @GetMapping("/invitations")
    public ApiResult<List<Map<String, Object>>> listInvitations(
            @RequestParam String tenantId) {
        List<Map<String, Object>> invitations = tenantOnboardingService.listInvitations(tenantId);
        return ApiResult.success(invitations);
    }

    /**
     * 接受邀请
     */
    @PostMapping("/invitations/{invitationCode}/accept")
    public ApiResult<Map<String, Object>> acceptInvitation(
            @PathVariable String invitationCode,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> invitation = tenantOnboardingService.acceptInvitation(invitationCode, body);
        return ApiResult.success(invitation);
    }

    /**
     * 创建服务账号
     */
    @PostMapping("/service-accounts")
    public ApiResult<Map<String, Object>> createServiceAccount(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        body.putIfAbsent("tenantId", orgContext.getTenantId());
        body.putIfAbsent("hospitalCode", orgContext.getHospitalCode());
        Map<String, Object> account = tenantOnboardingService.createServiceAccount(body);
        return ApiResult.success(account);
    }

    /**
     * 查询服务账号列表
     */
    @GetMapping("/service-accounts")
    public ApiResult<List<Map<String, Object>>> listServiceAccounts(
            @RequestParam String tenantId) {
        List<Map<String, Object>> accounts = tenantOnboardingService.listServiceAccounts(tenantId);
        return ApiResult.success(accounts);
    }

    /**
     * 吊销服务账号
     */
    @PostMapping("/service-accounts/{accountCode}/revoke")
    public ApiResult<Map<String, Object>> revokeServiceAccount(
            @PathVariable String accountCode) {
        Map<String, Object> account = tenantOnboardingService.revokeServiceAccount(accountCode);
        return ApiResult.success(account);
    }
}