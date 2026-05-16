package com.zyengine.pathway;

import com.zyengine.common.ApiResult;
import com.zyengine.dto.PatientNodeState;
import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.PatientTaskState;
import com.zyengine.dto.PathwayVariationRecord;
import com.zyengine.dto.RecommendationCard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PathwayController {
    private final PathwayService pathwayService;

    public PathwayController(PathwayService pathwayService) {
        this.pathwayService = pathwayService;
    }

    @PostMapping("/pathways")
    public ApiResult<Map<String, Object>> createPathway(@RequestBody Map<String, Object> config) {
        return ApiResult.success(pathwayService.createPathway(config));
    }

    @GetMapping("/pathways")
    public ApiResult<List<Map<String, Object>>> listPathways() {
        return ApiResult.success(pathwayService.listPathways());
    }

    @GetMapping("/pathways/{pathwayCode}")
    public ApiResult<Map<String, Object>> getPathway(@PathVariable String pathwayCode,
                                                     @RequestParam(required = false) String versionNo) {
        return ApiResult.success(pathwayService.getPathway(pathwayCode, versionNo));
    }

    @GetMapping("/pathways/{pathwayCode}/diff")
    public ApiResult<Map<String, Object>> diffPathway(@PathVariable String pathwayCode,
                                                      @RequestParam(name = "from") String fromVersion,
                                                      @RequestParam(name = "to") String toVersion) {
        return ApiResult.success(pathwayService.diffPathway(pathwayCode, fromVersion, toVersion));
    }

    @PostMapping("/pathways/{pathwayCode}/publish")
    public ApiResult<Map<String, Object>> publishPathway(@PathVariable String pathwayCode,
                                                         @RequestBody Map<String, Object> request) {
        return ApiResult.success(pathwayService.publish(pathwayCode, request));
    }

    @PostMapping("/patient-pathways/candidates")
    public ApiResult<List<RecommendationCard>> candidates(@RequestBody Map<String, Object> patientContext) {
        return ApiResult.success(pathwayService.candidates(patientContext));
    }

    @PostMapping("/patient-pathways/admit")
    public ApiResult<PatientPathwayInstance> admit(@RequestBody Map<String, Object> request) {
        return ApiResult.success(pathwayService.admit(request));
    }

    @GetMapping("/patient-pathways/{instanceId}")
    public ApiResult<Map<String, Object>> getInstance(@PathVariable String instanceId) {
        return ApiResult.success(pathwayService.getInstanceDetail(instanceId));
    }

    @GetMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}")
    public ApiResult<PatientNodeState> getNodeState(@PathVariable String instanceId,
                                                    @PathVariable String nodeCode) {
        return ApiResult.success(pathwayService.getNodeState(instanceId, nodeCode));
    }

    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/complete")
    public ApiResult<PatientTaskState> completeTask(@PathVariable String instanceId,
                                                    @PathVariable String nodeCode,
                                                    @PathVariable String taskCode,
                                                    @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(pathwayService.completeTask(instanceId, nodeCode, taskCode, request));
    }

    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/tasks/{taskCode}/skip")
    public ApiResult<PatientTaskState> skipTask(@PathVariable String instanceId,
                                                @PathVariable String nodeCode,
                                                @PathVariable String taskCode,
                                                @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(pathwayService.skipTask(instanceId, nodeCode, taskCode, request));
    }

    @PostMapping("/patient-pathways/{instanceId}/variations")
    public ApiResult<PathwayVariationRecord> recordVariation(@PathVariable String instanceId,
                                                             @RequestBody Map<String, Object> request) {
        return ApiResult.success(pathwayService.recordVariation(instanceId, request));
    }

    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/complete")
    public ApiResult<PatientPathwayInstance> completeNode(@PathVariable String instanceId,
                                                          @PathVariable String nodeCode,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(pathwayService.completeNode(instanceId, nodeCode, request));
    }

    @GetMapping("/pathway-instances")
    public ApiResult<List<PatientPathwayInstance>> listInstances(@RequestParam(required = false) String pathwayCode,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String patientId,
                                                                  @RequestParam(required = false) String encounterId,
                                                                  @RequestParam(required = false) String currentNodeCode,
                                                                  @RequestParam(required = false) String limit) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        filters.put("limit", limit);
        return ApiResult.success(pathwayService.listInstances(filters));
    }

    @GetMapping("/pathway-instances/summary")
    public ApiResult<Map<String, Object>> instanceSummary(@RequestParam(required = false) String pathwayCode,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String patientId,
                                                          @RequestParam(required = false) String encounterId,
                                                          @RequestParam(required = false) String currentNodeCode) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        return ApiResult.success(pathwayService.summarizeInstances(filters));
    }

    @GetMapping("/pathway-instances/node-completion")
    public ApiResult<Map<String, Object>> nodeCompletion(@RequestParam(required = false) String pathwayCode,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String patientId,
                                                         @RequestParam(required = false) String encounterId) {
        Map<String, String> filters = new java.util.LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        return ApiResult.success(pathwayService.summarizeNodeCompletion(filters));
    }

    @GetMapping("/pathway-variations")
    public ApiResult<List<PathwayVariationRecord>> listVariations(@RequestParam(required = false) String pathwayCode,
                                                                  @RequestParam(required = false) String patientId,
                                                                  @RequestParam(required = false) String encounterId,
                                                                  @RequestParam(required = false) String variationType,
                                                                  @RequestParam(required = false) String nodeCode,
                                                                  @RequestParam(required = false) String instanceId,
                                                                  @RequestParam(required = false) String limit) {
        return ApiResult.success(pathwayService.listVariations(filterMap(pathwayCode, patientId, encounterId,
                variationType, nodeCode, instanceId, limit)));
    }

    @GetMapping("/pathway-variations/summary")
    public ApiResult<Map<String, Object>> variationSummary(@RequestParam(required = false) String pathwayCode,
                                                           @RequestParam(required = false) String patientId,
                                                           @RequestParam(required = false) String encounterId,
                                                           @RequestParam(required = false) String variationType,
                                                           @RequestParam(required = false) String nodeCode,
                                                           @RequestParam(required = false) String instanceId) {
        return ApiResult.success(pathwayService.summarizeVariations(filterMap(pathwayCode, patientId, encounterId,
                variationType, nodeCode, instanceId, null)));
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
}
