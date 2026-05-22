package com.medkernel.impl.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.impl.entity.*;
import com.medkernel.impl.service.ImplementationService;
import com.medkernel.organization.OrganizationContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 实施控制器：客户实施、培训和试运行管理 REST API
 */
@Tag(name = "Implementation Onboarding")
@RestController
@RequestMapping("/api/impl")
public class ImplementationController {

    private final ImplementationService implementationService;
    private final OrganizationContextService organizationContextService;

    public ImplementationController(ImplementationService implementationService,
                                    OrganizationContextService organizationContextService) {
        this.implementationService = implementationService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 清单模板 ====================

    @Operation(summary = "Create checklist template")
    @PostMapping("/checklist-templates")
    public ApiResult<OnboardingChecklistTemplate> createTemplate(
            @RequestBody OnboardingChecklistTemplate template, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        template.setTenantId(tenantId);
        if (template.getTemplateCode() == null || template.getTemplateCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Template code is required");
        }
        if (template.getTemplateName() == null || template.getTemplateName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Template name is required");
        }
        OnboardingChecklistTemplate created = implementationService.createTemplate(template);
        return ApiResult.success(created);
    }

    @Operation(summary = "Update checklist template")
    @PutMapping("/checklist-templates/{templateId}")
    public ApiResult<OnboardingChecklistTemplate> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody OnboardingChecklistTemplate template, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        template.setId(templateId);
        template.setTenantId(tenantId);
        OnboardingChecklistTemplate updated = implementationService.updateTemplate(template);
        return ApiResult.success(updated);
    }

