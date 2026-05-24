package com.medkernel.engine.org;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

class OrgUnitServiceTest {

    private OrgUnitRepository repository;
    private OrgUnitService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(OrgUnitRepository.class);
        service = new OrgUnitService(repository);
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void rejectsRequestsWithoutTenantContext() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.empty(), null));
        assertThatThrownBy(() -> service.listByCurrentTenant(PageRequest.defaults()))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TENANT_CONTEXT_MISSING);
    }

    @Test
    void filtersByCurrentTenantWhenListing() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.tenant("t-99"), "u-1"));
        Mockito.when(repository.countByTenantId("t-99")).thenReturn(3L);
        Mockito.when(repository.pageByTenantId(eq("t-99"), anyInt(), anyInt()))
            .thenReturn(List.of(sampleHospital("t-99")));

        PageResponse<OrgUnit> page = service.listByCurrentTenant(PageRequest.defaults());

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).tenantId()).isEqualTo("t-99");
    }

    @Test
    void getByCodeNotFoundThrowsApiException() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.tenant("t-1"), "u"));
        Mockito.when(repository.findByTenantIdAndCode(eq("t-1"), eq("MISSING")))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCurrentTenantAndCode("MISSING"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void childrenMapGroupsByParent() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.tenant("t-1"), "u"));
        OrgUnit root = sample("t-1", null, OrgLevel.HOSPITAL, "HOSP-001", 1L);
        OrgUnit dept1 = sample("t-1", 1L, OrgLevel.DEPARTMENT, "DEPT-A", 2L);
        OrgUnit dept2 = sample("t-1", 1L, OrgLevel.DEPARTMENT, "DEPT-B", 3L);
        Mockito.when(repository.findByTenantIdOrderByLevelAscCodeAsc("t-1"))
            .thenReturn(List.of(root, dept1, dept2));

        var map = service.childrenMapByCurrentTenant();
        assertThat(map.get(0L)).hasSize(1);
        assertThat(map.get(1L)).extracting(OrgUnit::code).containsExactly("DEPT-A", "DEPT-B");
    }

    @Test
    void emptyTenantReturnsEmptyPage() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.tenant("t-new"), "u"));
        Mockito.when(repository.countByTenantId("t-new")).thenReturn(0L);

        PageResponse<OrgUnit> page = service.listByCurrentTenant(PageRequest.defaults());
        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        Mockito.verify(repository, Mockito.never()).pageByTenantId(any(), anyInt(), anyInt());
    }

    private OrgUnit sampleHospital(String tenantId) {
        return sample(tenantId, null, OrgLevel.HOSPITAL, "HOSP-001", 10L);
    }

    private OrgUnit sample(String tenantId, Long parentId, OrgLevel level, String code, Long id) {
        Instant now = Instant.now();
        return new OrgUnit(id, parentId, tenantId, level, code, code, null, null,
            OrgUnitStatus.ACTIVE, now, "system", now, "system");
    }
}
