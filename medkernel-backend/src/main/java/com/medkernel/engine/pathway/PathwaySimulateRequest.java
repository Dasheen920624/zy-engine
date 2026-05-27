package com.medkernel.engine.pathway;

import java.util.List;

public record PathwaySimulateRequest(
    String startNodeCode,
    List<String> requestedNextNodeCodes
) {

    public PathwaySimulateRequest {
        requestedNextNodeCodes = requestedNextNodeCodes == null
            ? List.of() : List.copyOf(requestedNextNodeCodes);
    }
}
