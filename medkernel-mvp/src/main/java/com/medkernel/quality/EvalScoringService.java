package com.medkernel.quality;

import com.medkernel.common.OrganizationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能评估评分引擎。
 * 基于评估指标集对评估对象执行评分，计算加权总分、风险等级、异常和缺失事实。
 */
@Service
public class EvalScoringService {
    private final EvalService evalService;

    private static final AtomicLong EVAL_SEQ = new AtomicLong(1);
    private final Map<String, EvalResult> resultStore = new ConcurrentHashMap<String, EvalResult>();

    public EvalScoringService(EvalService evalService) {
        this.evalService = evalService;
    }

    /**
     * 执行评估评分。
     *
     * @param setCode    指标集编码
     * @param subjectId  评估对象ID
     * @param subjectName 评估对象名称
     * @param inputData  各指标的输入数据（indicator_code -> raw_value）
     * @param orgContext 组织上下文
     * @return 评估结果
     */
    public EvalResult evaluate(String setCode, String subjectId, String subjectName,
                                Map<String, Object> inputData, OrganizationContext orgContext) {
        // 获取指标集
        EvalIndicatorSet set = evalService.getSet(setCode, orgContext);
        if (set == null) {
            throw new IllegalArgumentException("Indicator set not found: " + setCode);
        }
        if (!"PUBLISHED".equals(set.getStatus())) {
            throw new IllegalArgumentException("Indicator set must be PUBLISHED before evaluation. Current: " + set.getStatus());
        }

        List<EvalIndicator> indicators = evalService.listIndicatorsBySet(setCode);
        if (indicators.isEmpty()) {
            throw new IllegalArgumentException("No indicators in set: " + setCode);
        }

        // 计算各指标得分
        List<EvalResult.IndicatorScore> scores = new ArrayList<EvalResult.IndicatorScore>();
        List<EvalResult.EvalFact> abnormalFacts = new ArrayList<EvalResult.EvalFact>();
        List<EvalResult.EvalFact> missingFacts = new ArrayList<EvalResult.EvalFact>();
        double totalWeightedScore = 0;
        double totalMaxWeightedScore = 0;

        for (EvalIndicator indicator : indicators) {
            EvalResult.IndicatorScore score = new EvalResult.IndicatorScore();
            score.setIndicatorCode(indicator.getIndicatorCode());
            score.setIndicatorName(indicator.getIndicatorName());
            score.setIndicatorType(indicator.getIndicatorType());
            score.setWeight(indicator.getWeight());
            score.setMaxValue(indicator.getMaxValue());
            score.setUnit(indicator.getUnit());

            // 获取输入值
            Object rawValue = inputData.get(indicator.getIndicatorCode());
            if (rawValue == null) {
                // 输入缺失
                score.setRawScore(0);
                score.setWeightedScore(0);
                score.setThresholdMet(false);
                score.setRiskLevel("HIGH");
                score.setExplanation("输入数据缺失");

                EvalResult.EvalFact missing = new EvalResult.EvalFact();
                missing.setFactType("MISSING");
                missing.setIndicatorCode(indicator.getIndicatorCode());
                missing.setIndicatorName(indicator.getIndicatorName());
                missing.setDescription("指标 " + indicator.getIndicatorName() + " 输入数据缺失");
                missing.setSeverity("HIGH");
                missingFacts.add(missing);
            } else {
                // 计算原始得分
                double rawScore = toDouble(rawValue);
                score.setRawScore(rawScore);

                // 计算加权得分
                double weightedScore = rawScore * indicator.getWeight();
                score.setWeightedScore(weightedScore);

                // 判断阈值
                boolean thresholdMet = evaluateThreshold(indicator.getThresholdExpression(), rawScore);
                score.setThresholdMet(thresholdMet);

                // 映射风险等级
                String riskLevel = mapRiskLevel(indicator.getRiskLevelMapping(), rawScore);
                score.setRiskLevel(riskLevel);

                // 生成解释
                score.setExplanation(buildExplanation(indicator, rawScore, thresholdMet, riskLevel));

                // 异常事实
                if (!thresholdMet) {
                    EvalResult.EvalFact abnormal = new EvalResult.EvalFact();
                    abnormal.setFactType("ABNORMAL");
                    abnormal.setIndicatorCode(indicator.getIndicatorCode());
                    abnormal.setIndicatorName(indicator.getIndicatorName());
                    abnormal.setDescription(indicator.getIndicatorName() + " 得分 " + rawScore + indicator.getUnit()
                            + " 未达阈值 " + (indicator.getThresholdExpression() != null ? indicator.getThresholdExpression() : "N/A"));
                    abnormal.setSeverity(riskLevel);
                    abnormalFacts.add(abnormal);
                }
            }

            totalWeightedScore += score.getWeightedScore();
            totalMaxWeightedScore += indicator.getMaxValue() * indicator.getWeight();
            scores.add(score);
        }

        // 计算总分百分比
        double percentage = totalMaxWeightedScore > 0 ? (totalWeightedScore / totalMaxWeightedScore) * 100 : 0;

        // 确定整体风险等级
        String overallRiskLevel = determineOverallRiskLevel(percentage, abnormalFacts);

        // 构建评估结果
        EvalResult result = new EvalResult();
        result.setEvalId("EVAL-" + String.format("%04d", EVAL_SEQ.getAndIncrement()));
        result.setTenantId(orgContext.getTenantId());
        result.setSetCode(setCode);
        result.setSubjectType(set.getSubjectType());
        result.setSubjectId(subjectId);
        result.setSubjectName(subjectName);
        result.setTotalScore(Math.round(totalWeightedScore * 100.0) / 100.0);
        result.setMaxPossibleScore(Math.round(totalMaxWeightedScore * 100.0) / 100.0);
        result.setRiskLevel(overallRiskLevel);
        result.setIndicatorScores(scores);
        result.setAbnormalFacts(abnormalFacts);
        result.setMissingFacts(missingFacts);
        result.setEvaluatedBy("system");
        result.setEvaluatedAt(LocalDateTime.now().toString());
        result.setOrgContext(buildOrgContextView(orgContext));

        resultStore.put(result.getEvalId(), result);
        return result;
    }