    @Operation(summary = "List checklist templates")
    @GetMapping("/checklist-templates")
    public ApiResult<List<OnboardingChecklistTemplate>> listTemplates(
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) Boolean isActive,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<OnboardingChecklistTemplate> templates = implementationService.listTemplates(tenantId, phase, isActive);
        return ApiResult.success(templates);
    }

    @Operation(summary = "Get checklist template")
    @GetMapping("/checklist-templates/{templateId}")
    public ApiResult<OnboardingChecklistTemplate> getTemplate(
            @PathVariable Long templateId, HttpServletRequest request) {
        OnboardingChecklistTemplate template = implementationService.getTemplate(templateId);
        if (template == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Checklist template not found: " + templateId);
        }
        return ApiResult.success(template);
    }

    // ==================== 清单检查项 ====================

    @Operation(summary = "Add checklist item")
    @PostMapping("/checklist-templates/{templateId}/items")
    public ApiResult<OnboardingChecklistItem> addChecklistItem(
            @PathVariable Long templateId,
            @RequestBody OnboardingChecklistItem item, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        item.setTenantId(tenantId);
        item.setTemplateId(templateId);
        if (item.getItemCode() == null || item.getItemCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Item code is required");
        }
        if (item.getItemName() == null || item.getItemName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Item name is required");
        }
        OnboardingChecklistItem created = implementationService.addChecklistItem(item);
        return ApiResult.success(created);
    }

    @Operation(summary = "Update checklist item")
    @PutMapping("/checklist-items/{itemId}")
    public ApiResult<OnboardingChecklistItem> updateChecklistItem(
            @PathVariable Long itemId,
            @RequestBody OnboardingChecklistItem item, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        item.setId(itemId);
        item.setTenantId(tenantId);
        OnboardingChecklistItem updated = implementationService.updateChecklistItem(item);
        return ApiResult.success(updated);
    }

    @Operation(summary = "Check/uncheck checklist item")
    @PostMapping("/checklist-items/{itemId}/check")
    public ApiResult<OnboardingChecklistItem> checkItem(
            @PathVariable Long itemId,
            @RequestBody CheckItemParam param, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        OnboardingChecklistItem checked = implementationService.checkItem(itemId, tenantId, param.isChecked());
        return ApiResult.success(checked);
    }

    // ==================== 培训材料 ====================

    @Operation(summary = "Create training material")
    @PostMapping("/training-materials")
    public ApiResult<TrainingMaterial> createMaterial(
            @RequestBody TrainingMaterial material, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        material.setTenantId(tenantId);
        if (material.getMaterialCode() == null || material.getMaterialCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Material code is required");
        }
        if (material.getMaterialName() == null || material.getMaterialName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Material name is required");
        }
        TrainingMaterial created = implementationService.createMaterial(material);
        return ApiResult.success(created);
    }

    @Operation(summary = "Update training material")
    @PutMapping("/training-materials/{materialId}")
    public ApiResult<TrainingMaterial> updateMaterial(
            @PathVariable Long materialId,
            @RequestBody TrainingMaterial material, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        material.setId(materialId);
        material.setTenantId(tenantId);
        TrainingMaterial updated = implementationService.updateMaterial(material);
        return ApiResult.success(updated);
    }

    @Operation(summary = "List training materials")
    @GetMapping("/training-materials")
    public ApiResult<List<TrainingMaterial>> listMaterials(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isPublished,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<TrainingMaterial> materials = implementationService.listMaterials(tenantId, category, isPublished);
        return ApiResult.success(materials);
    }

    @Operation(summary = "Publish training material")
    @PostMapping("/training-materials/{materialId}/publish")
    public ApiResult<TrainingMaterial> publishMaterial(
            @PathVariable Long materialId, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        TrainingMaterial published = implementationService.publishMaterial(materialId, tenantId);
        return ApiResult.success(published);
    }

    // ==================== 演示数据包 ====================

    @Operation(summary = "Create demo data package")
    @PostMapping("/demo-data-packages")
    public ApiResult<DemoDataPackage> createDemoDataPackage(
            @RequestBody DemoDataPackage pkg, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        pkg.setTenantId(tenantId);
        if (pkg.getPackageCode() == null || pkg.getPackageCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Package code is required");
        }
        if (pkg.getPackageName() == null || pkg.getPackageName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Package name is required");
        }
        DemoDataPackage created = implementationService.createDemoDataPackage(pkg);
        return ApiResult.success(created);
    }

    @Operation(summary = "List demo data packages")
    @GetMapping("/demo-data-packages")
    public ApiResult<List<DemoDataPackage>> listDemoDataPackages(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<DemoDataPackage> packages = implementationService.listDemoDataPackages(tenantId, status);
        return ApiResult.success(packages);
    }

    @Operation(summary = "Get demo data package")
    @GetMapping("/demo-data-packages/{packageId}")
    public ApiResult<DemoDataPackage> getDemoDataPackage(
            @PathVariable Long packageId, HttpServletRequest request) {
        DemoDataPackage pkg = implementationService.getDemoDataPackage(packageId);
        if (pkg == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Demo data package not found: " + packageId);
        }
        return ApiResult.success(pkg);
    }

    // ==================== 试运行计划 ====================

    @Operation(summary = "Create trial plan")
    @PostMapping("/trial-plans")
    public ApiResult<TrialPlan> createTrialPlan(
            @RequestBody TrialPlan plan, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        plan.setTenantId(tenantId);
        if (plan.getPlanCode() == null || plan.getPlanCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Plan code is required");
        }
        if (plan.getPlanName() == null || plan.getPlanName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Plan name is required");
        }
        TrialPlan created = implementationService.createTrialPlan(plan);
        return ApiResult.success(created);
    }

    @Operation(summary = "Update trial plan")
    @PutMapping("/trial-plans/{planId}")
    public ApiResult<TrialPlan> updateTrialPlan(
            @PathVariable Long planId,
            @RequestBody TrialPlan plan, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        plan.setId(planId);
        plan.setTenantId(tenantId);
        TrialPlan updated = implementationService.updateTrialPlan(plan);
        return ApiResult.success(updated);
    }

    @Operation(summary = "List trial plans")
    @GetMapping("/trial-plans")
    public ApiResult<List<TrialPlan>> listTrialPlans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<TrialPlan> plans = implementationService.listTrialPlans(tenantId, status, approvalStatus);
        return ApiResult.success(plans);
    }

    @Operation(summary = "Approve trial plan")
    @PostMapping("/trial-plans/{planId}/approve")
    public ApiResult<TrialPlan> approvePlan(
            @PathVariable Long planId,
            @RequestBody ApproveParam param, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        TrialPlan approved = implementationService.approvePlan(planId, tenantId, param.getApprovedBy());
        return ApiResult.success(approved);
    }

    // ==================== 试运行记录 ====================

    @Operation(summary = "Add trial record")
    @PostMapping("/trial-plans/{planId}/records")
    public ApiResult<TrialRecord> addTrialRecord(
            @PathVariable Long planId,
            @RequestBody TrialRecord record, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        record.setTenantId(tenantId);
        record.setPlanId(planId);
        TrialRecord created = implementationService.addTrialRecord(record);
        return ApiResult.success(created);
    }

    @Operation(summary = "List trial records")
    @GetMapping("/trial-plans/{planId}/records")
    public ApiResult<List<TrialRecord>> listTrialRecords(
            @PathVariable Long planId, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<TrialRecord> records = implementationService.listTrialRecords(tenantId, planId);
        return ApiResult.success(records);
    }

    // ==================== 问题反馈 ====================

    @Operation(summary = "Create issue feedback")
    @PostMapping("/issues")
    public ApiResult<IssueFeedback> createIssue(
            @RequestBody IssueFeedback issue, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        issue.setTenantId(tenantId);
        if (issue.getIssueCode() == null || issue.getIssueCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Issue code is required");
        }
        if (issue.getTitle() == null || issue.getTitle().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Title is required");
        }
        IssueFeedback created = implementationService.createIssue(issue);
        return ApiResult.success(created);
    }

    @Operation(summary = "Update issue feedback")
    @PutMapping("/issues/{issueId}")
    public ApiResult<IssueFeedback> updateIssue(
            @PathVariable Long issueId,
            @RequestBody IssueFeedback issue, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        issue.setId(issueId);
        issue.setTenantId(tenantId);
        IssueFeedback updated = implementationService.updateIssue(issue);
        return ApiResult.success(updated);
    }

    @Operation(summary = "List issue feedbacks")
    @GetMapping("/issues")
    public ApiResult<List<IssueFeedback>> listIssues(
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        List<IssueFeedback> issues = implementationService.listIssues(tenantId, planId, status, severity);
        return ApiResult.success(issues);
    }

    @Operation(summary = "Resolve issue feedback")
    @PostMapping("/issues/{issueId}/resolve")
    public ApiResult<IssueFeedback> resolveIssue(
            @PathVariable Long issueId,
            @RequestBody ResolveIssueParam param, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        IssueFeedback resolved = implementationService.resolveIssue(
                issueId, tenantId, param.getResolvedBy(), param.getResolution());
        return ApiResult.success(resolved);
    }

    // ==================== 请求参数类 ====================

    public static class CheckItemParam {
        private boolean checked;

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    public static class ApproveParam {
        private String approvedBy;

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
    }

    public static class ResolveIssueParam {
        private String resolvedBy;
        private String resolution;

        public String getResolvedBy() {
            return resolvedBy;
        }

        public void setResolvedBy(String resolvedBy) {
            this.resolvedBy = resolvedBy;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }
    }
}
