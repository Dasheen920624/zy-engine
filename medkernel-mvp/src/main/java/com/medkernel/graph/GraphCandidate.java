package com.medkernel.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphCandidate {
    private String diseaseCode;
    private String diseaseName;
    private double rawGraphScore;
    private List<Map<String, Object>> matchedRelations = new ArrayList<Map<String, Object>>();
    private List<String> evidenceRefs = new ArrayList<String>();
    private String graphVersion;
    private String graphSource;
    private boolean degraded;
    private String degradedReason;

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

    public String getGraphVersion() {
        return graphVersion;
    }

    public void setGraphVersion(String graphVersion) {
        this.graphVersion = graphVersion;
    }

    public String getGraphSource() {
        return graphSource;
    }

    public void setGraphSource(String graphSource) {
        this.graphSource = graphSource;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getDegradedReason() {
        return degradedReason;
    }

    public void setDegradedReason(String degradedReason) {
        this.degradedReason = degradedReason;
    }
}
