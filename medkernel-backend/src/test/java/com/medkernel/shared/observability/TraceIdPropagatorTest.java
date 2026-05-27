package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class TraceIdPropagatorTest {

    @AfterEach
    void clear() {
        RequestContext.clear();
        MDC.clear();
    }

    @Test
    void wrapPropagatesRequestContextAndMdcToAsyncThread() throws Exception {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-async", OrgScope.tenant("tenant-A"), "tester"));
        MDC.put(MdcEnrichmentFilter.MDC_TRACE_ID, "trace-async");
        MDC.put(MdcEnrichmentFilter.MDC_TENANT_ID, "tenant-A");

        AtomicReference<String> capturedTrace = new AtomicReference<>();
        AtomicReference<String> capturedMdc = new AtomicReference<>();
        AtomicReference<String> capturedTenant = new AtomicReference<>();

        Runnable task = TraceIdPropagator.wrap(() -> {
            capturedTrace.set(RequestContext.currentTraceId());
            capturedMdc.set(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID));
            OrgScope scope = RequestContext.currentOrgScope();
            capturedTenant.set(scope == null ? null : scope.tenantId());
        });

        CompletableFuture.runAsync(task).get();

        assertThat(capturedTrace.get()).isEqualTo("trace-async");
        assertThat(capturedMdc.get()).isEqualTo("trace-async");
        assertThat(capturedTenant.get()).isEqualTo("tenant-A");
    }

    @Test
    void wrapClearsContextAfterTaskExecution() throws Exception {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-x", OrgScope.tenant("tenant-A"), "tester"));

        AtomicReference<String> insideTask = new AtomicReference<>();
        AtomicReference<String> beforeTask = new AtomicReference<>("sentinel");
        AtomicReference<String> afterTask = new AtomicReference<>("sentinel");
        AtomicReference<String> afterMdc = new AtomicReference<>("sentinel");
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();

        Runnable task = TraceIdPropagator.wrap(() -> {
            insideTask.set(RequestContext.currentTraceId());
        });

        // 在另一个线程执行；该线程开始时无 context；wrap 应注入；结束时彻底清理（不残留随机 traceId）
        Thread t = new Thread(() -> {
            beforeTask.set(RequestContext.currentTraceId());
            task.run();
            afterTask.set(RequestContext.currentTraceId());
            afterMdc.set(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID));
        });
        t.setUncaughtExceptionHandler((thread, ex) -> threadFailure.set(ex));
        t.start();
        t.join();

        assertThat(threadFailure.get()).as("子线程不应抛异常").isNull();
        assertThat(insideTask.get()).isEqualTo("trace-x");
        assertThat(beforeTask.get()).as("任务前子线程 RequestContext 应为空").isNull();
        assertThat(afterTask.get()).as("任务后子线程 RequestContext 应彻底清理，不残留 wrap 内 fallback 的随机 traceId").isNull();
        assertThat(afterMdc.get()).as("任务后子线程 MDC traceId 应彻底清理").isNull();
    }

    @Test
    void wrapPreservesPreExistingContextInThread() throws Exception {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-outer", OrgScope.tenant("tenant-A"), "tester"));

        AtomicReference<String> insideTask = new AtomicReference<>();
        AtomicReference<String> afterTask = new AtomicReference<>("sentinel");
        AtomicReference<String> afterMdc = new AtomicReference<>("sentinel");
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();

        Runnable task = TraceIdPropagator.wrap(() -> insideTask.set(RequestContext.currentTraceId()));

        Thread t = new Thread(() -> {
            // 子线程预先有 trace-pre
            RequestContext.restore(new RequestContext.Snapshot(
                "trace-pre", OrgScope.tenant("tenant-B"), "preuser"));
            MDC.put(MdcEnrichmentFilter.MDC_TRACE_ID, "trace-pre");
            task.run();
            afterTask.set(RequestContext.currentTraceId());
            afterMdc.set(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID));
        });
        t.setUncaughtExceptionHandler((thread, ex) -> threadFailure.set(ex));
        t.start();
        t.join();

        assertThat(threadFailure.get()).isNull();
        assertThat(insideTask.get()).isEqualTo("trace-outer");
        assertThat(afterTask.get()).as("子线程原有 trace-pre 应被精确恢复").isEqualTo("trace-pre");
        assertThat(afterMdc.get()).as("子线程原有 MDC trace-pre 应被精确恢复").isEqualTo("trace-pre");
    }

    @Test
    void restoreFromTraceSetsRequestContextAndMdc() {
        TraceIdPropagator.restoreFromTrace("trace-restored", "tenant-B", "system");

        assertThat(RequestContext.currentTraceId()).isEqualTo("trace-restored");
        assertThat(RequestContext.currentOrgScope().tenantId()).isEqualTo("tenant-B");
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isEqualTo("trace-restored");
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TENANT_ID)).isEqualTo("tenant-B");

        TraceIdPropagator.clear();
        assertThat(RequestContext.currentTraceId()).isNull();
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isNull();
    }
}
