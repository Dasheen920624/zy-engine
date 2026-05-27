package com.medkernel.engine.pathway;

import java.util.List;

public record PathwayGraph(
    List<PathwayNode> nodes,
    List<PathwayEdge> edges
) {

    public PathwayGraph {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }
}
