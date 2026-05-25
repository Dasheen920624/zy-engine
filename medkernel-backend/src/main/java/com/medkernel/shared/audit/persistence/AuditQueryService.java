package com.medkernel.shared.audit.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.medkernel.shared.api.CursorRequest;
import com.medkernel.shared.api.CursorResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 审计读模型查询服务（GA-ENG-BASE-04）。
 *
 * <p>从 {@link RequestContext} 强制取出 tenantId 作为查询作用域；
 * 调用方任何尝试传入 tenantId 的 API 都不被接受。
 */
@Service
public class AuditQueryService {

    private final AuditEventRepository repository;

    public AuditQueryService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public CursorResponse<AuditEventRecord> list(CursorRequest cursor,
                                                 String action,
                                                 String resourceType,
                                                 String actorUserId,
                                                 Instant from,
                                                 Instant to) {
        String tenantId = requireCurrentTenant();
        int size = cursor.safeSize();
        Long cursorId = parseCursor(cursor.cursor());

        AuditEventQuery query = new AuditEventQuery(
            action, resourceType, actorUserId, from, to, cursorId, size);
        List<AuditEventRecord> rows = repository.findPage(tenantId, query);

        if (rows.size() > size) {
            List<AuditEventRecord> page = rows.subList(0, size);
            String nextCursor = String.valueOf(page.get(page.size() - 1).id());
            return CursorResponse.of(page, nextCursor);
        }
        return CursorResponse.of(rows, null);
    }

    public Optional<AuditEventRecord> findByEventId(String eventId) {
        String tenantId = requireCurrentTenant();
        return repository.findByEventId(tenantId, eventId);
    }

    private static Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor.trim());
        } catch (NumberFormatException ex) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "非法 cursor，必须为数字 id");
        }
    }

    private static String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }
}
