package com.medkernel.rule;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.common.TraceContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 规则动作日志服务。
 * <p>
 * 记录医嘱安全拦截等场景下的决策内容、决策人、理由、知情同意等信息，
 * 供药师审方等下游角色查看医生的原始决策。
 * </p>
 *
 * <p>后续可接 Oracle/达梦持久化（RULE_ACTION_LOG 表），当前先用内存环形缓冲。</p>
 */
@Service
public class RuleActionLogService {

    private static final int LOG_RING_CAPACITY = 1000;
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** 按 actionLogId 索引 */
    private final Map<String, Map<String, Object>> logById = new ConcurrentHashMap<>();
    /** 按时间排序的环形缓冲 */
    private final Deque<Map<String, Object>> logRing = new ConcurrentLinkedDeque<>();

    /**
     * 创建一条规则动作日志。
     *
     * @param ruleCode       命中规则编码
     * @param ruleName       命中规则名称
     * @param actionMode     动作模式（NOTICE/SOFT/BLOCK）
     * @param patientId      患者 ID
     * @param encounterId    就诊 ID
     * @param orderCode      医嘱编码
     * @param orderName      医嘱名称
     * @param doctorId       医生 ID
     * @param doctorName     医生姓名
     * @param decision       决策（CANCEL/MODIFY/INSIST）
     * @param reason         理由（≥20 字，BLOCK 模式必填）
     * @param informedConsent 是否已知情同意
     * @param orgContext      组织上下文
     * @return 创建的日志条目
     */
    public Map<String, Object> createLog(
            String ruleCode,
            String ruleName,
            ActionMode actionMode,
            String patientId,
            String encounterId,
            String orderCode,
            String orderName,
            String doctorId,
            String doctorName,
            String decision,
            String reason,
            boolean informedConsent,
            OrganizationContext orgContext) {

        String actionLogId = "RAL-" + UUID.randomUUID().toString().substring(0, 8);
        String now = OffsetDateTime.now().format(ISO_FMT);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("action_log_id", actionLogId);
        log.put("rule_code", ruleCode);
        log.put("rule_name", ruleName);
        log.put("action_mode", actionMode.name());
        log.put("patient_id", patientId);
        log.put("encounter_id", encounterId);
        log.put("order_code", orderCode);
        log.put("order_name", orderName);
        log.put("doctor_id", doctorId);
        log.put("doctor_name", doctorName);
        log.put("decision", decision);
        log.put("reason", reason);
        log.put("informed_consent", informedConsent);
        log.put("created_time", now);
        log.put("trace_id", TraceContext.getTraceId());

        // 组织信息
        if (orgContext != null) {
            log.put("tenant_id", orgContext.getTenantId());
            log.put("hospital_code", orgContext.getHospitalCode());
            log.put("department_code", orgContext.getDepartmentCode());
        }

        logById.put(actionLogId, log);
        logRing.addFirst(log);

        // 环形缓冲淘汰
        while (logRing.size() > LOG_RING_CAPACITY) {
            Map<String, Object> evicted = logRing.removeLast();
            if (evicted != null) {
                logById.remove(evicted.get("action_log_id"));
            }
        }

        return log;
    }

    /**
     * 根据 actionLogId 查询单条日志。
     */
    public Map<String, Object> getLog(String actionLogId) {
        return logById.get(actionLogId);
    }

    /**
     * 按条件查询日志列表。
     *
     * @param patientId   患者 ID（可选）
     * @param doctorId    医生 ID（可选）
     * @param ruleCode    规则编码（可选）
     * @param actionMode  动作模式（可选）
     * @param decision    决策（可选）
     * @param limit       最大返回条数
     * @return 日志列表
     */
    public List<Map<String, Object>> listLogs(
            String patientId,
            String doctorId,
            String ruleCode,
            String actionMode,
            String decision,
            int limit) {

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> log : logRing) {
            if (result.size() >= limit) {
                break;
            }
            if (patientId != null && !patientId.equals(log.get("patient_id"))) {
                continue;
            }
            if (doctorId != null && !doctorId.equals(log.get("doctor_id"))) {
                continue;
            }
            if (ruleCode != null && !ruleCode.equals(log.get("rule_code"))) {
                continue;
            }
            if (actionMode != null && !actionMode.equalsIgnoreCase((String) log.get("action_mode"))) {
                continue;
            }
            if (decision != null && !decision.equalsIgnoreCase((String) log.get("decision"))) {
                continue;
            }
            result.add(log);
        }
        return result;
    }
}
