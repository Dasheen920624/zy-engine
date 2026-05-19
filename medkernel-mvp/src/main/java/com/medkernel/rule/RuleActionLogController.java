package com.medkernel.rule;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 规则动作日志 API。
 * <p>
 * 记录医嘱安全拦截（BLOCK 模式）下的医生决策、理由和知情同意信息，
 * 供药师审方等下游角色查看。
 * </p>
 */
@RestController
@RequestMapping("/api/rule-action-logs")
public class RuleActionLogController {

    private final RuleActionLogService actionLogService;
    private final OrganizationContextService organizationContextService;

    public RuleActionLogController(RuleActionLogService actionLogService,
                                    OrganizationContextService organizationContextService) {
        this.actionLogService = actionLogService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 创建规则动作日志。
     * <p>
     * 请求体字段：
     * <ul>
     *   <li>{@code rule_code} - 命中规则编码（必填）</li>
     *   <li>{@code rule_name} - 命中规则名称（可选）</li>
     *   <li>{@code action_mode} - 动作模式 NOTICE/SOFT/BLOCK（必填）</li>
     *   <li>{@code patient_id} - 患者 ID（必填）</li>
     *   <li>{@code encounter_id} - 就诊 ID（可选）</li>
     *   <li>{@code order_code} - 医嘱编码（必填）</li>
     *   <li>{@code order_name} - 医嘱名称（可选）</li>
     *   <li>{@code doctor_id} - 医生 ID（必填）</li>
     *   <li>{@code doctor_name} - 医生姓名（可选）</li>
     *   <li>{@code decision} - 决策 CANCEL/MODIFY/INSIST（必填）</li>
     *   <li>{@code reason} - 理由（BLOCK 模式必填，≥20 字）</li>
     *   <li>{@code informed_consent} - 是否已知情同意（BLOCK 模式必填）</li>
     * </ul>
     */
    @PostMapping
    public ApiResult<Map<String, Object>> createLog(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        // 参数提取与校验
        String ruleCode = (String) request.get("rule_code");
        String ruleName = (String) request.get("rule_name");
        String actionModeStr = (String) request.get("action_mode");
        String patientId = (String) request.get("patient_id");
        String encounterId = (String) request.get("encounter_id");
        String orderCode = (String) request.get("order_code");
        String orderName = (String) request.get("order_name");
        String doctorId = (String) request.get("doctor_id");
        String doctorName = (String) request.get("doctor_name");
        String decision = (String) request.get("decision");
        String reason = (String) request.get("reason");
        Object consentObj = request.get("informed_consent");
        boolean informedConsent = consentObj instanceof Boolean ? (Boolean) consentObj : false;

        // 必填校验
        if (ruleCode == null || ruleCode.isEmpty()) {
            return ApiResult.error("VALIDATION_ERROR", "rule_code 不能为空");
        }
        if (actionModeStr == null || !ActionMode.isValid(actionModeStr)) {
            return ApiResult.error("VALIDATION_ERROR", "action_mode 必须为 NOTICE/SOFT/BLOCK");
        }
        if (patientId == null || patientId.isEmpty()) {
            return ApiResult.error("VALIDATION_ERROR", "patient_id 不能为空");
        }
        if (orderCode == null || orderCode.isEmpty()) {
            return ApiResult.error("VALIDATION_ERROR", "order_code 不能为空");
        }
        if (doctorId == null || doctorId.isEmpty()) {
            return ApiResult.error("VALIDATION_ERROR", "doctor_id 不能为空");
        }
        if (decision == null || decision.isEmpty()) {
            return ApiResult.error("VALIDATION_ERROR", "decision 不能为空");
        }

        ActionMode actionMode = ActionMode.parse(actionModeStr);

        // BLOCK 模式额外校验
        if (actionMode == ActionMode.BLOCK) {
            if (reason == null || reason.trim().length() < 20) {
                return ApiResult.error("VALIDATION_ERROR", "BLOCK 模式下 reason 不能为空且不少于 20 字");
            }
            if (!informedConsent) {
                return ApiResult.error("VALIDATION_ERROR", "BLOCK 模式下必须确认知情同意");
            }
        }

        Map<String, Object> log = actionLogService.createLog(
                ruleCode, ruleName, actionMode,
                patientId, encounterId,
                orderCode, orderName,
                doctorId, doctorName,
                decision, reason, informedConsent,
                orgContext);

        return ApiResult.success(log);
    }

    /**
     * 查询单条日志详情。
     */
    @GetMapping("/{actionLogId}")
    public ApiResult<Map<String, Object>> getLog(@PathVariable String actionLogId) {
        Map<String, Object> log = actionLogService.getLog(actionLogId);
        if (log == null) {
            return ApiResult.error("NOT_FOUND", "日志不存在: " + actionLogId);
        }
        return ApiResult.success(log);
    }

    /**
     * 查询日志列表。
     */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listLogs(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String actionMode,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false, defaultValue = "50") String limit) {

        int limitInt;
        try {
            limitInt = Math.min(Math.max(Integer.parseInt(limit), 1), 200);
        } catch (NumberFormatException e) {
            limitInt = 50;
        }

        List<Map<String, Object>> logs = actionLogService.listLogs(
                patientId, doctorId, ruleCode, actionMode, decision, limitInt);

        return ApiResult.success(logs);
    }
}
