package com.medkernel.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 审计事件发布器。
 *
 * <p>调用 {@link #publish(AuditAction, String, String, String)} 发布事件；
 * 事件通过 Spring {@link ApplicationEventPublisher} 在请求线程内同步分发到所有 {@code @EventListener}。
 *
 * <p>{@link LoggingAuditSink} 同步写入 INFO 日志便于本地开发观察；
 * {@code com.medkernel.shared.audit.persistence.AuditPersistenceSink} 在事务提交后异步落库
 * 并写入 SM3 哈希链（GA-ENG-BASE-04）。
 */
@Component
public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public AuditEvent publish(AuditAction action, String resourceType, String resourceId, String summary) {
        AuditEvent event = AuditEvent.of(action, resourceType, resourceId, summary);
        publisher.publishEvent(event);
        return event;
    }

    public void publish(AuditEvent event) {
        publisher.publishEvent(event);
    }

    @Component
    static class LoggingAuditSink {
        private static final Logger log = LoggerFactory.getLogger("audit");

        @org.springframework.context.event.EventListener
        public void onEvent(AuditEvent event) {
            log.info("AUDIT action={} resource={}/{} actor={} tenant={} traceId={} summary={}",
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.actorUserId(),
                event.orgScope() == null ? null : event.orgScope().tenantId(),
                event.traceId(),
                event.summary());
        }
    }
}
