package com.medkernel.engine.tenant;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.StateTransitionRecorder;

/**
 * 租户定制与客户成功生命周期服务层。
 *
 * <p>实现个性化品牌存取与 6 阶段多维生命周期的物理推进及同事务审计追溯。
 */
@Service
public class TenantPilotService {

    private final BrandingRepository brandingRepository;
    private final SuccessPlanRepository successPlanRepository;
    private final StateTransitionRecorder transitionRecorder;

    public TenantPilotService(BrandingRepository brandingRepository,
                              SuccessPlanRepository successPlanRepository,
                              StateTransitionRecorder transitionRecorder) {
        this.brandingRepository = brandingRepository;
        this.successPlanRepository = successPlanRepository;
        this.transitionRecorder = transitionRecorder;
    }

    /**
     * 获取租户定制品牌信息，不存在时自动物理落库初始化默认配置。
     *
     * @param tenantId 租户 ID
     * @return 品牌配置
     */
    @Transactional
    public Branding getBranding(String tenantId) {
        return brandingRepository.findByTenantId(tenantId)
            .orElseGet(() -> {
                Branding defaultBranding = new Branding(
                    null,
                    tenantId,
                    "MedKernel 智能示范医院",
                    "http://assets/logo-hospital-default.png",
                    "#0F172A",
                    false,
                    "{}",
                    Instant.now(),
                    currentActor(),
                    Instant.now(),
                    currentActor()
                );
                return brandingRepository.save(defaultBranding);
            });
    }

    /**
     * 保存定制品牌信息。
     *
     * @param tenantId 租户 ID
     * @param input    输入参数
     * @return 更新后的品牌配置
     */
    @Transactional
    public Branding saveBranding(String tenantId, Branding input) {
        Branding existing = brandingRepository.findByTenantId(tenantId)
            .orElse(null);

        Branding toSave;
        if (existing != null) {
            toSave = new Branding(
                existing.id(),
                tenantId,
                input.hospitalName() == null ? existing.hospitalName() : input.hospitalName(),
                input.logoUrl() == null ? existing.logoUrl() : input.logoUrl(),
                input.themeColor() == null ? existing.themeColor() : input.themeColor(),
                input.expertMode() == null ? existing.expertMode() : input.expertMode(),
                input.customBrandingJson() == null ? existing.customBrandingJson() : input.customBrandingJson(),
                existing.createdAt(),
                existing.createdBy(),
                Instant.now(),
                currentActor()
            );
        } else {
            toSave = new Branding(
                null,
                tenantId,
                input.hospitalName() == null ? "MedKernel 智能示范医院" : input.hospitalName(),
                input.logoUrl() == null ? "http://assets/logo-hospital-default.png" : input.logoUrl(),
                input.themeColor() == null ? "#0F172A" : input.themeColor(),
                input.expertMode() != null && input.expertMode(),
                input.customBrandingJson() == null ? "{}" : input.customBrandingJson(),
                Instant.now(),
                currentActor(),
                Instant.now(),
                currentActor()
            );
        }
        return brandingRepository.save(toSave);
    }

    /**
     * 获取客户成功多维生命周期计划，不存在时自动物理落库初始化首阶段。
     *
     * @param tenantId 租户 ID
     * @return 生命周期计划
     */
    @Transactional
    public SuccessPlan getSuccessPlan(String tenantId) {
        return successPlanRepository.findByTenantId(tenantId)
            .orElseGet(() -> {
                SuccessPlan defaultPlan = new SuccessPlan(
                    null,
                    tenantId,
                    "PREPARATION", // 起始准备阶段
                    80, // 初始健康得分
                    "CDSS,QC,FOLLOWUP", // 已激活模块
                    "Stroke,ChestPain", // 已激活专病包
                    Instant.now(),
                    currentActor(),
                    Instant.now(),
                    currentActor()
                );
                return successPlanRepository.save(defaultPlan);
            });
    }

    /**
     * 推进生命周期到下一阶段，并在事务中记录变迁审计历史。
     *
     * @param tenantId  租户 ID
     * @param nextStage 下一阶段
     * @return 更新后的生命周期计划
     */
    @Transactional
    public SuccessPlan transitionStage(String tenantId, String nextStage) {
        SuccessPlan plan = getSuccessPlan(tenantId);
        String currentStage = plan.currentStage();

        if (currentStage.equals(nextStage)) {
            return plan;
        }

        validateStage(nextStage);

        SuccessPlan updated = new SuccessPlan(
            plan.id(),
            tenantId,
            nextStage,
            plan.healthScore(),
            plan.activatedModules(),
            plan.activatedPathways(),
            plan.createdAt(),
            plan.createdBy(),
            Instant.now(),
            currentActor()
        );
        SuccessPlan result = successPlanRepository.save(updated);

        // 物理调用底座统一状态变迁审计组件
        transitionRecorder.record(
            "tenant_success_plan",
            tenantId,
            currentStage,
            nextStage,
            "推进租户生命周期阶段至 " + nextStage,
            null
        );

        return result;
    }

    private void validateStage(String stage) {
        switch (stage) {
            case "PREPARATION":
            case "PILOT":
            case "ACCEPTANCE":
            case "PROMOTION":
            case "RUNNING":
            case "RENEWAL":
                break;
            default:
                throw new ApiException(ErrorCode.BAD_REQUEST, "非法的生命周期阶段名称: " + stage);
        }
    }

    private String currentActor() {
        return RequestContext.currentUserId()
            .filter(s -> !s.isBlank())
            .orElse("system");
    }
}
