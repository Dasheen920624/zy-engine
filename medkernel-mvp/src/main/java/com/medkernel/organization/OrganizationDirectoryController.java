package com.medkernel.organization;

import com.medkernel.common.ApiResult;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Organization Directory")
@RestController
@RequestMapping("/api/organizations")
public class OrganizationDirectoryController {
    private final OrganizationDirectoryService organizationDirectoryService;
    private final OrganizationContextService organizationContextService;

    public OrganizationDirectoryController(OrganizationDirectoryService organizationDirectoryService,
                                           OrganizationContextService organizationContextService) {
        this.organizationDirectoryService = organizationDirectoryService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Import units")
    @PostMapping
    public ApiResult<Map<String, Object>> importUnits(@RequestBody Object request,
                                                       HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(organizationDirectoryService.importUnits(request));
    }

    @Operation(summary = "List units")
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listUnits(@RequestParam(required = false) String level,
                                                         @RequestParam(required = false) String parentLevel,
                                                         @RequestParam(required = false) String parent_level,
                                                         @RequestParam(required = false) String parentCode,
                                                         @RequestParam(required = false) String parent_code,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String limit,
                                                         HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("level", level);
        filters.put("parentLevel", parentLevel == null ? parent_level : parentLevel);
        filters.put("parentCode", parentCode == null ? parent_code : parentCode);
        filters.put("status", status);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(organizationDirectoryService.listUnits(filters));
    }

    @Operation(summary = "Tree")
    @GetMapping("/tree")
    public ApiResult<Map<String, Object>> tree(@RequestParam(required = false) String rootLevel,
                                               @RequestParam(required = false) String root_level,
                                               @RequestParam(required = false) String rootCode,
                                               @RequestParam(required = false) String root_code,
                                               HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("rootLevel", rootLevel == null ? root_level : rootLevel);
        filters.put("rootCode", rootCode == null ? root_code : rootCode);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(organizationDirectoryService.tree(filters));
    }

    @Operation(summary = "Get unit")
    @GetMapping("/{level}/{code}")
    public ApiResult<Map<String, Object>> getUnit(@PathVariable String level,
                                                  @PathVariable String code,
                                                  HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(organizationDirectoryService.getUnit(level, code, filters.get("tenantId")));
    }
}
