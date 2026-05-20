package com.medkernel.quality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估结果。
 * 记录一次评估执行的完整结果，含总分、各指标得分、风险等级、异常和缺失事实。
 */
public class EvalResult {
    private String evalId;
    private String tenantId;
    private String setCode;
    private String subjectType;
    private String subjectId;
    private String subjectName;
    private double totalScore;
    private double maxPossibleScore;
    private String riskLevel; // INFO, LOW, MEDIUM, HIGH, CRITICAL
    private List<IndicatorScore> indicatorScores;
    private List<EvalFact> abnormalFacts;
    private List<EvalFact> missingFacts;
    private String evaluatedBy;
    private String evaluatedAt;
    private Map<String, Object> orgContext;

    public static class IndicatorScore {
        private String indicatorCode;
        private String indicatorName;
        private String indicatorType;
        private double weight;
        private double rawScore;
        private double weightedScore;
        private double maxValue;
        private String riskLevel;
        private String unit;
        private boolean thresholdMet;
        private String explanation;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("indicator_code", indicatorCode);
            view.put("indicator_name", indicatorName);
            view.put("indicator_type", indicatorType);
            view.put("weight", weight);
            view.put("raw_score", rawScore);
            view.put("weighted_score", weightedScore);
            view.put("max_value", maxValue);
            view.put("risk_level", riskLevel);
            view.put("unit", unit);
            view.put("threshold_met", thresholdMet);
            view.put("explanation", explanation);
            return view;
        }

        public String getIndicatorCode() { return indicatorCode; }
        public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
        public String getIndicatorName() { return indicatorName; }
        public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
        public String getIndicatorType() { return indicatorType; }
        public void setIndicatorType(String indicatorType) { this.indicatorType = indicatorType; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public double getRawScore() { return rawScore; }
        public void setRawScore(double rawScore) { this.rawScore = rawScore; }
        public double getWeightedScore() { return weightedScore; }
        public void setWeightedScore(double weightedScore) { this.weightedScore = weightedScore; }
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public boolean isThresholdMet() { return thresholdMet; }
        public void setThresholdMet(boolean thresholdMet) { this.thresholdMet = thresholdMet; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class EvalFact {
        private String factType; // ABNORMAL, MISSING
        private String indicatorCode;
        private String indicatorName;
        private String description;
        private String severity;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("fact_type", factType);
            view.put("indicator_code", indicatorCode);
            view.put("indicator_name", indicatorName);
            view.put("description", description);
            view.put("severity", severity);
            return view;
        }

        public String getFactType() { return factType; }
        public void setFactType(String factType) { this.factType = factType; }
        public String getIndicatorCode() { return indicatorCode; }
        public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
        public String getIndicatorName() { return indicatorName; }
        public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("eval_id", evalId);
        view.put("tenant_id", tenantId);
        view.put("set_code", setCode);
        view.put("subject_type", subjectType);
        view.put("subject_id", subjectId);
        view.put("subject_name", subjectName);
        view.put("total_score", totalScore);
        view.put("max_possible_score", maxPossibleScore);
        view.put("score_percentage", maxPossibleScore > 0 ? Math.round(totalScore / maxPossibleScore * 1000.0) / 10.0 : 0);
        view.put("risk_level", riskLevel);
        if (indicatorScores != null) {
            List<Map<String, Object>> scoreViews = new ArrayList<Map<String, Object>>();
            for (IndicatorScore score : indicatorScores) {
                scoreViews.add(score.toView());
            }
            view.put("indicator_scores", scoreViews);
        }
        if (abnormalFacts != null) {
            List<Map<String, Object>> factViews = new ArrayList<Map<String, Object>>();
            for (EvalFact fact : abnormalFacts) {
                factViews.add(fact.toView());
            }
            view.put("abnormal_facts", factViews);
        }
        if (missingFacts != null) {
            List<Map<String, Object>> factViews = new ArrayList<Map<String, Object>>();
            for (EvalFact fact : missingFacts) {
                factViews.add(fact.toView());
            }
            view.put("missing_facts", factViews);
        }
        view.put("evaluated_by", evaluatedBy);
        view.put("evaluated_at", evaluatedAt);
        view.put("org_context", orgContext);
        return view;
    }

    // Getters and Setters
    public String getEvalId() { return evalId; }
    public void setEvalId(String evalId) { this.evalId = evalId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
    public double getMaxPossibleScore() { return maxPossibleScore; }
    public void setMaxPossibleScore(double maxPossibleScore) { this.maxPossibleScore = maxPossibleScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<IndicatorScore> getIndicatorScores() { return indicatorScores; }
    public void setIndicatorScores(List<IndicatorScore> indicatorScores) { this.indicatorScores = indicatorScores; }
    public List<EvalFact> getAbnormalFacts() { return abnormalFacts; }
    public void setAbnormalFacts(List<EvalFact> abnormalFacts) { this.abnormalFacts = abnormalFacts; }
    public List<EvalFact> getMissingFacts() { return missingFacts; }
    public void setMissingFacts(List<EvalFact> missingFacts) { this.missingFacts = missingFacts; }
    public String getEvaluatedBy() { return evaluatedBy; }
    public void setEvaluatedBy(String evaluatedBy) { this.evaluatedBy = evaluatedBy; }
    public String getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(String evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Map<String, Object> getOrgContext() { return orgContext; }
    public void setOrgContext(Map<String, Object> orgContext) { this.orgContext = orgContext; }
}
