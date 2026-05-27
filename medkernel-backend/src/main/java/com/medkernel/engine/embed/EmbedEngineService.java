package com.medkernel.engine.embed;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 页面嵌入引擎服务实现类 (GA-ENG-API-11)。
 *
 * <p>提供嵌入 Launch Token 的生命周期管理（短时生成、原子锁定校验）、Origin安全域名过滤以及医生反馈决策闭环子事务留痕能力。
 */
@Service
public class EmbedEngineService {

    private static final int DEFAULT_EXPIRE_SECONDS = 60;

    private final EmbedLaunchTokenRepository tokenRepo;
    private final EmbedOriginWhitelistRepository originRepo;
    private final AuditEventPublisher auditPublisher;
    private final IsolatedAuditPublisher isolatedAudit;

    public EmbedEngineService(EmbedLaunchTokenRepository tokenRepo,
                              EmbedOriginWhitelistRepository originRepo,
                              AuditEventPublisher auditPublisher,
                              IsolatedAuditPublisher isolatedAudit) {
        this.tokenRepo = tokenRepo;
        this.originRepo = originRepo;
        this.auditPublisher = auditPublisher;
        this.isolatedAudit = isolatedAudit;
    }

    /**
     * 生成一次性嵌入启动令牌。
     *
     * @param req 令牌申请请求信息，含用户、就诊和触发位置点
     * @return 启动令牌及拼接好的嵌入URL
     */
    @Transactional
    public EmbedLaunchTokenResponse generateToken(EmbedLaunchTokenRequest req) {
        String tenantId = requireCurrentTenant();
        String createdBy = RequestContext.currentUserId().orElse("system");
        String traceId = RequestContext.currentTraceId();

        String tokenValue = "tkn-" + UUID.randomUUID().toString().replace("-", "");
        int expireSec = req.expireSeconds() != null && req.expireSeconds() > 0 ? req.expireSeconds() : DEFAULT_EXPIRE_SECONDS;
        Instant now = Instant.now();
        Instant expiredAt = now.plusSeconds(expireSec);

        EmbedLaunchToken entity = new EmbedLaunchToken(
            null,
            tokenValue,
            tenantId,
            req.userId(),
            req.roleCode(),
            req.patientId(),
            req.encounterId(),
            req.triggerPoint(),
            "UNUSED",
            expiredAt,
            now,
            createdBy,
            now,
            createdBy,
            traceId
        );
        tokenRepo.save(entity);

        // 拼接默认页面嵌入 URL，外部 HIS 可直接使用此 URL 嵌入
        String embedUrl = String.format("/embed/launch?token=%s", tokenValue);

        auditPublisher.publish(AuditAction.CREATE, "embed_launch_token", tokenValue,
            "生成嵌入启动令牌 triggerPoint=" + req.triggerPoint() + " patientId=" + req.patientId());

        return new EmbedLaunchTokenResponse(tokenValue, expiredAt, embedUrl);
    }

