package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalPatient;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.StateTransitionRecorder;

class ContextSnapshotServiceTest {

    private ContextSnapshotRepository snapshots;
    private CanonicalResourceRepository resources;
    private ContextIdempotencyKeyRepository idemRepo;
    private ContextValidator validator;
    private PackageVersionPort versions;
    private TerminologyMappingPort mapping;
    private AuditEventPublisher auditPublisher;
    private StateTransitionRecorder recorder;
    private ContextSnapshotService service;

    @BeforeEach
    void setUp() {
        snapshots = mock(ContextSnapshotRepository.class);
        resources = mock(CanonicalResourceRepository.class);
        idemRepo = mock(ContextIdempotencyKeyRepository.class);
        validator = new ContextValidator();
        versions = new LenientPackageVersionAdapter();
        mapping = mock(TerminologyMappingPort.class);
        auditPublisher = mock(AuditEventPublisher.class);
        recorder = mock(StateTransitionRecorder.class);
        when(mapping.evaluate(anyString(), any())).thenReturn(Map.of());
        ObjectMapper json = new ObjectMapper();
        json.findAndRegisterModules();
        service = new ContextSnapshotService(snapshots, resources, idemRepo,
            validator, versions, mapping, auditPublisher, recorder, json);

        when(snapshots.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resources.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestContext.restore(new RequestContext.Snapshot(
            "trace-test", OrgScope.tenant("tenant-A"), "tester"));
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateSnapshotWhenAllValid() {
        ContextSnapshotResponse resp = service.create(sampleRequest(), null);

        assertThat(resp.snapshotId()).startsWith("ctx-");
        assertThat(resp.status()).isEqualTo(ContextSnapshotStatus.ACTIVE);
        assertThat(resp.qualityStatus()).isEqualTo(QualityStatus.VALID);
        verify(snapshots, times(1)).save(any());
        // 1 patient + 1 encounter
        verify(resources, times(2)).save(any());
        verify(idemRepo, never()).save(any());
        verify(auditPublisher, times(1)).publish(
            eq(AuditAction.CREATE), eq("context_snapshot"), anyString(), anyString());
    }

    @Test
    void shouldNotEmitAuditWhenRejectedByInvalidQuality() {
        var resourcesDto = new ContextSnapshotResources(null,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", resourcesDto);
        assertThatThrownBy(() -> service.create(req, null)).isInstanceOf(ApiException.class);
        verify(auditPublisher, never()).publish(any(AuditAction.class), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectWhenPatientMissing() {
        var resourcesDto = new ContextSnapshotResources(null,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", resourcesDto);

        assertThatThrownBy(() -> service.create(req, null))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_CONTEXT_003);
    }

    @Test
    void shouldRejectWhenPackageVersionBlank() {
        var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
            "kpv-1", "", "ppv-1", validResources());

        assertThatThrownBy(() -> service.create(req, null))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_CONTEXT_002);
    }

    @Test
    void shouldReturnCachedSnapshotWhenIdempotencyKeyMatches() {
        when(idemRepo.findByTenantIdAndIdempotencyKey("tenant-A", "key-1"))
            .thenReturn(Optional.of(new ContextIdempotencyKey(
                1L, "tenant-A", "key-1", "ctx-cached", "digest",
                Instant.now().plusSeconds(60), Instant.now())));
        when(snapshots.findBySnapshotIdAndTenantId("ctx-cached", "tenant-A"))
            .thenReturn(Optional.of(new ContextSnapshot(
                1L, "ctx-cached", "tenant-A", "ORG-1", "MPI-1", null,
                "kpv-1", "rpv-1", "ppv-1",
                ContextSnapshotStatus.ACTIVE, "[]", "{}",
                QualityStatus.VALID, "trace", null, Instant.now(), "tester")));

        ContextSnapshotResponse resp = service.create(sampleRequest(), "key-1");

        assertThat(resp.snapshotId()).isEqualTo("ctx-cached");
        verify(snapshots, never()).save(any());
    }

    @Test
    void shouldPersistIdempotencyKeyWhenProvidedAndMiss() {
        when(idemRepo.findByTenantIdAndIdempotencyKey(eq("tenant-A"), anyString()))
            .thenReturn(Optional.empty());

        service.create(sampleRequest(), "fresh-key");

        verify(idemRepo, times(1)).save(any());
    }

    @Test
    void shouldRequireTenantContextOnCreate() {
        RequestContext.restore(new RequestContext.Snapshot("trace", OrgScope.empty(), "tester"));
        assertThatThrownBy(() -> service.create(sampleRequest(), null))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TENANT_CONTEXT_MISSING);
    }

    @Test
    void shouldFindByIdWithinCurrentTenant() {
        when(snapshots.findBySnapshotIdAndTenantId("ctx-1", "tenant-A"))
            .thenReturn(Optional.of(new ContextSnapshot(
                1L, "ctx-1", "tenant-A", "ORG-1", "MPI-1", "ENC-1",
                "kpv-1", "rpv-1", "ppv-1",
                ContextSnapshotStatus.ACTIVE, "[]", "{}",
                QualityStatus.VALID, "trace", null, Instant.now(), "tester")));

        ContextSnapshotResponse resp = service.findById("ctx-1");

        assertThat(resp.snapshotId()).isEqualTo("ctx-1");
        assertThat(resp.qualityStatus()).isEqualTo(QualityStatus.VALID);
    }

    @Test
    void shouldThrowWhenFindByIdMisses() {
        when(snapshots.findBySnapshotIdAndTenantId("nope", "tenant-A"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("nope"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_CONTEXT_001);
    }

    @Test
    void shouldListByPatientWithinCurrentTenant() {
        Instant now = Instant.now();
        when(snapshots.countByTenantIdAndPatientId("tenant-A", "MPI-1")).thenReturn(2L);
        when(snapshots.pageByTenantIdAndPatientIdOrderByCreatedAtDesc(
                "tenant-A", "MPI-1", 0, 20))
            .thenReturn(List.of(
                new ContextSnapshot(1L, "ctx-1", "tenant-A", "ORG-1", "MPI-1", "ENC-1",
                    "kpv-1", "rpv-1", "ppv-1",
                    ContextSnapshotStatus.ACTIVE, "[]", "{}",
                    QualityStatus.VALID, "t", null, now, "tester"),
                new ContextSnapshot(2L, "ctx-2", "tenant-A", "ORG-1", "MPI-1", null,
                    "kpv-1", "rpv-1", "ppv-1",
                    ContextSnapshotStatus.SUPERSEDED, "[]", "{}",
                    QualityStatus.PARTIAL, "t", null, now, "tester")
            ));

        var filter = new ContextSnapshotFilter("MPI-1", null, null, null, null);
        PageResponse<ContextSnapshotSummary> page = service.list(filter, PageRequest.defaults());

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(ContextSnapshotSummary::snapshotId)
            .containsExactly("ctx-1", "ctx-2");
    }

    @Test
    void shouldListByEncounterWhenPatientAbsent() {
        when(snapshots.countByTenantIdAndEncounterId("tenant-A", "ENC-X")).thenReturn(1L);
        when(snapshots.pageByTenantIdAndEncounterIdOrderByCreatedAtDesc(
                "tenant-A", "ENC-X", 0, 20))
            .thenReturn(List.of(
                new ContextSnapshot(1L, "ctx-e", "tenant-A", "ORG-1", "MPI-9", "ENC-X",
                    "kpv-1", "rpv-1", "ppv-1",
                    ContextSnapshotStatus.ACTIVE, "[]", "{}",
                    QualityStatus.VALID, "t", null, Instant.now(), "tester")));

        var filter = new ContextSnapshotFilter(null, "ENC-X", null, null, null);
        PageResponse<ContextSnapshotSummary> page = service.list(filter, PageRequest.defaults());

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).encounterId()).isEqualTo("ENC-X");
    }

    @Test
    void shouldReturnEmptyPageWhenNeitherPatientNorEncounter() {
        var filter = new ContextSnapshotFilter(null, null, null, null, null);
        PageResponse<ContextSnapshotSummary> page = service.list(filter, PageRequest.defaults());
        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }

    @Test
    void createWritesInitialStateTransition() {
        service.create(sampleRequest(), null);

        ArgumentCaptor<String> entityType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toStatus = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(recorder).record(entityType.capture(), anyString(), isNull(),
            toStatus.capture(), reason.capture(), isNull());

        assertThat(entityType.getValue()).isEqualTo("context_snapshot");
        assertThat(toStatus.getValue()).isEqualTo("ACTIVE");
        assertThat(reason.getValue()).isEqualTo("INITIAL_CREATE");
    }

    private ContextSnapshotRequest sampleRequest() {
        return new ContextSnapshotRequest("MPI-1", "ENC-1", "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", validResources());
    }

    private ContextSnapshotResources validResources() {
        var patient = new CanonicalPatient("MPI-1", "张三",
            LocalDate.of(1980, 1, 1), "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        var enc = new CanonicalEncounter("ENC-1", "IP", Instant.now(), null,
            "DEPT-A", "DOC-A", null, "HIS", "rec-2", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        return new ContextSnapshotResources(patient,
            List.of(enc), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
