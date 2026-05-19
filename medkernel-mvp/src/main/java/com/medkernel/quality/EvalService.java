package com.medkernel.quality;

import com.medkernel.cdss.CdssRiskLevel;
import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.provenance.PublishGateService;
import com.medkernel.provenance.SourceAssetBindingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能评估指标模型服务。
 * 管理评估指标集和指标的 CRUD、版本管理、来源绑定和发布门禁。
 */
@Service
public class EvalService {
    private final OrganizationContextService organizationContextService;
    private final SourceAssetBindingService sourceAssetBindingService;
    private final PublishGateService publishGateService;

    private static final AtomicLong SET_SEQ = new AtomicLong(1);
    private static final AtomicLong IND_SEQ = new AtomicLong(1);
    private final Map<String, EvalIndicatorSet> setStore = new ConcurrentHashMap<String, EvalIndicatorSet>();
    private final Map<String, EvalIndicator> indicatorStore = new ConcurrentHashMap<String, EvalIndicator>();

    public EvalService(OrganizationContextService organizationContextService,
                       SourceAssetBindingService sourceAssetBindingService,
                       PublishGateService publishGateService) {
        this.organizationContextService = organizationContextService;
        this.sourceAssetBindingService = sourceAssetBindingService;
        this.publishGateService = publishGateService;
    }

    // ==================== 指标集 CRUD ====================

    public EvalIndicatorSet createSet(Map<String, Object> request, OrganizationContext orgContext) {
        String setCode = generateSetCode();
        EvalIndicatorSet set = new EvalIndicatorSet();
        set.setTenantId(orgContext.getTenantId());
        set.setSetCode(setCode);
        set.setSetName((String) request.get("set_name"));
        set.setSubjectType((String) request.get("subject_type"));
        set.setDescription((String) request.get("description"));
        set.setVersion("1.0.0");
        set.setStatus("DRAFT");
        set.setDocumentCode((String) request.get("document_code"));
        set.setCitationId((String) request.get("citation_id"));
        set.setBindingType((String) request.get("binding_type"));
        applyOrgContext(set, orgContext);
        set.setCreatedBy((String) request.get("created_by"));
        set.setCreatedTime(LocalDateTime.now().toString());
        set.setUpdatedTime(LocalDateTime.now().toString());
        set.setIndicators(new ArrayList<EvalIndicator>());

        setStore.put(setCode, set);
        return set;
    }

    public EvalIndicatorSet updateSet(String setCode, Map<String, Object> request, OrganizationContext orgContext) {
        EvalIndicatorSet set = setStore.get(setCode);
        if (set == null) {
            throw new IllegalArgumentException("Indicator set not found: " + setCode);
        }
        if ("PUBLISHED".equals(set.getStatus())) {
            throw new IllegalArgumentException("Cannot update a PUBLISHED indicator set. Create a new version instead.");
        }
        if (request.containsKey("set_name")) set.setSetName((String) request.get("set_name"));
        if (request.containsKey("subject_type")) set.setSubjectType((String) request.get("subject_type"));
        if (request.containsKey("description")) set.setDescription((String) request.get("description"));
        if (request.containsKey("document_code")) set.setDocumentCode((String) request.get("document_code"));
        if (request.containsKey("citation_id")) set.setCitationId((String) request.get("citation_id"));
        if (request.containsKey("binding_type")) set.setBindingType((String) request.get("binding_type"));
        set.setUpdatedTime(LocalDateTime.now().toString());
        return set;
    }

