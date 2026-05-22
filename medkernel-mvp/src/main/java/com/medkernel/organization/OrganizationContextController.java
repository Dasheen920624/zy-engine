package com.medkernel.organization;

import com.medkernel.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Tag(name = "Organization Context")
@RestController
@RequestMapping("/api/system")
public class OrganizationContextController {
    private final OrganizationContextService organizationContextService;

    public OrganizationContextController(OrganizationContextService organizationContextService) {
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Org context")
    @GetMapping("/org-context")
    public ApiResult<Map<String, Object>> orgContext(HttpServletRequest request) {
        return ApiResult.success(organizationContextService.orgContextView(request));
    }
}
