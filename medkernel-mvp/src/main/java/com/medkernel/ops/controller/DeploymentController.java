package com.medkernel.ops.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.ops.entity.DeploymentPackage;
import com.medkernel.ops.service.DeploymentService;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 部署控制器：提供离线部署与回滚管理的 REST API
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST   /api/ops/deployments - 创建部署包</li>
 *   <li>PUT    /api/ops/deployments/{packageId} - 更新部署包</li>
 *   <li>GET    /api/ops/deployments - 查询部署包</li>
 *   <li>GET    /api/ops/deployments/{packageId} - 获取部署包详情</li>
 *   <li>POST   /api/ops/deployments/{packageId}/deploy - 执行部署</li>
 *   <li>POST   /api/ops/deployments/{packageId}/rollback - 执行回滚</li>
 *   <li>POST   /api/ops/deployments/{packageId}/pre-check - 部署前检查</li>
 *   <li>POST   /api/ops/deployments/{packageId}/post-check - 部署后检查</li>
 *   <li>GET    /api/ops/deployments/history - 部署历史</li>
 *   <li>GET    /api/ops/deployments/current - 当前部署版本</li>
 * </ul>
 */
@Tag(name = "Ops Deployment")
@RestController
@RequestMapping("/api/ops/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final OrganizationContextService organizationContextService;

    public DeploymentController(DeploymentService deploymentService,
                                OrganizationContextService organizationContextService) {
        this.deploymentService = deploymentService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 创建部署包
     */
    @Operation(summary = "Create deployment package")
    @PostMapping
    public ApiResult<DeploymentPackage> create(@RequestBody DeploymentPackage pkg,
                                                HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        pkg.setTenantId(tenantId);

        if (pkg.getPackageCode() == null || pkg.getPackageCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Package code is required");
        }
        if (pkg.getVersion() == null || pkg.getVersion().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Version is required");
        }

        DeploymentPackage created = deploymentService.createDeploymentPackage(pkg);
        return ApiResult.success(created);
    }

    /**
     * 更新部署包
     */
    @Operation(summary = "Update deployment package")
    @PutMapping("/{packageId}")
    public ApiResult<DeploymentPackage> update(@PathVariable Long packageId,
                                                @RequestBody DeploymentPackage pkg,
                                                HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        pkg.setId(packageId);
        pkg.setTenantId(tenantId);

        try {
            DeploymentPackage updated = deploymentService.updateDeploymentPackage(pkg);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 查询部署包
     */
    @Operation(summary = "List deployment packages")
    @GetMapping
    public ApiResult<List<DeploymentPackage>> list(
            @RequestParam(required = false) String targetEnvironment,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<DeploymentPackage> packages = deploymentService.listDeploymentPackages(tenantId, targetEnvironment, status);
        return ApiResult.success(packages);
    }

    /**
     * 获取部署包详情
     */
    @Operation(summary = "Get deployment package")
    @GetMapping("/{packageId}")
    public ApiResult<DeploymentPackage> get(@PathVariable Long packageId, HttpServletRequest request) {
        DeploymentPackage pkg = deploymentService.getDeploymentPackage(packageId);
        if (pkg == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Deployment package not found: " + packageId);
        }
        return ApiResult.success(pkg);
    }

    /**
     * 执行部署
     */
    @Operation(summary = "Deploy")
    @PostMapping("/{packageId}/deploy")
    public ApiResult<DeploymentPackage> deploy(@PathVariable Long packageId,
                                                @RequestBody DeployParam param,
                                                HttpServletRequest request) {
        try {
            DeploymentPackage result = deploymentService.deploy(packageId, param.getDeployedBy());
            return ApiResult.success(result);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 执行回滚
     */
    @Operation(summary = "Rollback")
    @PostMapping("/{packageId}/rollback")
    public ApiResult<DeploymentPackage> rollback(@PathVariable Long packageId,
                                                  @RequestBody RollbackParam param,
                                                  HttpServletRequest request) {
        try {
            DeploymentPackage result = deploymentService.rollback(
                    packageId, param.getRolledBackBy(), param.getRollbackReason());
            return ApiResult.success(result);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 部署前检查
     */
    @Operation(summary = "Pre-check")
    @PostMapping("/{packageId}/pre-check")
    public ApiResult<String> preCheck(@PathVariable Long packageId, HttpServletRequest request) {
        try {
            String result = deploymentService.preCheck(packageId);
            return ApiResult.success(result);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 部署后检查
     */
    @Operation(summary = "Post-check")
    @PostMapping("/{packageId}/post-check")
    public ApiResult<String> postCheck(@PathVariable Long packageId, HttpServletRequest request) {
        try {
            String result = deploymentService.postCheck(packageId);
            return ApiResult.success(result);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 部署历史
     */
    @Operation(summary = "Deployment history")
    @GetMapping("/history")
    public ApiResult<List<DeploymentPackage>> history(
            @RequestParam(required = false) String targetEnvironment,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<DeploymentPackage> packages = deploymentService.getDeploymentHistory(tenantId, targetEnvironment, limit);
        return ApiResult.success(packages);
    }

    /**
     * 当前部署版本
     */
    @Operation(summary = "Current deployment")
    @GetMapping("/current")
    public ApiResult<DeploymentPackage> current(
            @RequestParam String targetEnvironment,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        DeploymentPackage pkg = deploymentService.getCurrentDeployment(tenantId, targetEnvironment);
        if (pkg == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "No current deployment found");
        }
        return ApiResult.success(pkg);
    }

    // ==================== 请求参数类 ====================

    public static class DeployParam {
        private String deployedBy;

        public String getDeployedBy() {
            return deployedBy;
        }

        public void setDeployedBy(String deployedBy) {
            this.deployedBy = deployedBy;
        }
    }

    public static class RollbackParam {
        private String rolledBackBy;
        private String rollbackReason;

        public String getRolledBackBy() {
            return rolledBackBy;
        }

        public void setRolledBackBy(String rolledBackBy) {
            this.rolledBackBy = rolledBackBy;
        }

        public String getRollbackReason() {
            return rollbackReason;
        }

        public void setRollbackReason(String rollbackReason) {
            this.rollbackReason = rollbackReason;
        }
    }
}