    /**
     * 获取评估结果。
     */
    public EvalResult getResult(String evalId, OrganizationContext orgContext) {
        EvalResult result = resultStore.get(evalId);
        if (result == null || !orgContext.getTenantId().equals(result.getTenantId())) {
            return null;
        }
        return result;
    }

    /**
     * 列出评估结果。
     */
    public List<EvalResult> listResults(String setCode, String subjectType, OrganizationContext orgContext) {
        List<EvalResult> results = new ArrayList<EvalResult>();
        for (EvalResult result : resultStore.values()) {
            if (!orgContext.getTenantId().equals(result.getTenantId())) continue;
            if (setCode != null && !setCode.equals(result.getSetCode())) continue;
            if (subjectType != null && !subjectType.equalsIgnoreCase(result.getSubjectType())) continue;
            results.add(result);
        }
        return results;
    }

    // ==================== 辅助方法 ====================

    private boolean evaluateThreshold(String thresholdExpression, double rawScore) {
        if (thresholdExpression == null || thresholdExpression.isEmpty()) {
            return true; // 无阈值则默认通过
        }
        // MVP: 简单解析 "score >= N" 格式
        String expr = thresholdExpression.trim().toLowerCase();
        if (expr.contains(">=")) {
            double target = parseNumber(expr.substring(expr.indexOf(">=") + 2));
            return rawScore >= target;
        } else if (expr.contains("<=")) {
            double target = parseNumber(expr.substring(expr.indexOf("<=") + 2));
            return rawScore <= target;
        } else if (expr.contains(">")) {
            double target = parseNumber(expr.substring(expr.indexOf(">") + 1));
            return rawScore > target;
        } else if (expr.contains("<")) {
            double target = parseNumber(expr.substring(expr.indexOf("<") + 1));
            return rawScore < target;
        }
        return true;
    }

    private String mapRiskLevel(String riskLevelMapping, double rawScore) {
        if (riskLevelMapping == null || riskLevelMapping.isEmpty()) {
            return "INFO";
        }
        // MVP: 简单解析 {"HIGH":"score<60","MEDIUM":"score<80","LOW":"score<90"} 格式
        // 按优先级检查：CRITICAL > HIGH > MEDIUM > LOW
        String[] levels = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};
        for (String level : levels) {
            String key = "\"" + level + "\"";
            int idx = riskLevelMapping.indexOf(key);
            if (idx < 0) continue;
            // 找到冒号后的条件
            int colonIdx = riskLevelMapping.indexOf(":", idx);
            int commaIdx = riskLevelMapping.indexOf(",", colonIdx);
            int bracketIdx = riskLevelMapping.indexOf("}", colonIdx);
            int endIdx = Math.min(commaIdx > 0 ? commaIdx : Integer.MAX_VALUE,
                    bracketIdx > 0 ? bracketIdx : Integer.MAX_VALUE);
            if (endIdx == Integer.MAX_VALUE) endIdx = riskLevelMapping.length();

            String condition = riskLevelMapping.substring(colonIdx + 1, endIdx)
                    .replace("\"", "").trim();
            if (evaluateThreshold(condition, rawScore)) {
                return level;
            }
        }
        return "INFO";
    }

    private String determineOverallRiskLevel(double percentage, List<EvalResult.EvalFact> abnormalFacts) {
        // 有 CRITICAL 异常事实 → CRITICAL
        for (EvalResult.EvalFact fact : abnormalFacts) {
            if ("CRITICAL".equals(fact.getSeverity())) return "CRITICAL";
        }
        // 有 HIGH 异常事实 → HIGH
        for (EvalResult.EvalFact fact : abnormalFacts) {
            if ("HIGH".equals(fact.getSeverity())) return "HIGH";
        }
        // 百分比判断
        if (percentage < 60) return "HIGH";
        if (percentage < 75) return "MEDIUM";
        if (percentage < 90) return "LOW";
        return "INFO";
    }

    private String buildExplanation(EvalIndicator indicator, double rawScore, boolean thresholdMet, String riskLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(indicator.getIndicatorName());
        sb.append(" 得分 ").append(rawScore);
        if (indicator.getUnit() != null) sb.append(indicator.getUnit());
        sb.append("（满分 ").append(indicator.getMaxValue());
        if (indicator.getUnit() != null) sb.append(indicator.getUnit());
        sb.append("）");
        sb.append(thresholdMet ? "，达标" : "，未达标");
        sb.append("，风险等级：").append(riskLevel);
        return sb.toString();
    }

    private double parseNumber(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private Map<String, Object> buildOrgContextView(OrganizationContext ctx) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("group_code", ctx.getGroupCode());
        view.put("hospital_code", ctx.getHospitalCode());
        view.put("campus_code", ctx.getCampusCode());
        view.put("site_code", ctx.getSiteCode());
        view.put("department_code", ctx.getDepartmentCode());
        view.put("scope_level", ctx.getScopeLevel());
        view.put("scope_code", ctx.getScopeCode());
        view.put("org_source", ctx.getOrgSource());
        return view;
    }
}
