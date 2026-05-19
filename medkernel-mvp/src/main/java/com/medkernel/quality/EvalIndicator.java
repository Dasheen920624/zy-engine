package com.medkernel.quality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估指标。
 * 单个指标，含指标编码、名称、类型、权重、阈值、风险等级映射、计算表达式。
 * 对应数据库表：qc_eval_indicator
 */
public class EvalIndicator {
    private String tenantId;
    private String indicatorCode;
    private String setCode;
    private String indicatorName;
    private String indicatorType; // SCORE, RATE, COUNT, BOOLEAN
    private double weight;
    private double maxValue;
    private String thresholdExpression; // 阈值表达式，如 "score >= 60"
    private String riskLevelMapping; // 风险等级映射 JSON，如 {"HIGH":"score<60","MEDIUM":"score<80","LOW":"score<90"}
    private String calcExpression; // 计算表达式，如 "hit_count / total_count * 100"
    private String unit; // 单位，如 "%", "分", "次"
    private String description;
    private String documentCode;
    private String citationId;
    private String bindingType;
    private String status; // DRAFT, PUBLISHED, DEPRECATED
    private String createdBy;
    private String createdTime;
    private String updatedTime;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }

    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }

    public String getIndicatorType() { return indicatorType; }
    public void setIndicatorType(String indicatorType) { this.indicatorType = indicatorType; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }

    public String getThresholdExpression() { return thresholdExpression; }
    public void setThresholdExpression(String thresholdExpression) { this.thresholdExpression = thresholdExpression; }

    public String getRiskLevelMapping() { return riskLevelMapping; }
    public void setRiskLevelMapping(String riskLevelMapping) { this.riskLevelMapping = riskLevelMapping; }

    public String getCalcExpression() { return calcExpression; }
    public void setCalcExpression(String calcExpression) { this.calcExpression = calcExpression; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDocumentCode() { return documentCode; }
    public void setDocumentCode(String documentCode) { this.documentCode = documentCode; }

    public String getCitationId() { return citationId; }
    public void setCitationId(String citationId) { this.citationId = citationId; }

    public String getBindingType() { return bindingType; }
    public void setBindingType(String bindingType) { this.bindingType = bindingType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("indicator_code", indicatorCode);
        view.put("set_code", setCode);
        view.put("indicator_name", indicatorName);
        view.put("indicator_type", indicatorType);
        view.put("weight", weight);
        view.put("max_value", maxValue);
        view.put("threshold_expression", thresholdExpression);
        view.put("risk_level_mapping", riskLevelMapping);
        view.put("calc_expression", calcExpression);
        view.put("unit", unit);
        view.put("description", description);
        view.put("source", buildSourceView());
        view.put("status", status);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        return view;
    }

    private Map<String, Object> buildSourceView() {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("document_code", documentCode);
        source.put("citation_id", citationId);
        source.put("binding_type", bindingType);
        return source;
    }
}