    public EvalIndicatorSet publishSet(String setCode, OrganizationContext orgContext) {
        EvalIndicatorSet set = setStore.get(setCode);
        if (set == null) {
            throw new IllegalArgumentException("Indicator set not found: " + setCode);
        }
        if ("PUBLISHED".equals(set.getStatus())) {
            throw new IllegalArgumentException("Indicator set is already PUBLISHED.");
        }
        // 发布门禁：检查来源文档绑定
        if (set.getDocumentCode() != null && !set.getDocumentCode().isEmpty()) {
            publishGateService.requirePublishGate("QC_METRIC", setCode, orgContext.getTenantId());
        }
        // 检查指标集内至少有一个指标
        List<EvalIndicator> indicators = listIndicatorsBySet(setCode);
        if (indicators.isEmpty()) {
            throw new IllegalArgumentException("Cannot publish an indicator set with no indicators.");
        }
        // 检查权重总和
        double totalWeight = 0;
        for (EvalIndicator ind : indicators) {
            totalWeight += ind.getWeight();
        }
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Indicator set must have positive total weight.");
        }
        set.setStatus("PUBLISHED");
        set.setUpdatedTime(LocalDateTime.now().toString());
        return set;
    }

    public EvalIndicatorSet deprecateSet(String setCode, OrganizationContext orgContext) {
        EvalIndicatorSet set = setStore.get(setCode);
        if (set == null) {
            throw new IllegalArgumentException("Indicator set not found: " + setCode);
        }
        set.setStatus("DEPRECATED");
        set.setUpdatedTime(LocalDateTime.now().toString());
        return set;
    }

    public List<EvalIndicatorSet> listSets(Map<String, String> filters, OrganizationContext orgContext) {
        List<EvalIndicatorSet> result = new ArrayList<EvalIndicatorSet>();
        for (EvalIndicatorSet set : setStore.values()) {
            if (!orgContext.getTenantId().equals(set.getTenantId())) continue;
            if (filters != null) {
                String subjectType = filters.get("subject_type");
                if (subjectType != null && !subjectType.equalsIgnoreCase(set.getSubjectType())) continue;
                String status = filters.get("status");
                if (status != null && !status.equalsIgnoreCase(set.getStatus())) continue;
            }
            // 加载指标
            set.setIndicators(listIndicatorsBySet(set.getSetCode()));
            result.add(set);
        }
        return result;
    }

    public EvalIndicatorSet getSet(String setCode, OrganizationContext orgContext) {
        EvalIndicatorSet set = setStore.get(setCode);
        if (set == null || !orgContext.getTenantId().equals(set.getTenantId())) {
            return null;
        }
        set.setIndicators(listIndicatorsBySet(setCode));
        return set;
    }

    // ==================== 指标 CRUD ====================

    public EvalIndicator createIndicator(String setCode, Map<String, Object> request, OrganizationContext orgContext) {
        EvalIndicatorSet set = setStore.get(setCode);
        if (set == null) {
            throw new IllegalArgumentException("Indicator set not found: " + setCode);
        }
        if ("PUBLISHED".equals(set.getStatus())) {
            throw new IllegalArgumentException("Cannot add indicators to a PUBLISHED set.");
        }

        String indicatorCode = generateIndicatorCode();
        EvalIndicator indicator = new EvalIndicator();
        indicator.setTenantId(orgContext.getTenantId());
        indicator.setIndicatorCode(indicatorCode);
        indicator.setSetCode(setCode);
        indicator.setIndicatorName((String) request.get("indicator_name"));
        indicator.setIndicatorType((String) request.get("indicator_type"));
        indicator.setWeight(request.containsKey("weight") ? toDouble(request.get("weight")) : 1.0);
        indicator.setMaxValue(request.containsKey("max_value") ? toDouble(request.get("max_value")) : 100.0);
        indicator.setThresholdExpression((String) request.get("threshold_expression"));
        indicator.setRiskLevelMapping((String) request.get("risk_level_mapping"));
        indicator.setCalcExpression((String) request.get("calc_expression"));
        indicator.setUnit((String) request.get("unit"));
        indicator.setDescription((String) request.get("description"));
        indicator.setDocumentCode((String) request.get("document_code"));
        indicator.setCitationId((String) request.get("citation_id"));
        indicator.setBindingType((String) request.get("binding_type"));
        indicator.setStatus("DRAFT");
        indicator.setCreatedBy((String) request.get("created_by"));
        indicator.setCreatedTime(LocalDateTime.now().toString());
        indicator.setUpdatedTime(LocalDateTime.now().toString());

        indicatorStore.put(indicatorCode, indicator);
        return indicator;
    }

    public EvalIndicator updateIndicator(String indicatorCode, Map<String, Object> request, OrganizationContext orgContext) {
        EvalIndicator indicator = indicatorStore.get(indicatorCode);
        if (indicator == null) {
            throw new IllegalArgumentException("Indicator not found: " + indicatorCode);
        }
        // 检查所属指标集是否已发布
        EvalIndicatorSet set = setStore.get(indicator.getSetCode());
        if (set != null && "PUBLISHED".equals(set.getStatus())) {
            throw new IllegalArgumentException("Cannot update indicator in a PUBLISHED set.");
        }
        if (request.containsKey("indicator_name")) indicator.setIndicatorName((String) request.get("indicator_name"));
        if (request.containsKey("indicator_type")) indicator.setIndicatorType((String) request.get("indicator_type"));
        if (request.containsKey("weight")) indicator.setWeight(toDouble(request.get("weight")));
        if (request.containsKey("max_value")) indicator.setMaxValue(toDouble(request.get("max_value")));
        if (request.containsKey("threshold_expression")) indicator.setThresholdExpression((String) request.get("threshold_expression"));
        if (request.containsKey("risk_level_mapping")) indicator.setRiskLevelMapping((String) request.get("risk_level_mapping"));
        if (request.containsKey("calc_expression")) indicator.setCalcExpression((String) request.get("calc_expression"));
        if (request.containsKey("unit")) indicator.setUnit((String) request.get("unit"));
        if (request.containsKey("description")) indicator.setDescription((String) request.get("description"));
        if (request.containsKey("document_code")) indicator.setDocumentCode((String) request.get("document_code"));
        if (request.containsKey("citation_id")) indicator.setCitationId((String) request.get("citation_id"));
        if (request.containsKey("binding_type")) indicator.setBindingType((String) request.get("binding_type"));
        indicator.setUpdatedTime(LocalDateTime.now().toString());
        return indicator;
    }

    public EvalIndicator deleteIndicator(String indicatorCode, OrganizationContext orgContext) {
        EvalIndicator indicator = indicatorStore.remove(indicatorCode);
        if (indicator == null) {
            throw new IllegalArgumentException("Indicator not found: " + indicatorCode);
        }
        return indicator;
    }

    public List<EvalIndicator> listIndicatorsBySet(String setCode) {
        List<EvalIndicator> result = new ArrayList<EvalIndicator>();
        for (EvalIndicator ind : indicatorStore.values()) {
            if (setCode.equals(ind.getSetCode())) {
                result.add(ind);
            }
        }
        return result;
    }

    public EvalIndicator getIndicator(String indicatorCode, OrganizationContext orgContext) {
        EvalIndicator indicator = indicatorStore.get(indicatorCode);
        if (indicator == null || !orgContext.getTenantId().equals(indicator.getTenantId())) {
            return null;
        }
        return indicator;
    }

    // ==================== 辅助方法 ====================

    private String generateSetCode() {
        return "EVAL-SET-" + String.format("%04d", SET_SEQ.getAndIncrement());
    }

    private String generateIndicatorCode() {
        return "EVAL-IND-" + String.format("%04d", IND_SEQ.getAndIncrement());
    }

    private void applyOrgContext(EvalIndicatorSet set, OrganizationContext orgContext) {
        set.setGroupCode(orgContext.getGroupCode());
        set.setHospitalCode(orgContext.getHospitalCode());
        set.setCampusCode(orgContext.getCampusCode());
        set.setSiteCode(orgContext.getSiteCode());
        set.setDepartmentCode(orgContext.getDepartmentCode());
        set.setScopeLevel(orgContext.getScopeLevel());
        set.setScopeCode(orgContext.getScopeCode());
        set.setOrgSource(orgContext.getOrgSource());
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
