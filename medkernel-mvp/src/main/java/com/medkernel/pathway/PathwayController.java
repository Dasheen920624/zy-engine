package com.medkernel.pathway;

import com.medkernel.common.ApiResult;
import com.medkernel.dto.PatientNodeState;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Tag(name = "Pathway")
@RestController
@RequestMapping("/api")
public class PathwayController {
    private final PathwayService pathwayService;
    private final OrganizationContextService organizationContextService;

    public PathwayController(PathwayService pathwayService,
                             OrganizationContextService organizationContextService) {
        this.pathwayService = pathwayService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Create pathway")
    @PostMapping("/pathways")
    public ApiResult<Map<String, Object>> createPathway(@RequestBody Map<String, Object> config,
                                                         HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, config);
        return ApiResult.success(pathwayService.createPathway(config));
    }

    @Operation(summary = "List pathways")
    @GetMapping("/pathways")
    public ApiResult<Map<String, Object>> listPathways(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false, defaultValue = "1") String page,
            @RequestParam(required = false, defaultValue = "20") String size,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("search", search);
        filters.put("status", status);
        filters.put("dept", dept);
        filters.put("page", page);
        filters.put("size", size);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(pathwayService.listPathwaysFiltered(filters));
    }

    @Operation(summary = "Get pathway")
    @GetMapping("/pathways/{pathwayCode}")
    public ApiResult<Map<String, Object>> getPathway(@PathVariable String pathwayCode,
                                                     @RequestParam(required = false) String versionNo,
                                                     HttpServletRequest httpRequest) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("versionNo", versionNo);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(pathwayService.getPathway(pathwayCode, versionNo));
    }

    @Operation(summary = "Delete pathway")
    @DeleteMapping("/pathways/{pathwayCode}")
    public ApiResult<Map<String, Object>> deletePathway(@PathVariable String pathwayCode,
                                                         HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(pathwayService.deletePathway(pathwayCode));
    }

    @Operation(summary = "Diff pathway")
    @GetMapping("/pathways/{pathwayCode}/diff")
    public ApiResult<Map<String, Object>> diffPathway(@PathVariable String pathwayCode,
                                                      @RequestParam(name = "from") String fromVersion,
                                                      @RequestParam(name = "to") String toVersion,
                                                      HttpServletRequest httpRequest) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("fromVersion", fromVersion);
        filters.put("toVersion", toVersion);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(pathwayService.diffPathway(pathwayCode, fromVersion, toVersion));
    }

    @Operation(summary = "Publish pathway")
    @PostMapping("/pathways/{pathwayCode}/publish")
    public ApiResult<Map<String, Object>> publishPathway(@PathVariable String pathwayCode,
                                                         @RequestBody Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.publish(pathwayCode, request));
    }

    @Operation(summary = "Rollback pathway")
    @PostMapping("/pathways/{pathwayCode}/rollback")
    public ApiResult<Map<String, Object>> rollbackPathway(@PathVariable String pathwayCode,
                                                          @RequestBody(required = false) Map<String, Object> request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.rollback(pathwayCode, request));
    }

    @Operation(summary = "Candidates")
    @PostMapping("/patient-pathways/candidates")
    public ApiResult<List<RecommendationCard>> candidates(@RequestBody Map<String, Object> patientContext,
                                                           HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, patientContext);
        return ApiResult.success(pathwayService.candidates(patientContext));
    }

    @Operation(summary = "Admit")
    @PostMapping("/patient-pathways/admit")
    public ApiResult<PatientPathwayInstance> admit(@RequestBody Map<String, Object> request,
                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.admit(request, orgContext));
    }

    @Operation(summary = "Get instance")
    @GetMapping("/patient-pathways/{instanceId}")
    public ApiResult<Map<String, Object>> getInstance(@PathVariable String instanceId) {
        return ApiResult.success(pathwayService.getInstanceDetail(instanceId));
    }

    @Operation(summary = "Get node state")
    @GetMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}")
    public ApiResult<PatientNodeState> getNodeState(@PathVariable String instanceId,
                                                    @PathVariable String nodeCode,
                                                    HttpServletRequest httpRequest) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("instanceId", instanceId);
        filters.put("nodeCode", nodeCode);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(pathwayService.getNodeState(instanceId, nodeCode));
    }

    @Operation(summary = "Complete task")
    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/complete")
    public ApiResult<PatientTaskState> completeTask(@PathVariable String instanceId,
                                                    @PathVariable String nodeCode,
                                                    @PathVariable String taskCode,
                                                    @RequestBody(required = false) Map<String, Object> request,
                                                    HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.completeTask(instanceId, nodeCode, taskCode, request));
    }

    @Operation(summary = "Skip task")
    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/skip")
    public ApiResult<PatientTaskState> skipTask(@PathVariable String instanceId,
                                                @PathVariable String nodeCode,
                                                @PathVariable String taskCode,
                                                @RequestBody(required = false) Map<String, Object> request,
                                                HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.skipTask(instanceId, nodeCode, taskCode, request));
    }

    @Operation(summary = "Record variation")
    @PostMapping("/patient-pathways/{instanceId}/variations")
    public ApiResult<PathwayVariationRecord> recordVariation(@PathVariable String instanceId,
                                                             @RequestBody Map<String, Object> request,
                                                             HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.recordVariation(instanceId, request));
    }

    @Operation(summary = "Complete node")
    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/complete")
    public ApiResult<PatientPathwayInstance> completeNode(@PathVariable String instanceId,
                                                          @PathVariable String nodeCode,
                                                          @RequestBody(required = false) Map<String, Object> request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(pathwayService.completeNode(instanceId, nodeCode, request));
    }

    @Operation(summary = "List instances")
    @GetMapping("/pathway-instances")
    public ApiResult<List<PatientPathwayInstance>> listInstances(@RequestParam(required = false) String pathwayCode,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String patientId,
                                                                  @RequestParam(required = false) String encounterId,
                                                                  @RequestParam(required = false) String currentNodeCode,
                                                                  @RequestParam(required = false) String limit,
                                                                  HttpServletRequest request) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        filters.put("limit", limit);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.listInstances(filters));
    }

    @Operation(summary = "Instance summary")
    @GetMapping("/pathway-instances/summary")
    public ApiResult<Map<String, Object>> instanceSummary(@RequestParam(required = false) String pathwayCode,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String patientId,
                                                          @RequestParam(required = false) String encounterId,
                                                          @RequestParam(required = false) String currentNodeCode,
                                                          HttpServletRequest request) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.summarizeInstances(filters));
    }

    @Operation(summary = "Node completion")
    @GetMapping("/pathway-instances/node-completion")
    public ApiResult<Map<String, Object>> nodeCompletion(@RequestParam(required = false) String pathwayCode,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String patientId,
                                                         @RequestParam(required = false) String encounterId,
                                                         HttpServletRequest request) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.summarizeNodeCompletion(filters));
    }

    @Operation(summary = "Node stay duration")
    @GetMapping("/pathway-instances/node-stay-duration")
    public ApiResult<Map<String, Object>> nodeStayDuration(@RequestParam(required = false) String pathwayCode,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String patientId,
                                                           @RequestParam(required = false) String encounterId,
                                                           HttpServletRequest request) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.summarizeNodeStayDuration(filters));
    }

    @Operation(summary = "List variations")
    @GetMapping("/pathway-variations")
    public ApiResult<List<PathwayVariationRecord>> listVariations(@RequestParam(required = false) String pathwayCode,
                                                                  @RequestParam(required = false) String patientId,
                                                                  @RequestParam(required = false) String encounterId,
                                                                  @RequestParam(required = false) String variationType,
                                                                  @RequestParam(required = false) String nodeCode,
                                                                  @RequestParam(required = false) String instanceId,
                                                                  @RequestParam(required = false) String limit,
                                                                  HttpServletRequest request) {
        Map<String, String> filters = filterMap(pathwayCode, patientId, encounterId,
                variationType, nodeCode, instanceId, limit);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.listVariations(filters));
    }

    @Operation(summary = "Variation summary")
    @GetMapping("/pathway-variations/summary")
    public ApiResult<Map<String, Object>> variationSummary(@RequestParam(required = false) String pathwayCode,
                                                           @RequestParam(required = false) String patientId,
                                                           @RequestParam(required = false) String encounterId,
                                                           @RequestParam(required = false) String variationType,
                                                           @RequestParam(required = false) String nodeCode,
                                                           @RequestParam(required = false) String instanceId,
                                                           HttpServletRequest request) {
        Map<String, String> filters = filterMap(pathwayCode, patientId, encounterId,
                variationType, nodeCode, instanceId, null);
        addOrgFilters(filters, request);
        return ApiResult.success(pathwayService.summarizeVariations(filters));
    }

    private Map<String, String> filterMap(String pathwayCode, String patientId, String encounterId,
                                          String variationType, String nodeCode, String instanceId, String limit) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("variationType", variationType);
        filters.put("nodeCode", nodeCode);
        filters.put("instanceId", instanceId);
        filters.put("limit", limit);
        return filters;
    }

    private void addOrgFilters(Map<String, String> filters, HttpServletRequest request) {
        organizationContextService.applyExplicitFilters(filters, request);
    }
}
