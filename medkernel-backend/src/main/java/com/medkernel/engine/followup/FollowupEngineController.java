package com.medkernel.engine.followup;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.datascope.DataScope;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 随访引擎控制器 (GA-ENG-API-09)。
 *
 * <p>提供随访计划智能生成、分期任务调度、问卷回收提交、以及临床异常事件上报等 REST 服务接口。
 * 全线受 {@link DataScope} 与权限切面拦截，确保多租户数据严格物理隔离。
 */
@RestController
@RequestMapping("/api/v1/engine/followup")
@DataScope(requireTenant = true)
public class FollowupEngineController {

    private final FollowupEngineService service;

    public FollowupEngineController(FollowupEngineService service) {
        this.service = service;
    }

    /**
     * 智能生成随访计划。
     *
     * @param request 随访计划生成请求，包含患者、就诊、路径、病种与风险分层等数据
     * @return 随访计划详情及生成的下属随访任务列表
     */
    @PostMapping("/plans/generate")
    @PreAuthorize("@perm.has('followup.write')")
    public ApiResult<FollowupPlanDetailResponse> generatePlan(@Valid @RequestBody FollowupPlanGenerateRequest request) {
        return ApiResult.ok(service.generatePlan(request));
    }

    /**
     * 查询随访计划详情。
     *
     * @param planId 随访计划业务唯一 ID
     * @return 随访计划详情及任务状态列表
     */
    @GetMapping("/plans/{planId}")
    @PreAuthorize("@perm.has('followup.read')")
    public ApiResult<FollowupPlanDetailResponse> getPlanDetail(@PathVariable String planId) {
        return ApiResult.ok(service.getPlanDetail(planId));
    }

    /**
     * 提交患者出院随访问卷数据，并自动标记对应随访任务为已完成。
     *
     * @param taskId  随访任务业务唯一 ID
     * @param request 问卷提交请求，包含表单 JSON 数据与执行人信息
     * @return 空响应
     */
    @PostMapping("/tasks/{taskId}/questionnaires")
    @PreAuthorize("@perm.has('followup.write')")
    public ApiResult<Void> submitQuestionnaire(
            @PathVariable String taskId,
            @Valid @RequestBody FollowupQuestionnaireSubmitRequest request) {
        service.submitQuestionnaire(taskId, request);
        return ApiResult.empty();
    }

    /**
     * 上报临床异常事件或异常回流入院监控。
     *
     * @param request 异常上报请求，包含计划 ID、事件类型、事件详细载荷等
     * @return 空响应
     */
    @PostMapping("/events/report-abnormal")
    @PreAuthorize("@perm.has('followup.write')")
    public ApiResult<Void> reportAbnormal(@Valid @RequestBody FollowupAbnormalReportRequest request) {
        service.reportAbnormal(request);
        return ApiResult.empty();
    }
}
