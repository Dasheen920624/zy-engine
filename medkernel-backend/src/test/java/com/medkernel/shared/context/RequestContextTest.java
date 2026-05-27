package com.medkernel.shared.context;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void snapshotAutoGeneratesTraceIdWhenMissing() {
        RequestContext.Snapshot s = new RequestContext.Snapshot(null, OrgScope.empty(), null);
        assertThat(s.traceId()).isNotBlank();
    }

    @Test
    void restoreSetsThreadLocal() {
        RequestContext.restore(new RequestContext.Snapshot("trace-x", OrgScope.tenant("t-1"), "u-7"));
        assertThat(RequestContext.currentTraceId()).isEqualTo("trace-x");
        assertThat(RequestContext.currentOrgScope().tenantId()).isEqualTo("t-1");
        assertThat(RequestContext.currentUserId()).contains("u-7");
    }

    @Test
    void clearRemovesThreadLocal() {
        RequestContext.restore(new RequestContext.Snapshot("trace-x", OrgScope.empty(), null));
        RequestContext.clear();
        assertThat(RequestContext.currentTraceId()).isNull();
        assertThat(RequestContext.currentOrgScope().tenantId()).isNull();
    }

    @Test
    void runWithRestoresPreviousAfterCompletion() {
        RequestContext.restore(new RequestContext.Snapshot("outer", OrgScope.tenant("t-1"), "u-1"));
        AtomicReference<String> innerTrace = new AtomicReference<>();

        RequestContext.runWith(
            new RequestContext.Snapshot("inner", OrgScope.tenant("t-2"), "u-2"),
            () -> innerTrace.set(RequestContext.currentTraceId())
        );

        assertThat(innerTrace.get()).isEqualTo("inner");
        assertThat(RequestContext.currentTraceId()).isEqualTo("outer");
        assertThat(RequestContext.currentOrgScope().tenantId()).isEqualTo("t-1");
    }

    @Test
    void peekSnapshotReturnsNullWhenThreadLocalEmpty() {
        // peekSnapshot 与 snapshot() 的关键差别：未设置时返回 null（不生成 fallback）
        assertThat(RequestContext.peekSnapshot()).isNull();
        assertThat(RequestContext.snapshot()).isNotNull();  // snapshot() 会 fallback 生成新 UUID
    }

    @Test
    void peekSnapshotReturnsActualSnapshotWhenSet() {
        RequestContext.Snapshot s = new RequestContext.Snapshot("trace-peek", OrgScope.tenant("t-9"), "u-9");
        RequestContext.restore(s);
        assertThat(RequestContext.peekSnapshot()).isSameAs(s);
    }

    @Test
    void emptyOrgScopeHasNoTenant() {
        OrgScope empty = OrgScope.empty();
        assertThat(empty.hasTenant()).isFalse();
        assertThat(OrgScope.tenant("t-1").hasTenant()).isTrue();
    }
}
