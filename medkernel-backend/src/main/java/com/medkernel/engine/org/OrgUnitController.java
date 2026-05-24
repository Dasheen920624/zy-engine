package com.medkernel.engine.org;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;

/**
 * MedKernel v1.0 GA · 试点准备菜单下的组织单元 API。
 *
 * <p>对应宪法 §2.2 "试点准备 → 客户实施向导 / 租户开通" 菜单的后端能力。
 *
 * <p>所有端点都按 {@link com.medkernel.shared.context.RequestContext} 中的 tenantId 隐式过滤；
 * 客户端不允许传 tenantId 参数（防止越权伪造）。
 */
@RestController
@RequestMapping("/api/v1/tenant/org-units")
public class OrgUnitController {

    private final OrgUnitService service;

    public OrgUnitController(OrgUnitService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<PageResponse<OrgUnit>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageRequest req = new PageRequest(page, size, sort);
        return ApiResult.ok(service.listByCurrentTenant(req));
    }

    @GetMapping("/{code}")
    public ApiResult<OrgUnit> get(@PathVariable String code) {
        return ApiResult.ok(service.getByCurrentTenantAndCode(code));
    }

    @GetMapping("/by-level")
    public ApiResult<List<OrgUnit>> byLevel(@RequestParam OrgLevel level) {
        return ApiResult.ok(service.listByCurrentTenantAndLevel(level));
    }

    @GetMapping("/children-map")
    public ApiResult<Map<Long, List<OrgUnit>>> childrenMap() {
        return ApiResult.ok(service.childrenMapByCurrentTenant());
    }
}
