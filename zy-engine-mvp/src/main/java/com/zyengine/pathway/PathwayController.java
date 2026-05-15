package com.zyengine.pathway;

import com.zyengine.common.ApiResult;
import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.RecommendationCard;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/patient-pathways/{instanceId}/nodes/{nodeCode}/complete")
    public ApiResult<PatientPathwayInstance> completeNode(@PathVariable String instanceId,
                                                          @PathVariable String nodeCode,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(pathwayService.completeNode(instanceId, nodeCode));
    }
}

