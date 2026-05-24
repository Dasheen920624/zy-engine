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
 * <p>当前 PR 只接入 {@link LoggingAuditSink} 写入 INFO 日志，便于本地开发观察审计轨迹。
 * GA-ENG-BASE-04 任务实施时将新增 {@code AuditPersistenceSink} 将事件落库到 {@code audit_event} 表，
 * 并补上 SM3 签名链。
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
