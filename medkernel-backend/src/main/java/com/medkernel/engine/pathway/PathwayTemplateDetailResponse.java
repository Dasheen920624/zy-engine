package com.medkernel.engine.pathway;

import java.util.List;

public record PathwayTemplateDetailResponse(
    PathwayTemplate template,
    List<PathwayNode> nodes,
    List<PathwayEdge> edges,
    List<SpecialtyMetricBinding> metricBindings,
    String traceId
) {

    public PathwayTemplateDetailResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        metricBindings = metricBindings == null ? List.of() : List.copyOf(metricBindings);
    }
}
