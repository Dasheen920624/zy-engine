package com.medkernel.security;

import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SEC-005: 角色责任链审计服务。
 * <p>
 * 记录关键业务操作（起草、审核、发布、回滚、复核、告警处理）的责任人链，
 * 确保每个环节的操作者可追溯。
 */
@Service
public class ResponsibilityChainService {

    private static final Logger log = LoggerFactory.getLogger(ResponsibilityChainService.class);

    private final EnginePersistenceService persistenceService;

    private final Map<String, ResponsibilityChain> chainStore = new ConcurrentHashMap<String, ResponsibilityChain>();

    public ResponsibilityChainService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * 记录责任链操作。
     *
     * @param assetType  资产类型（RULE/PATHWAY/CONFIG_PACKAGE/ADAPTER/GRAPH/QC_METRIC）
     * @param assetCode  资产编码
     * @param action     操作类型（DRAFT/REVIEW/PUBLISH/ROLLBACK/REVERIFY/ALERT_HANDLE）
     * @param operatorId 操作人ID
     * @param tenantId   租户ID
     * @param detail     操作详情
     */
    public void recordAction(String assetType, String assetCode, String action,
                             String operatorId, String tenantId, Map<String, Object> detail) {
        String chainId = chainKey(assetType, assetCode, tenantId);
        ResponsibilityChain chain = chainStore.computeIfAbsent(chainId, k -> {
            ResponsibilityChain c = new ResponsibilityChain();
            c.setAssetType(assetType);
            c.setAssetCode(assetCode);
            c.setTenantId(tenantId);
            return c;
        });

        ResponsibilityAction ra = new ResponsibilityAction();
        ra.setAction(action);
        ra.setOperatorId(operatorId);
        ra.setTimestamp(LocalDateTime.now());
        ra.setDetail(detail != null ? detail : Collections.<String, Object>emptyMap());

        chain.getActions().add(ra);

        // 持久化审计日志
        try {
            Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
            auditDetail.put("asset_type", assetType);
            auditDetail.put("asset_code", assetCode);
            auditDetail.put("action", action);
            auditDetail.put("operator_id", operatorId);
            auditDetail.put("chain_length", chain.getActions().size());
            if (detail != null) {
                auditDetail.putAll(detail);
            }
            persistenceService.saveAuditLog("RESPONSIBILITY_CHAIN", action, assetType,
                    assetCode, null, null, operatorId, auditDetail);
        } catch (RuntimeException ex) {
            log.warn("[SEC-005] Responsibility chain audit log persistence failed: {}", ex.getMessage());
        }

        log.info("[SEC-005] Responsibility chain: {}:{}:{} action={} operator={} chain_length={}",
                assetType, assetCode, tenantId, action, operatorId, chain.getActions().size());
    }

    /**
     * 查询责任链。
     */
    public Map<String, Object> getChain(String assetType, String assetCode, String tenantId) {
        String chainId = chainKey(assetType, assetCode, tenantId);
        ResponsibilityChain chain = chainStore.get(chainId);
        if (chain == null) {
            Map<String, Object> empty = new LinkedHashMap<String, Object>();
            empty.put("asset_type", assetType);
            empty.put("asset_code", assetCode);
            empty.put("tenant_id", tenantId);
            empty.put("actions", Collections.emptyList());
            empty.put("total_actions", 0);
            return empty;
        }
        return chain.toMap();
    }

    /**
     * 查询资产的所有操作者。
     */
    public List<String> getOperators(String assetType, String assetCode, String tenantId) {
        String chainId = chainKey(assetType, assetCode, tenantId);
        ResponsibilityChain chain = chainStore.get(chainId);
        if (chain == null) {
            return Collections.emptyList();
        }
        List<String> operators = new ArrayList<String>();
        for (ResponsibilityAction action : chain.getActions()) {
            if (action.getOperatorId() != null && !operators.contains(action.getOperatorId())) {
                operators.add(action.getOperatorId());
            }
        }
        return operators;
    }

    private String chainKey(String assetType, String assetCode, String tenantId) {
        return (tenantId != null ? tenantId : "default") + "::" + assetType + "::" + assetCode;
    }

    // --- Inner classes ---

    static class ResponsibilityChain {
        private String assetType;
        private String assetCode;
        private String tenantId;
        private final List<ResponsibilityAction> actions = new ArrayList<ResponsibilityAction>();

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("asset_type", assetType);
            m.put("asset_code", assetCode);
            m.put("tenant_id", tenantId);
            m.put("total_actions", actions.size());
            List<Map<String, Object>> actionList = new ArrayList<Map<String, Object>>();
            for (ResponsibilityAction a : actions) {
                actionList.add(a.toMap());
            }
            m.put("actions", actionList);
            return m;
        }

        String getAssetType() { return assetType; }
        void setAssetType(String assetType) { this.assetType = assetType; }
        String getAssetCode() { return assetCode; }
        void setAssetCode(String assetCode) { this.assetCode = assetCode; }
        String getTenantId() { return tenantId; }
        void setTenantId(String tenantId) { this.tenantId = tenantId; }
        List<ResponsibilityAction> getActions() { return actions; }
    }

    static class ResponsibilityAction {
        private String action;
        private String operatorId;
        private LocalDateTime timestamp;
        private Map<String, Object> detail;

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("action", action);
            m.put("operator_id", operatorId);
            m.put("timestamp", timestamp != null ? timestamp.toString() : null);
            m.put("detail", detail);
            return m;
        }

        String getAction() { return action; }
        void setAction(String action) { this.action = action; }
        String getOperatorId() { return operatorId; }
        void setOperatorId(String operatorId) { this.operatorId = operatorId; }
        LocalDateTime getTimestamp() { return timestamp; }
        void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        Map<String, Object> getDetail() { return detail; }
        void setDetail(Map<String, Object> detail) { this.detail = detail; }
    }
}
