package com.medkernel.shared.audit.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.observability.BusinessMetrics;

/**
 * 审计事件持久化 Sink（GA-ENG-BASE-04）。
 *
 * <p>监听 {@link AuditEvent}：
 * <ul>
 *   <li>若发布在事务内：{@code AFTER_COMMIT} 阶段交给 {@link AuditChainWriter} 落库</li>
 *   <li>若发布在事务外（如直接接口调用）：立即同步落库，避免事件丢失</li>
 * </ul>
 *
 * <p>失败策略：捕获所有持久化异常，递增
 * {@link BusinessMetrics#incAuditPersistenceFailures()} 并写 ERROR 日志；
 * 不向业务调用方抛出，避免审计存储抖动连累主链路。
 */
@Component
public class AuditPersistenceSink {

    private static final Logger log = LoggerFactory.getLogger(AuditPersistenceSink.class);

    private final AuditChainWriter writer;
    private final BusinessMetrics metrics;

    public AuditPersistenceSink(AuditChainWriter writer, BusinessMetrics metrics) {
        this.writer = writer;
        this.metrics = metrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(AuditEvent event) {
        safePersist(event);
    }

    /** 兜底：当事件发布时没有活动事务，{@code @TransactionalEventListener} 不会触发，
     *  这里通过普通 {@link EventListener} 同步落库，保证事件必然进入存储。 */
    @EventListener
    public void onNoTransaction(AuditEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return; // let AFTER_COMMIT handle it
        }
        safePersist(event);
    }

    private void safePersist(AuditEvent event) {
        try {
            writer.persist(event);
            metrics.incAuditChainSigned();
        } catch (RuntimeException ex) {
            metrics.incAuditPersistenceFailures();
            log.error(
                "AUDIT_PERSISTENCE_FAILED eventId={} action={} resource={}/{} actor={} traceId={} cause={}",
                event.id(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.actorUserId(),
                event.traceId(),
                ex.toString(),
                ex);
        }
    }
}
