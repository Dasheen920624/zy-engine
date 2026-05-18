package com.medkernel.organization;

import com.medkernel.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class OrganizationContextController {
    private final OrganizationContextService organizationContextService;

    public OrganizationContextController(OrganizationContextService organizationContextService) {
        this.organizationContextService = organizationContextService;
    }

    @GetMapping("/org-context")
    public ApiResult<Map<String, Object>> orgContext(HttpServletRequest request) {
        return ApiResult.success(organizationContextService.orgContextView(request));
    }
}
