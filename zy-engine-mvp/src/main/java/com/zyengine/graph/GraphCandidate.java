package com.zyengine.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphCandidate {
    private String diseaseCode;
    private String diseaseName;
    private double rawGraphScore;
    private List<Map<String, Object>> matchedRelations = new ArrayList<Map<String, Object>>();
    private List<String> evidenceRefs = new ArrayList<String>();

    public String getDiseaseCode() {
        return diseaseCode;
    }

    public void setDiseaseCode(String diseaseCode) {
        this.diseaseCode = diseaseCode;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public double getRawGraphScore() {
        return rawGraphScore;
    }

    public void setRawGraphScore(double rawGraphScore) {
        this.rawGraphScore = rawGraphScore;
    }

    public List<Map<String, Object>> getMatchedRelations() {
        return matchedRelations;
    }

    public void setMatchedRelations(List<Map<String, Object>> matchedRelations) {
        this.matchedRelations = matchedRelations;
    }

    public List<String> getEvidenceRefs() {
        return evidenceRefs;
    }

    public void setEvidenceRefs(List<String> evidenceRefs) {
        this.evidenceRefs = evidenceRefs;
    }
}

