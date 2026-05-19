package com.medkernel.rule;

import com.medkernel.common.TraceContext;
import com.medkernel.organization.OrganizationContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 规则动作日志服务
 * 记录和查询规则命中后的用户决策
 */
@Service
public class RuleActionLogService {
    private static final int LOG_RING_CAPACITY = 1000;
    
    private final Deque<RuleActionLog> actionLogs = new ConcurrentLinkedDeque<>();
    private final AtomicLong logSequence = new AtomicLong();

    /**
     * 记录用户决策
     * @param request 决策请求
     * @param orgContext 组织上下文
     * @return 记录的日志
     */
    public RuleActionLog recordDecision(Map<String, Object> request, OrganizationContext orgContext) {
        RuleActionLog log = new RuleActionLog();
        log.setLogId("RAL-" + logSequence.incrementAndGet());
        log.setRuleCode(string(request.get("rule_code")));
        log.setRuleVersion(string(request.get("rule_version")));
        log.setPatientId(string(request.get("patient_id")));
        log.setEncounterId(string(request.get("encounter_id")));
        log.setOrderId(string(request.get("order_id")));
        log.setActionMode(parseActionMode(string(request.get("action_mode"))));
        log.setDecision(string(request.get("decision")));
        log.setDecisionBy(string(request.get("decision_by")));
        log.setDecisionTime(OffsetDateTime.now());
        log.setReason(string(request.get("reason")));
        log.setInformedConsent(booleanValue(request.get("informed_consent")));
        log.setFamilyNotified(booleanValue(request.get("family_notified")));
        log.setTraceId(TraceContext.getTraceId());
        
        // 设置组织上下文
        if (orgContext != null) {
            log.setTenantId(orgContext.getTenantId());
            log.setGroupCode(orgContext.getGroupCode());
            log.setHospitalCode(orgContext.getHospitalCode());
            log.setCampusCode(orgContext.getCampusCode());
            log.setSiteCode(orgContext.getSiteCode());
            log.setDepartmentCode(orgContext.getDepartmentCode());
            log.setScopeLevel(orgContext.getScopeLevel());
            log.setScopeCode(orgContext.getScopeCode());
            log.setOrgSource(orgContext.getOrgSource());
        }
        
        log.setCreatedTime(OffsetDateTime.now());
        
        // 环形缓冲存储
        actionLogs.addFirst(log);
        while (actionLogs.size() > LOG_RING_CAPACITY) {
            actionLogs.removeLast();
        }
        
        return log;
    }

    /**
     * 根据患者和就诊记录查询决策日志
     */
    public List<RuleActionLog> queryByPatient(String patientId, String encounterId) {
        List<RuleActionLog> result = new ArrayList<>();
        for (RuleActionLog log : actionLogs) {
            if (patientId.equals(log.getPatientId()) && 
                (encounterId == null || encounterId.equals(log.getEncounterId()))) {
                result.add(log);
            }
        }
        return result;
    }

    /**
     * 根据规则查询决策日志
     */
    public List<RuleActionLog> queryByRule(String ruleCode, String ruleVersion) {
        List<RuleActionLog> result = new ArrayList<>();
        for (RuleActionLog log : actionLogs) {
            if (ruleCode.equals(log.getRuleCode()) && 
                (ruleVersion == null || ruleVersion.equals(log.getRuleVersion()))) {
                result.add(log);
            }
        }
        return result;
    }

    /**
     * 根据订单查询决策日志
     */
    public List<RuleActionLog> queryByOrder(String orderId) {
        List<RuleActionLog> result = new ArrayList<>();
        for (RuleActionLog log : actionLogs) {
            if (orderId.equals(log.getOrderId())) {
                result.add(log);
            }
        }
        return result;
    }

    /**
     * 查询所有决策日志
     */
    public List<RuleActionLog> listAll(Map<String, String> filters) {
        List<RuleActionLog> result = new ArrayList<>();
        int limit = 100;
        if (filters.containsKey("limit")) {
            try {
                limit = Integer.parseInt(filters.get("limit"));
            } catch (NumberFormatException ignored) {
            }
        }
        
        int count = 0;
        for (RuleActionLog log : actionLogs) {
            if (count >= limit) break;
            
            boolean matches = true;
            if (filters.containsKey("patient_id") && !filters.get("patient_id").equals(log.getPatientId())) {
                matches = false;
            }
            if (filters.containsKey("encounter_id") && !filters.get("encounter_id").equals(log.getEncounterId())) {
                matches = false;
            }
            if (filters.containsKey("rule_code") && !filters.get("rule_code").equals(log.getRuleCode())) {
                matches = false;
            }
            if (filters.containsKey("decision") && !filters.get("decision").equals(log.getDecision())) {
                matches = false;
            }
            if (filters.containsKey("decision_by") && !filters.get("decision_by").equals(log.getDecisionBy())) {
                matches = false;
            }
            
            if (matches) {
                result.add(log);
                count++;
            }
        }
        return result;
    }

    /**
     * 根据ID获取决策日志
     */
    public RuleActionLog getById(String logId) {
        for (RuleActionLog log : actionLogs) {
            if (logId.equals(log.getLogId())) {
                return log;
            }
        }
        return null;
    }

    private ActionMode parseActionMode(String mode) {
        if (mode == null) return ActionMode.NOTICE;
        try {
            return ActionMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActionMode.NOTICE;
        }
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean booleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString());
    }
}