package com.medkernel.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecommendationCard {
    private String recommendationId;
    private String scenario;
    private String patientId;
    private String encounterId;
    private String targetCode;
    private String targetName;
    private double score;
    private String confidence;
    private String actionLevel;
    private List<Map<String, Object>> supportingFacts = new ArrayList<Map<String, Object>>();
    private List<String> missingFacts = new ArrayList<String>();
    private List<String> evidenceRefs = new ArrayList<String>();
    private List<String> suggestedActions = new ArrayList<String>();

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getActionLevel() {
        return actionLevel;
    }

    public void setActionLevel(String actionLevel) {
        this.actionLevel = actionLevel;
    }

    public List<Map<String, Object>> getSupportingFacts() {
        return supportingFacts;
    }

    public void setSupportingFacts(List<Map<String, Object>> supportingFacts) {
        this.supportingFacts = supportingFacts;
    }

    public List<String> getMissingFacts() {
        return missingFacts;
    }

    public void setMissingFacts(List<String> missingFacts) {
        this.missingFacts = missingFacts;
    }

    public List<String> getEvidenceRefs() {
        return evidenceRefs;
    }

    public void setEvidenceRefs(List<String> evidenceRefs) {
        this.evidenceRefs = evidenceRefs;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }
}

