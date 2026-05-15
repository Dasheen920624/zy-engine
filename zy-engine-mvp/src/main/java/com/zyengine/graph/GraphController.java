package com.zyengine.graph;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {
    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/disease-candidates")
    public ApiResult<List<GraphCandidate>> diseaseCandidates(@RequestBody Map<String, Object> request) {
        return ApiResult.success(graphService.diseaseCandidates(request));
    }

    @PostMapping("/evidence")
    public ApiResult<List<Map<String, Object>>> evidence(@RequestBody Map<String, Object> request) {
        return ApiResult.success(graphService.evidence(request));
    }
}

