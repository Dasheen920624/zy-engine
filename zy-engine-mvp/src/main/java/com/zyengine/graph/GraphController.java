package com.zyengine.graph;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
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

    @PostMapping("/versions")
    public ApiResult<List<Map<String, Object>>> importVersions(@RequestBody Object request) {
        return ApiResult.success(graphService.importGraphVersions(request));
    }

    @GetMapping("/versions")
    public ApiResult<List<Map<String, Object>>> listVersions() {
        return ApiResult.success(graphService.listGraphVersions());
    }

    @GetMapping("/versions/{graphVersion}")
    public ApiResult<Map<String, Object>> getVersion(@PathVariable String graphVersion) {
        return ApiResult.success(graphService.getGraphVersion(graphVersion));
    }

    @PostMapping("/versions/{graphVersion}/activate")
    public ApiResult<Map<String, Object>> activateVersion(@PathVariable String graphVersion,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(graphService.activateVersion(graphVersion, request));
    }

    @PostMapping("/versions/{graphVersion}/rollback")
    public ApiResult<Map<String, Object>> rollbackVersion(@PathVariable String graphVersion,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(graphService.rollbackVersion(graphVersion, request));
    }

    @PostMapping("/evidences")
    public ApiResult<List<Map<String, Object>>> importEvidences(@RequestBody Object request) {
        return ApiResult.success(graphService.importGraphEvidences(request));
    }

    @GetMapping("/evidences")
    public ApiResult<List<Map<String, Object>>> listEvidences(@RequestParam(required = false) String graphVersion,
                                                              @RequestParam(required = false) String targetCode,
                                                              @RequestParam(required = false) String targetType,
                                                              @RequestParam(required = false) String evidenceType,
                                                              @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("targetCode", targetCode);
        filters.put("targetType", targetType);
        filters.put("evidenceType", evidenceType);
        filters.put("limit", limit);
        return ApiResult.success(graphService.listGraphEvidences(filters));
    }

    @GetMapping("/evidences/{evidenceId}")
    public ApiResult<Map<String, Object>> getEvidence(@PathVariable String evidenceId) {
        return ApiResult.success(graphService.getGraphEvidence(evidenceId));
    }

    @PostMapping("/nodes")
    public ApiResult<List<Map<String, Object>>> importNodes(@RequestBody Object request) {
        return ApiResult.success(graphService.importGraphNodes(request));
    }

    @GetMapping("/nodes")
    public ApiResult<List<Map<String, Object>>> listNodes(@RequestParam(required = false) String graphVersion,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("type", type);
        filters.put("limit", limit);
        return ApiResult.success(graphService.listGraphNodes(filters));
    }

    @PostMapping("/edges")
    public ApiResult<List<Map<String, Object>>> importEdges(@RequestBody Object request) {
        return ApiResult.success(graphService.importGraphEdges(request));
    }

    @GetMapping("/edges")
    public ApiResult<List<Map<String, Object>>> listEdges(@RequestParam(required = false) String graphVersion,
                                                          @RequestParam(required = false) String fromCode,
                                                          @RequestParam(required = false) String toCode,
                                                          @RequestParam(required = false) String relationType,
                                                          @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("fromCode", fromCode);
        filters.put("toCode", toCode);
        filters.put("relationType", relationType);
        filters.put("limit", limit);
        return ApiResult.success(graphService.listGraphEdges(filters));
    }
}
