package com.medkernel.shared.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.medkernel.shared.context.RequestContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
        RequestContext.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        CapturingChain chain = new CapturingChain();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getHeader(TraceIdFilter.HEADER)).isNotBlank();
        assertThat(chain.capturedTraceId).isEqualTo(resp.getHeader(TraceIdFilter.HEADER));
        // After filter completes, MDC + ThreadLocal should be cleared
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
        assertThat(RequestContext.currentTraceId()).isNull();
    }

    @Test
    void propagatesExistingTraceIdFromHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        req.addHeader(TraceIdFilter.HEADER, "client-trace-123");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        CapturingChain chain = new CapturingChain();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getHeader(TraceIdFilter.HEADER)).isEqualTo("client-trace-123");
        assertThat(chain.capturedTraceId).isEqualTo("client-trace-123");
    }

    @Test
    void rejectsInvalidHeaderCharactersAndRegenerates() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        req.addHeader(TraceIdFilter.HEADER, "evil\nlog\ninjection");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        CapturingChain chain = new CapturingChain();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getHeader(TraceIdFilter.HEADER))
            .isNotEqualTo("evil\nlog\ninjection")
            .isNotBlank();
    }

    @Test
    void rejectsExtremelyLongHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        req.addHeader(TraceIdFilter.HEADER, "x".repeat(200));
        MockHttpServletResponse resp = new MockHttpServletResponse();

        CapturingChain chain = new CapturingChain();
        filter.doFilter(req, resp, chain);

        assertThat(resp.getHeader(TraceIdFilter.HEADER).length()).isLessThanOrEqualTo(128);
    }

    @Test
    void clearsContextEvenWhenChainThrows() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        FilterChain throwingChain = (request, response) -> {
            throw new ServletException("boom");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> filter.doFilter(req, resp, throwingChain)
        ).isInstanceOf(ServletException.class);

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
        assertThat(RequestContext.currentTraceId()).isNull();
    }

    static class CapturingChain implements FilterChain {
        String capturedTraceId;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            capturedTraceId = MDC.get(TraceIdFilter.MDC_KEY);
            // Context should also be populated mid-chain
            assertThat(RequestContext.currentTraceId()).isEqualTo(capturedTraceId);
        }
    }
}
