package com.medkernel.shared.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void ofComputesHasNextWhenMorePagesExist() {
        PageRequest req = new PageRequest(1, 20, null);
        PageResponse<String> resp = PageResponse.of(List.of("a", "b"), req, 100);

        assertThat(resp.items()).hasSize(2);
        assertThat(resp.page()).isEqualTo(1);
        assertThat(resp.size()).isEqualTo(20);
        assertThat(resp.total()).isEqualTo(100);
        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.totalEstimated()).isFalse();
    }

    @Test
    void ofComputesHasNextFalseOnLastPage() {
        PageRequest req = new PageRequest(5, 20, null);
        PageResponse<String> resp = PageResponse.of(List.of("z"), req, 100);

        assertThat(resp.hasNext()).isFalse();
    }

    @Test
    void emptyReturnsZeros() {
        PageResponse<String> resp = PageResponse.empty(PageRequest.defaults());
        assertThat(resp.items()).isEmpty();
        assertThat(resp.total()).isZero();
        assertThat(resp.hasNext()).isFalse();
    }

    @Test
    void pageRequestSafeSizeCapsAtMax() {
        PageRequest big = new PageRequest(1, 5000, null);
        assertThat(big.safeSize()).isEqualTo(PageRequest.MAX_SIZE);
    }

    @Test
    void pageRequestSafeSizeDefaultsWhenNull() {
        PageRequest none = new PageRequest(null, null, null);
        assertThat(none.safeSize()).isEqualTo(PageRequest.DEFAULT_SIZE);
        assertThat(none.safePage()).isEqualTo(PageRequest.DEFAULT_PAGE);
    }

    @Test
    void offsetComputedFromPageAndSize() {
        PageRequest req = new PageRequest(3, 50, null);
        assertThat(req.offset()).isEqualTo(100);
    }

    @Test
    void cursorRequestFirstHasNoCursor() {
        CursorRequest first = CursorRequest.first();
        assertThat(first.cursor()).isNull();
        assertThat(first.safeSize()).isEqualTo(CursorRequest.DEFAULT_SIZE);
    }

    @Test
    void cursorResponseOfMarksHasNextWhenCursorPresent() {
        CursorResponse<String> resp = CursorResponse.of(List.of("a"), "next-cursor");
        assertThat(resp.hasNext()).isTrue();
        assertThat(resp.nextCursor()).isEqualTo("next-cursor");

        CursorResponse<String> last = CursorResponse.of(List.of("a"), null);
        assertThat(last.hasNext()).isFalse();
    }
}
