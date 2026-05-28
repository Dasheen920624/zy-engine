package com.medkernel.engine.tenant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 客户成功多维生命周期管理 REST 控制器。
 *
 * <p>提供 GET 读取及 POST 生命周期演进接口。
 */
@RestController
@RequestMapping("/api/v1/platform/success/lifecycle")
@DataScope(requireTenant = true)
public class SuccessController {

    private final TenantPilotService service;

    public SuccessController(TenantPilotService service) {
        this.service = service;
    }

    private String requireTenantId() {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw ApiException.tenantMissing();
        }
        return tenantId;
    }

    /**
     * 获取当前上下文租户的生命周期状态切片。
     *
     * @return 统一返回格式包
     */
    @GetMapping
    @PreAuthorize("@perm.has('tenant.read')")
    public ApiResult<SuccessPlan> getLifecycle() {
        String tenantId = requireTenantId();
        return ApiResult.ok(service.getSuccessPlan(tenantId));
    }

    /**
     * 推进租户的客户成功演进生命阶段，并在事务中记录变迁审计历史。
     *
     * @param request 阶段推进请求
     * @return 统一返回格式包
     */
    @PostMapping("/transition")
    @PreAuthorize("@perm.has('tenant.write')")
    public ApiResult<SuccessPlan> transitionStage(@Valid @RequestBody TransitionRequest request) {
        String tenantId = requireTenantId();
        return ApiResult.ok(service.transitionStage(tenantId, request.nextStage()));
    }

    /**
     * 演进目标状态阶段请求对象。
     */
    public record TransitionRequest(
        @NotBlank(message = "目标演进阶段不能为空")
        String nextStage
    ) {}
}
