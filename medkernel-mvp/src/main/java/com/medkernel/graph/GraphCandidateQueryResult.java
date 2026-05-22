package com.medkernel.graph;
import java.util.List;
public class GraphCandidateQueryResult {
    private final List<GraphCandidate> candidates;
    private final String graphVersion;
    private final String source;
    private final boolean degraded;
    private final String degradedReason;
    GraphCandidateQueryResult(List<GraphCandidate> candidates, String graphVersion, String source,
                              boolean degraded, String degradedReason) {
        this.candidates = candidates;
        this.graphVersion = graphVersion;
        this.source = source;
        this.degraded = degraded;
        this.degradedReason = degradedReason;
    }
    public List<GraphCandidate> getCandidates() {
        return candidates;
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
