package com.medkernel.graph;
import java.util.List;
import java.util.Map;
public class GraphEvidenceQueryResult {
    private final List<Map<String, Object>> evidence;
    private final String targetCode;
    private final String graphVersion;
    private final String source;
    private final boolean degraded;
    private final String degradedReason;
    GraphEvidenceQueryResult(List<Map<String, Object>> evidence, String targetCode, String graphVersion,
                             String source, boolean degraded, String degradedReason) {
        this.evidence = evidence;
        this.targetCode = targetCode;
        this.graphVersion = graphVersion;
        this.source = source;
        this.degraded = degraded;
        this.degradedReason = degradedReason;
    }
    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }
    public String getTargetCode() {
        return targetCode;
    }
    public String getGraphVersion() {
        return graphVersion;
    }
    public String getSource() {
        return source;
    }
    public boolean isDegraded() {
        return degraded;
    }
    public String getDegradedReason() {
        return degradedReason;
    }
}
