package com.medkernel.impl.service;

import com.medkernel.impl.entity.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 实施服务：客户实施、培训和试运行管理（门面模式，委托给子服务）
 */
@Service
public class ImplementationService {

    private final OnboardingChecklistService onboardingChecklistService;
    private final TrainingAndDemoService trainingAndDemoService;
    private final TrialRunService trialRunService;

    public ImplementationService(OnboardingChecklistService onboardingChecklistService,
                                 TrainingAndDemoService trainingAndDemoService,
                                 TrialRunService trialRunService) {
        this.onboardingChecklistService = onboardingChecklistService;
        this.trainingAndDemoService = trainingAndDemoService;
        this.trialRunService = trialRunService;
    }

    // ==================== 清单模板管理（委托 OnboardingChecklistService） ====================

    public OnboardingChecklistTemplate createTemplate(OnboardingChecklistTemplate template) {
        return onboardingChecklistService.createTemplate(template);
    }

    public OnboardingChecklistTemplate updateTemplate(OnboardingChecklistTemplate template) {
        return onboardingChecklistService.updateTemplate(template);
    }

    public List<OnboardingChecklistTemplate> listTemplates(Long tenantId, String phase, Boolean isActive) {
        return onboardingChecklistService.listTemplates(tenantId, phase, isActive);
    }

    public OnboardingChecklistTemplate getTemplate(Long templateId) {
        return onboardingChecklistService.getTemplate(templateId);
    }

    // ==================== 清单检查项管理（委托 OnboardingChecklistService） ====================

    public OnboardingChecklistItem addChecklistItem(OnboardingChecklistItem item) {
        return onboardingChecklistService.addChecklistItem(item);
    }

    public OnboardingChecklistItem updateChecklistItem(OnboardingChecklistItem item) {
        return onboardingChecklistService.updateChecklistItem(item);
    }

    public OnboardingChecklistItem checkItem(Long itemId, Long tenantId, boolean checked) {
        return onboardingChecklistService.checkItem(itemId, tenantId, checked);
    }

    public List<OnboardingChecklistItem> listChecklistItems(Long tenantId, Long templateId) {
        return onboardingChecklistService.listChecklistItems(tenantId, templateId);
    }

    public OnboardingChecklistItem getChecklistItem(Long itemId) {
        return onboardingChecklistService.getChecklistItem(itemId);
    }

    // ==================== 培训材料管理（委托 TrainingAndDemoService） ====================

    public TrainingMaterial createMaterial(TrainingMaterial material) {
        return trainingAndDemoService.createMaterial(material);
    }

    public TrainingMaterial updateMaterial(TrainingMaterial material) {
        return trainingAndDemoService.updateMaterial(material);
    }

    public List<TrainingMaterial> listMaterials(Long tenantId, String category, Boolean isPublished) {
        return trainingAndDemoService.listMaterials(tenantId, category, isPublished);
    }

    public TrainingMaterial getMaterial(Long materialId) {
        return trainingAndDemoService.getMaterial(materialId);
    }

    public TrainingMaterial publishMaterial(Long materialId, Long tenantId) {
        return trainingAndDemoService.publishMaterial(materialId, tenantId);
    }

    // ==================== 演示数据包管理（委托 TrainingAndDemoService） ====================

    public DemoDataPackage createDemoDataPackage(DemoDataPackage pkg) {
        return trainingAndDemoService.createDemoDataPackage(pkg);
    }

    public DemoDataPackage updateDemoDataPackage(DemoDataPackage pkg) {
        return trainingAndDemoService.updateDemoDataPackage(pkg);
    }

    public List<DemoDataPackage> listDemoDataPackages(Long tenantId, String status) {
        return trainingAndDemoService.listDemoDataPackages(tenantId, status);
    }

    public DemoDataPackage getDemoDataPackage(Long packageId) {
        return trainingAndDemoService.getDemoDataPackage(packageId);
    }

    // ==================== 试运行计划管理（委托 TrialRunService） ====================

    public TrialPlan createTrialPlan(TrialPlan plan) {
        return trialRunService.createTrialPlan(plan);
    }

    public TrialPlan updateTrialPlan(TrialPlan plan) {
        return trialRunService.updateTrialPlan(plan);
    }

    public List<TrialPlan> listTrialPlans(Long tenantId, String status, String approvalStatus) {
        return trialRunService.listTrialPlans(tenantId, status, approvalStatus);
    }

    public TrialPlan getTrialPlan(Long planId) {
        return trialRunService.getTrialPlan(planId);
    }

    public TrialPlan approvePlan(Long planId, Long tenantId, String approvedBy) {
        return trialRunService.approvePlan(planId, tenantId, approvedBy);
    }

    // ==================== 试运行记录管理（委托 TrialRunService） ====================

    public TrialRecord addTrialRecord(TrialRecord record) {
        return trialRunService.addTrialRecord(record);
    }

    public List<TrialRecord> listTrialRecords(Long tenantId, Long planId) {
        return trialRunService.listTrialRecords(tenantId, planId);
    }

    // ==================== 问题反馈管理（委托 TrialRunService） ====================

    public IssueFeedback createIssue(IssueFeedback issue) {
        return trialRunService.createIssue(issue);
    }

    public IssueFeedback updateIssue(IssueFeedback issue) {
        return trialRunService.updateIssue(issue);
    }

    public List<IssueFeedback> listIssues(Long tenantId, Long planId, String status, String severity) {
        return trialRunService.listIssues(tenantId, planId, status, severity);
    }

    public IssueFeedback resolveIssue(Long issueId, Long tenantId, String resolvedBy, String resolution) {
        return trialRunService.resolveIssue(issueId, tenantId, resolvedBy, resolution);
    }

    public IssueFeedback getIssue(Long issueId) {
        return trialRunService.getIssue(issueId);
    }
}
