package com.zyengine.organization;

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
@RequestMapping("/api/organizations")
public class OrganizationDirectoryController {
    private final OrganizationDirectoryService organizationDirectoryService;

    public OrganizationDirectoryController(OrganizationDirectoryService organizationDirectoryService) {
        this.organizationDirectoryService = organizationDirectoryService;
    }

    @PostMapping
    public ApiResult<Map<String, Object>> importUnits(@RequestBody Object request) {
        return ApiResult.success(organizationDirectoryService.importUnits(request));
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> listUnits(@RequestParam(required = false) String tenantId,
                                                         @RequestParam(required = false) String tenant_id,
                                                         @RequestParam(required = false) String level,
                                                         @RequestParam(required = false) String parentLevel,
                                                         @RequestParam(required = false) String parent_level,
                                                         @RequestParam(required = false) String parentCode,
                                                         @RequestParam(required = false) String parent_code,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("level", level);
        filters.put("parentLevel", parentLevel == null ? parent_level : parentLevel);
        filters.put("parentCode", parentCode == null ? parent_code : parentCode);
        filters.put("status", status);
        filters.put("limit", limit);
        return ApiResult.success(organizationDirectoryService.listUnits(filters));
    }

    @GetMapping("/tree")
    public ApiResult<Map<String, Object>> tree(@RequestParam(required = false) String tenantId,
                                               @RequestParam(required = false) String tenant_id,
                                               @RequestParam(required = false) String rootLevel,
                                               @RequestParam(required = false) String root_level,
                                               @RequestParam(required = false) String rootCode,
                                               @RequestParam(required = false) String root_code) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("rootLevel", rootLevel == null ? root_level : rootLevel);
        filters.put("rootCode", rootCode == null ? root_code : rootCode);
        return ApiResult.success(organizationDirectoryService.tree(filters));
    }

    @GetMapping("/{level}/{code}")
    public ApiResult<Map<String, Object>> getUnit(@PathVariable String level,
                                                  @PathVariable String code,
                                                  @RequestParam(required = false) String tenantId,
                                                  @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(organizationDirectoryService.getUnit(level, code,
                tenantId == null ? tenant_id : tenantId));
    }
}
