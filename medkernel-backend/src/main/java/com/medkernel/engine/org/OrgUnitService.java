package com.medkernel.engine.org;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 组织单元业务服务。
 *
 * <p>所有方法在执行前都调用 {@link #requireCurrentTenant()}，从 {@link RequestContext} 取出 tenantId；
 * 未鉴权或 JWT 缺少 {@code tenant_id} claim 的请求会被翻译为 {@link ErrorCode#TENANT_CONTEXT_MISSING} 400 错误，
 * 由 {@code GlobalExceptionHandler} 统一返回 ApiResult。
 *
 * <p>避免在业务方法签名上暴露 tenantId 参数（客户端永远不应能伪造）。
 */
@Service
public class OrgUnitService {

    private final OrgUnitRepository repository;

    public OrgUnitService(OrgUnitRepository repository) {
        this.repository = repository;
    }

    public PageResponse<OrgUnit> listByCurrentTenant(PageRequest request) {
        String tenantId = requireCurrentTenant();
        int page = request.safePage();
        int size = request.safeSize();
        int offset = request.offset();

        long total = repository.countByTenantId(tenantId);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        List<OrgUnit> rows = repository.pageByTenantId(tenantId, offset, size);
        boolean hasNext = (long) page * size < total;
        return new PageResponse<>(rows, page, size, total, hasNext, false);
    }

    public OrgUnit getByCurrentTenantAndCode(String code) {
        String tenantId = requireCurrentTenant();
        return repository.findByTenantIdAndCode(tenantId, code)
            .orElseThrow(() -> ApiException.notFound("组织单元 code=" + code));
    }

    public List<OrgUnit> listByCurrentTenantAndLevel(OrgLevel level) {
        String tenantId = requireCurrentTenant();
        return repository.findByTenantIdAndLevelOrderByCodeAsc(tenantId, level);
    }

    /**
     * 返回按 parentId → children 的扁平映射，供前端按需展开成树。
     *
     * <p>不在服务端构建嵌套树，避免大组织在序列化阶段递归过深和分页缺失；
     * 客户端按需在抽屉里只渲染当前节点的直接子节点（懒加载）。
     */
    public Map<Long, List<OrgUnit>> childrenMapByCurrentTenant() {
        String tenantId = requireCurrentTenant();
        List<OrgUnit> all = repository.findByTenantIdOrderByLevelAscCodeAsc(tenantId);
        Map<Long, List<OrgUnit>> map = new HashMap<>();
        // root 节点（parentId == null）放到 key = 0L
        for (OrgUnit unit : all) {
            Long parentKey = unit.parentId() == null ? 0L : unit.parentId();
            map.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(unit);
        }
        return map;
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }
}
