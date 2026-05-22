package com.medkernel.pathway;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Tag(name = "Pathway Draft")
@RestController
@RequestMapping("/api/pathways")
public class PathwayDraftController {
    private final PathwayValidator pathwayValidator;
    private final PathwayService pathwayService;
    private final OrganizationContextService organizationContextService;

    public PathwayDraftController(PathwayValidator pathwayValidator,
                                  PathwayService pathwayService,
                                  OrganizationContextService organizationContextService) {
        this.pathwayValidator = pathwayValidator;
        this.pathwayService = pathwayService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Save draft")
    @PutMapping("/{pathwayCode}/draft")
    public ApiResult<Map<String, Object>> saveDraft(
            @PathVariable String pathwayCode,
            @RequestBody Map<String, Object> draft,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, draft);
        Map<String, Object> result = pathwayService.saveDraft(pathwayCode, draft, orgContext.getTenantId());
        return ApiResult.success(result);
    }

    @Operation(summary = "Validate")
    @PostMapping("/{pathwayCode}/validate")
    public ApiResult<Map<String, Object>> validate(
            @PathVariable String pathwayCode,
            @RequestBody Map<String, Object> draft) {
        Map<String, Object> result = pathwayValidator.validate(draft);
        return ApiResult.success(result);
    }

    @Operation(summary = "Submit review")
    @PostMapping("/{pathwayCode}/submit-review")
    public ApiResult<Map<String, Object>> submitReview(
            @PathVariable String pathwayCode,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = request != null
                ? organizationContextService.resolveWithBody(httpRequest, request)
                : organizationContextService.resolve(httpRequest);
        Map<String, Object> result = pathwayService.submitReview(pathwayCode, orgContext.getTenantId());
        return ApiResult.success(result);
    }
}
