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

        Runnable task = TraceIdPropagator.wrap(() -> {
            insideTask.set(RequestContext.currentTraceId());
        });

        // 在另一个线程执行；该线程开始时无 context；wrap 应注入；结束时清理
        Thread t = new Thread(() -> {
            assertThat(RequestContext.currentTraceId()).isNull();  // 任务前
            task.run();
            assertThat(RequestContext.currentTraceId()).isNull();  // 任务后清理
            assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isNull();
        });
        t.start();
        t.join();

        assertThat(insideTask.get()).isEqualTo("trace-x");
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
