package com.medkernel.graph;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {
    private final GraphService graphService;
    private final GraphSyncService graphSyncService;
    private final OrganizationContextService organizationContextService;

    public GraphController(GraphService graphService, GraphSyncService graphSyncService,
                           OrganizationContextService organizationContextService) {
        this.graphService = graphService;
        this.graphSyncService = graphSyncService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping("/disease-candidates")
    public ApiResult<List<GraphCandidate>> diseaseCandidates(@RequestBody Map<String, Object> request,
                                                              HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(graphService.diseaseCandidates(request));
    }

    @PostMapping("/evidence")
    public ApiResult<List<Map<String, Object>>> evidence(@RequestBody Map<String, Object> request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(graphService.evidence(request));
    }

    @PostMapping("/versions")
    public ApiResult<List<Map<String, Object>>> importVersions(@RequestBody Object request,
                                                                HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.importGraphVersions(request));
    }

    @GetMapping("/versions")
    public ApiResult<List<Map<String, Object>>> listVersions(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.listGraphVersions());
    }

    @GetMapping("/versions/{graphVersion}")
    public ApiResult<Map<String, Object>> getVersion(@PathVariable String graphVersion,
                                                      HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.getGraphVersion(graphVersion));
    }

    @PostMapping("/versions/{graphVersion}/activate")
    public ApiResult<Map<String, Object>> activateVersion(@PathVariable String graphVersion,
                                                          @RequestBody(required = false) Map<String, Object> request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.activateGraphVersion(graphVersion, request));
    }

    @PostMapping("/versions/{graphVersion}/rollback")
    public ApiResult<Map<String, Object>> rollbackVersion(@PathVariable String graphVersion,
                                                          @RequestBody(required = false) Map<String, Object> request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.rollbackVersion(graphVersion, request));
    }

    @PostMapping("/evidences")
    public ApiResult<List<Map<String, Object>>> importEvidences(@RequestBody Object request,
                                                                 HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.importGraphEvidences(request));
    }

    @GetMapping("/evidences")
    public ApiResult<List<Map<String, Object>>> listEvidences(@RequestParam(required = false) String graphVersion,
                                                              @RequestParam(required = false) String targetCode,
                                                              @RequestParam(required = false) String targetType,
                                                              @RequestParam(required = false) String evidenceType,
                                                              @RequestParam(required = false) String limit,
                                                              HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("targetCode", targetCode);
        filters.put("targetType", targetType);
        filters.put("evidenceType", evidenceType);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.listGraphEvidences(filters));
    }

    @GetMapping("/evidences/{evidenceId}")
    public ApiResult<Map<String, Object>> getEvidence(@PathVariable String evidenceId,
                                                       HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.getGraphEvidence(evidenceId));
    }

    @PostMapping("/nodes")
    public ApiResult<List<Map<String, Object>>> importNodes(@RequestBody Object request,
                                                              HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.importGraphNodes(request));
    }

    @GetMapping("/nodes")
    public ApiResult<List<Map<String, Object>>> listNodes(@RequestParam(required = false) String graphVersion,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String limit,
                                                          HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("type", type);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.listGraphNodes(filters));
    }

    @PostMapping("/edges")
    public ApiResult<List<Map<String, Object>>> importEdges(@RequestBody Object request,
                                                              HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(graphService.importGraphEdges(request));
    }

    @GetMapping("/edges")
    public ApiResult<List<Map<String, Object>>> listEdges(@RequestParam(required = false) String graphVersion,
                                                          @RequestParam(required = false) String fromCode,
                                                          @RequestParam(required = false) String toCode,
                                                          @RequestParam(required = false) String relationType,
                                                          @RequestParam(required = false) String limit,
                                                          HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("graphVersion", graphVersion);
        filters.put("fromCode", fromCode);
        filters.put("toCode", toCode);
        filters.put("relationType", relationType);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphService.listGraphEdges(filters));
    }

    // =========================================================================
    // GRAPH-005: Neo4j 同步（dry-run + 重试）
    // =========================================================================

    /**
     * 将图谱数据同步到 Neo4j。
     *
     * @param graphVersion 图谱版本号
     * @param dryRun       是否干运行（仅预览不同步）
     * @param httpRequest  HTTP 请求（用于获取操作人信息）
     * @return 同步任务结果
     */
    @PostMapping("/versions/{graphVersion}/sync")
    public ApiResult<Map<String, Object>> syncToNeo4j(@PathVariable String graphVersion,
                                                      @RequestParam(defaultValue = "false") boolean dryRun,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        String triggeredBy = httpRequest.getHeader("X-Operator-Id");
        GraphSyncTask task = graphSyncService.syncToNeo4j(graphVersion, dryRun, triggeredBy);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("task_code", task.getTaskCode());
        result.put("task_type", task.getTaskType());
        result.put("target_system", task.getTargetSystem());
        result.put("target_version", task.getTargetVersion());
        result.put("status", task.getStatus());
        result.put("dry_run", task.isDryRun());
        result.put("total_count", task.getTotalCount());
        result.put("success_count", task.getSuccessCount());
        result.put("failed_count", task.getFailedCount());
        result.put("skip_count", task.getSkipCount());
        result.put("duration_ms", task.getDurationMs());
        result.put("error_message", task.getErrorMessage());
        result.put("triggered_by", task.getTriggeredBy());
        result.put("started_time", task.getStartedTime());
        result.put("finished_time", task.getFinishedTime());
        return ApiResult.success(result);
    }

    /**
     * 重试失败的同步任务。
     *
     * @param taskCode    原任务编码
     * @param dryRun      是否干运行
     * @param httpRequest HTTP 请求
     * @return 重试后的同步任务结果
     */
    @PostMapping("/sync-tasks/{taskCode}/retry")
    public ApiResult<Map<String, Object>> retrySync(@PathVariable String taskCode,
                                                    @RequestParam(defaultValue = "false") boolean dryRun,
                                                    HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        String triggeredBy = httpRequest.getHeader("X-Operator-Id");
        GraphSyncTask task = graphSyncService.retrySync(taskCode, dryRun, triggeredBy);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("task_code", task.getTaskCode());
        result.put("status", task.getStatus());
        return ApiResult.success(result);
    }

    /**
     * 列出同步任务。
     */
    @GetMapping("/sync-tasks")
    public ApiResult<List<Map<String, Object>>> listSyncTasks(@RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String limit,
                                                              HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("status", status);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(graphSyncService.listSyncTasks(filters));
    }

    /**
     * 获取同步任务详情。
     */
    @GetMapping("/sync-tasks/{taskCode}")
    public ApiResult<Map<String, Object>> getSyncTask(@PathVariable String taskCode,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        return ApiResult.success(graphSyncService.getSyncTask(taskCode));
    }
}
