package com.medkernel.engine.knowledge;

import java.util.List;

import org.springframework.stereotype.Service;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 知识身份业务服务。
 *
 * <p>覆盖详细规范 §1797-1806：
 * <ul>
 *   <li>列表（分页 + 域/专科/状态/关键词筛选）</li>
 *   <li>详情（按 id / identity_code）</li>
 *   <li>活跃版本快捷查询</li>
 *   <li>历史 lineage（身份 + supersession 链 + 所有版本，按时间排序）</li>
 * </ul>
 *
 * <p>所有方法不接受 tenantId 入参，统一从 {@link RequestContext} 抽取。
 */
@Service
public class KnowledgeIdentityService {

    private final KnowledgeIdentityRepository identityRepository;
    private final KnowledgeAssetVersionRepository versionRepository;
    private final KnowledgeSupersessionRepository supersessionRepository;

    public KnowledgeIdentityService(KnowledgeIdentityRepository identityRepository,
                                    KnowledgeAssetVersionRepository versionRepository,
                                    KnowledgeSupersessionRepository supersessionRepository) {
        this.identityRepository = identityRepository;
        this.versionRepository = versionRepository;
        this.supersessionRepository = supersessionRepository;
    }

    public PageResponse<KnowledgeIdentity> page(PageRequest request, KnowledgeIdentityFilter filter) {
        String tenantId = requireCurrentTenant();
        int offset = request.offset();
        int size = request.safeSize();

        String domain = filter.domain() == null ? null : filter.domain().name();
        String status = filter.status() == null ? null : filter.status().name();
        String keyword = normalizeKeyword(filter.keyword());

        long total = identityRepository.countByFilter(tenantId, domain, filter.specialtyId(), status, keyword);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        List<KnowledgeIdentity> items = identityRepository.pageByFilter(
            tenantId, domain, filter.specialtyId(), status, keyword, offset, size);
        return PageResponse.of(items, request, total);
    }

    public KnowledgeIdentity get(Long id) {
        String tenantId = requireCurrentTenant();
        return identityRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> ApiException.notFound("知识身份 id=" + id));
    }

    public KnowledgeIdentity getByCode(String identityCode) {
        String tenantId = requireCurrentTenant();
        return identityRepository.findByTenantIdAndIdentityCode(tenantId, identityCode)
            .orElseThrow(() -> ApiException.notFound("知识身份 code=" + identityCode));
    }

    public KnowledgeAssetVersion getActiveVersion(Long identityId) {
        String tenantId = requireCurrentTenant();
        // 先校验身份存在 + 同租户
        identityRepository.findByTenantIdAndId(tenantId, identityId)
            .orElseThrow(() -> ApiException.notFound("知识身份 id=" + identityId));
        return versionRepository.findActiveByIdentity(tenantId, identityId)
            .orElseThrow(() -> ApiException.notFound("知识身份 id=" + identityId + " 当前无 ACTIVE 版本"));
    }

    public KnowledgeLineage getLineage(Long identityId) {
        String tenantId = requireCurrentTenant();
        KnowledgeIdentity identity = identityRepository.findByTenantIdAndId(tenantId, identityId)
            .orElseThrow(() -> ApiException.notFound("知识身份 id=" + identityId));
        List<KnowledgeAssetVersion> versions = versionRepository.listByIdentity(tenantId, identityId);
        List<KnowledgeSupersession> supersessions =
            supersessionRepository.findByTenantIdAndIdentityIdOrderByTransitionedAtAsc(tenantId, identityId);
        return new KnowledgeLineage(identity, versions, supersessions);
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private String normalizeKeyword(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        // SQL LIKE：包裹 %
        return "%" + trimmed + "%";
    }
}
