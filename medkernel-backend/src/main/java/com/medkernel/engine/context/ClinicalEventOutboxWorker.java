package com.medkernel.engine.context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.observability.TraceIdPropagator;

/**
 * DB-Only 临床事件 outbox worker。
 */
@Component
public class ClinicalEventOutboxWorker {

    private final ClinicalEventOutboxRepository outbox;
    private final ClinicalEventProcessor processor;
    private final ClinicalEventProperties properties;
    private final String workerId;

    public ClinicalEventOutboxWorker(ClinicalEventOutboxRepository outbox,
                                     ClinicalEventProcessor processor,
                                     ClinicalEventProperties properties) {
        this.outbox = outbox;
        this.processor = processor;
        this.properties = properties;
        this.workerId = resolveWorkerId();
    }

    @Scheduled(fixedDelayString = "${medkernel.events.worker.poll-interval-ms:200}")
    public void pollOnce() {
        Instant now = Instant.now();
        for (ClinicalEventOutbox row : outbox.findReadyToClaim(now, properties.workerBatchSize())) {
            if (outbox.claim(row.id(), "CLAIMED", workerId, now) == 0) {
                continue;
            }
            processClaimed(row);
        }
    }

    private void processClaimed(ClinicalEventOutbox row) {
        TraceIdPropagator.restoreFromTrace(row.traceId(), row.tenantId(), row.actorUserId());
        try {
            processor.process(row.eventId(), row.tenantId());
            outbox.markProcessed(row.id(), Instant.now());
        } catch (ApiException exception) {
            markFailed(row, exception.errorCode());
        } catch (RuntimeException exception) {
            markFailed(row, ErrorCode.INTERNAL_ERROR);
        } finally {
            TraceIdPropagator.clear();
        }
    }

    private void markFailed(ClinicalEventOutbox row, ErrorCode errorCode) {
        int nextRetry = safeRetryCount(row) + 1;
        boolean dead = nextRetry >= properties.maxRetries();
        String status = dead ? "DEAD" : "PENDING";
        Instant nextAttemptAt = nextAttemptAt(nextRetry);
        processor.markFailed(row.eventId(), row.tenantId(), errorCode, nextRetry, dead, nextAttemptAt);
        outbox.markFailed(row.id(), status, nextRetry, errorCode.code(), nextAttemptAt);
    }

    private Instant nextAttemptAt(int retryCount) {
        if (retryCount >= properties.maxRetries()) {
            return Instant.now();
        }
        int index = Math.min(retryCount - 1, properties.backoffSeconds().size() - 1);
        return Instant.now().plusSeconds(properties.backoffSeconds().get(index));
    }

    private int safeRetryCount(ClinicalEventOutbox row) {
        return row.retryCount() == null ? 0 : row.retryCount();
    }

    private String resolveWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "clinical-event-worker";
        }
    }
}
