package com.medkernel.shared.audit;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * MedKernel 共享审计事件（{@code com.medkernel.shared.audit.AuditEvent}）。
 *
 * <p>与 {@code com.medkernel.compliance.audit.AuditEvent}（合规模块的对外展示 DTO）区分：
 * <ul>
 *   <li>本类是引擎内部统一发出的事件契约，承载完整组织/追踪上下文</li>
 *   <li>{@code compliance.audit.AuditEvent} 是面向客户端的审计列表 DTO，由本事件投影而来</li>
 * </ul>
 *
 * <p>持久化 + SM3 哈希链由 {@code com.medkernel.shared.audit.persistence.AuditPersistenceSink}
 * 在 {@code AFTER_COMMIT} 阶段完成（GA-ENG-BASE-04）。
 *
 * <p>{@code outcome} 区分业务成功/失败（spec §6.1 第 5 条）：{@link #of} 默认 SUCCESS；
 * 业务失败用 {@link #failure} 工厂发出 outcome=FAILED + errorCode。outcome/errorCode
 * 是审计链外的元数据，不参与签名计算以保证既有审计签名继续可验。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
    String id,
    String traceId,
    Instant occurredAt,
    String actorUserId,
    AuditAction action,
    String resourceType,
    String resourceId,
    String summary,
    String payloadDigest,
    OrgScope orgScope,
    String outcome,
    String errorCode
) {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILED  = "FAILED";

    public static AuditEvent of(AuditAction action, String resourceType, String resourceId, String summary) {
        return new AuditEvent(
            UUID.randomUUID().toString(),
            RequestContext.currentTraceId(),
            Instant.now(),
            RequestContext.currentUserId().orElse(null),
            action, resourceType, resourceId, summary,
            null,
            RequestContext.currentOrgScope(),
            OUTCOME_SUCCESS, null
        );
    }

    /** 业务失败留痕：发出 outcome=FAILED + errorCode 的 audit。 */
    public static AuditEvent failure(AuditAction action, String resourceType, String resourceId,
                                     String errorCode, String summary) {
        return new AuditEvent(
            UUID.randomUUID().toString(),
            RequestContext.currentTraceId(),
            Instant.now(),
            RequestContext.currentUserId().orElse(null),
            action, resourceType, resourceId, summary,
            null,
            RequestContext.currentOrgScope(),
            OUTCOME_FAILED, errorCode
        );
    }

    public AuditEvent withPayloadDigest(String digest) {
        return new AuditEvent(id, traceId, occurredAt, actorUserId, action,
            resourceType, resourceId, summary, digest, orgScope, outcome, errorCode);
    }
}