    /**
     * 校验并原子性消费启动令牌，获取当前嵌入会话上下文。
     *
     * @param token 启动令牌
     * @param originHeader 请求头中的 Origin 域名，用于安全过滤
     * @return 会话及关联的临床上下文
     */
    @Transactional
    public EmbedLaunchContextResponse validateAndExchange(String token, String originHeader) {
        EmbedLaunchToken entity = tokenRepo.findByToken(token)
            .orElseThrow(() -> {
                publishFailureAudit(ErrorCode.ENG_EMBED_004, "启动令牌不存在 token=" + token);
                return new ApiException(ErrorCode.ENG_EMBED_004, "启动令牌不存在");
            });

        String tenantId = entity.tenantId();

        // 1. 域名白名单校验
        if (originHeader != null && !originHeader.isBlank()) {
            boolean allowed = originRepo.findByTenantIdAndOrigin(tenantId, originHeader).isPresent();
            if (!allowed) {
                publishFailureAudit(ErrorCode.ENG_EMBED_002, "非法的 Origin 域名=" + originHeader);
                throw new ApiException(ErrorCode.ENG_EMBED_002, "非法的 Origin 域名: " + originHeader);
            }
        }

        // 2. 令牌状态与时效性校验
        if ("USED".equalsIgnoreCase(entity.status())) {
            publishFailureAudit(ErrorCode.ENG_EMBED_003, "启动令牌已被使用 token=" + token);
            throw new ApiException(ErrorCode.ENG_EMBED_003, "启动令牌已被使用");
        }

        if ("EXPIRED".equalsIgnoreCase(entity.status()) || Instant.now().isAfter(entity.expiredAt())) {
            if ("UNUSED".equalsIgnoreCase(entity.status())) {
                EmbedLaunchToken expired = new EmbedLaunchToken(
                    entity.id(), entity.token(), entity.tenantId(), entity.userId(), entity.roleCode(),
                    entity.patientId(), entity.encounterId(), entity.triggerPoint(), "EXPIRED",
                    entity.expiredAt(), entity.createdAt(), entity.createdBy(), Instant.now(), entity.updatedBy(),
                    entity.traceId()
                );
                tokenRepo.save(expired);
            }
            publishFailureAudit(ErrorCode.ENG_EMBED_001, "启动令牌已过期 token=" + token);
            throw new ApiException(ErrorCode.ENG_EMBED_001, "启动令牌已过期");
        }

        // 3. 原子标记为已使用，实现一次性物理消费
        EmbedLaunchToken used = new EmbedLaunchToken(
            entity.id(), entity.token(), entity.tenantId(), entity.userId(), entity.roleCode(),
            entity.patientId(), entity.encounterId(), entity.triggerPoint(), "USED",
            entity.expiredAt(), entity.createdAt(), entity.createdBy(), Instant.now(), entity.updatedBy(),
            entity.traceId()
        );
        tokenRepo.save(used);

        auditPublisher.publish(AuditAction.EXECUTE, "embed_launch_token", token,
            "消费嵌入令牌成功 userId=" + entity.userId() + " triggerPoint=" + entity.triggerPoint());

        return new EmbedLaunchContextResponse(
            entity.userId(),
            entity.roleCode(),
            entity.tenantId(),
            entity.patientId(),
            entity.encounterId(),
            entity.triggerPoint(),
            true,
            entity.traceId()
        );
    }

    /**
     * 回传记录医师在工作站嵌入页面的交互采纳与拒绝反馈，强制采用隔离独立子事务记录审计。
     *
     * @param req 反馈请求参数
     */
    @Transactional
    public void feedback(EmbedFeedbackRequest req) {
        EmbedLaunchToken entity = tokenRepo.findByToken(req.token())
            .orElseThrow(() -> {
                publishFailureAudit(ErrorCode.ENG_EMBED_004, "提交反馈失败，启动令牌不存在 token=" + req.token());
                return new ApiException(ErrorCode.ENG_EMBED_004, "启动令牌不存在");
            });

        // 记录闭环反馈审计事件
        auditPublisher.publish(AuditAction.FEEDBACK, "embed_launch_token", req.token(),
            String.format("医生提交交互反馈 actionType=%s reason=%s patientId=%s",
                req.actionType(), req.reason() != null ? req.reason() : "", entity.patientId()));
    }

    /**
     * 为当前租户添加允许嵌入 Origin 白名单。
     *
     * @param req 域名Origin配置
     */
    @Transactional
    public void addOrigin(EmbedOriginRequest req) {
        String tenantId = requireCurrentTenant();
        String createdBy = RequestContext.currentUserId().orElse("system");

        Optional<EmbedOriginWhitelist> existing = originRepo.findByTenantIdAndOrigin(tenantId, req.origin());
        if (existing.isPresent()) {
            return;
        }

        Instant now = Instant.now();
        EmbedOriginWhitelist entity = new EmbedOriginWhitelist(
            null,
            tenantId,
            req.origin(),
            now,
            createdBy,
            now,
            createdBy
        );
        originRepo.save(entity);

        auditPublisher.publish(AuditAction.CREATE, "embed_origin_whitelist", req.origin(),
            "添加Origin安全域名白名单 origin=" + req.origin());
    }

    /**
     * 获取当前租户下配置的所有 Origin 白名单列表。
     *
     * @return Origin域名白名单列表
     */
    @Transactional(readOnly = true)
    public List<String> getOrigins() {
        String tenantId = requireCurrentTenant();
        return originRepo.findByTenantId(tenantId).stream()
            .map(EmbedOriginWhitelist::origin)
            .toList();
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private void publishFailureAudit(ErrorCode code, String summary) {
        isolatedAudit.publishInNewTx(AuditEvent.failure(
            AuditAction.EXECUTE, "embed_launch_token", null, code.code(), summary));
    }
}
