package com.medkernel.patient;

import com.medkernel.adapter.AdapterHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MPI模块单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MpiServiceTest {

    @Mock
    private MpiPersistenceService persistenceService;

    @Mock
    private AdapterHubService adapterHubService;

    @InjectMocks
    private MpiService mpiService;

    private PatientIdentity samplePatientIdentity;
    private VisitIdentity sampleVisitIdentity;
    private IdentityConflict sampleConflict;

    @BeforeEach
    void setUp() {
        samplePatientIdentity = new PatientIdentity();
        samplePatientIdentity.setId(1L);
        samplePatientIdentity.setTenantId("T001");
        samplePatientIdentity.setPlatformPatientId("P001");
        samplePatientIdentity.setIdentityType("HIS_PATIENT_ID");
        samplePatientIdentity.setExternalId("HIS001");
        samplePatientIdentity.setIdHash("hash001");
        samplePatientIdentity.setSourceSystem("HIS");
        samplePatientIdentity.setStatus("ACTIVE");
        samplePatientIdentity.setConfidence(100);
        samplePatientIdentity.setManuallyVerified(false);
        samplePatientIdentity.setCreatedTime(LocalDateTime.now());

        sampleVisitIdentity = new VisitIdentity();
        sampleVisitIdentity.setId(2L);
        sampleVisitIdentity.setTenantId("T001");
        sampleVisitIdentity.setPlatformVisitId("V001");
        sampleVisitIdentity.setPlatformPatientId("P001");
        sampleVisitIdentity.setVisitType("OUTPATIENT");
        sampleVisitIdentity.setIdentityType("HIS_VISIT_ID");
        sampleVisitIdentity.setExternalId("VISIT001");
        sampleVisitIdentity.setIdHash("hash002");
        sampleVisitIdentity.setSourceSystem("HIS");
        sampleVisitIdentity.setStatus("ACTIVE");
        sampleVisitIdentity.setVisitDate(LocalDate.now());
        sampleVisitIdentity.setCreatedTime(LocalDateTime.now());

        sampleConflict = new IdentityConflict();
        sampleConflict.setId(3L);
        sampleConflict.setTenantId("T001");
        sampleConflict.setConflictType("DUPLICATE_EXTERNAL");
        sampleConflict.setSeverity("HIGH");
        sampleConflict.setPatientIdentityIds("[1, 2]");
        sampleConflict.setConflictDescription("重复外部标识");
        sampleConflict.setStatus("PENDING");
        sampleConflict.setCreatedTime(LocalDateTime.now());
    }

    @Test
    void testRegisterPatientIdentity_NewIdentity() {
        when(persistenceService.findPatientIdentityByExternalId("T001", "HIS_PATIENT_ID", "HIS", "HIS001"))
                .thenReturn(null);
        when(persistenceService.savePatientIdentity(any(PatientIdentity.class)))
                .thenReturn(samplePatientIdentity);

        PatientIdentity result = mpiService.registerPatientIdentity("T001", "P001", "HIS_PATIENT_ID", "HIS001", "HIS");

        assertNotNull(result);
        assertEquals("P001", result.getPlatformPatientId());
        assertEquals("HIS_PATIENT_ID", result.getIdentityType());
        verify(persistenceService).savePatientIdentity(any(PatientIdentity.class));
    }

    @Test
    void testRegisterPatientIdentity_ExistingIdentity() {
        when(persistenceService.findPatientIdentityByExternalId("T001", "HIS_PATIENT_ID", "HIS", "HIS001"))
                .thenReturn(samplePatientIdentity);

        PatientIdentity result = mpiService.registerPatientIdentity("T001", "P001", "HIS_PATIENT_ID", "HIS001", "HIS");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(persistenceService, never()).savePatientIdentity(any(PatientIdentity.class));
    }

    @Test
    void testBatchRegisterPatientIdentities() {
        when(persistenceService.findPatientIdentityByExternalId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        when(persistenceService.savePatientIdentity(any(PatientIdentity.class)))
                .thenReturn(samplePatientIdentity);

        List<Map<String, String>> identities = Arrays.asList(
                Map.of("identityType", "HIS_PATIENT_ID", "externalId", "HIS001", "sourceSystem", "HIS"),
                Map.of("identityType", "EMR_PATIENT_ID", "externalId", "EMR001", "sourceSystem", "EMR")
        );

        int count = mpiService.batchRegisterPatientIdentities("T001", "P001", identities);

        assertEquals(2, count);
        verify(persistenceService, times(2)).savePatientIdentity(any(PatientIdentity.class));
    }

    @Test
    void testFindPatientIdentities() {
        when(persistenceService.findPatientIdentitiesByPlatformId("T001", "P001"))
                .thenReturn(Arrays.asList(samplePatientIdentity));

        List<PatientIdentity> result = mpiService.findPatientIdentities("T001", "P001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P001", result.get(0).getPlatformPatientId());
    }

    @Test
    void testFindPatientByExternalId() {
        when(persistenceService.findPatientIdentityByExternalId("T001", "HIS_PATIENT_ID", "HIS", "HIS001"))
                .thenReturn(samplePatientIdentity);

        PatientIdentity result = mpiService.findPatientByExternalId("T001", "HIS_PATIENT_ID", "HIS", "HIS001");

        assertNotNull(result);
        assertEquals("HIS001", result.getExternalId());
    }

    @Test
    void testVerifyPatientIdentity() {
        doNothing().when(persistenceService).verifyPatientIdentity(1L, "admin");

        mpiService.verifyPatientIdentity(1L, "admin");

        verify(persistenceService).verifyPatientIdentity(1L, "admin");
    }

    @Test
    void testMergePatientIdentities() {
        PatientIdentity targetIdentity = new PatientIdentity();
        targetIdentity.setId(2L);
        targetIdentity.setTenantId("T001");
        targetIdentity.setPlatformPatientId("P002");

        when(persistenceService.findPatientIdentityById(1L)).thenReturn(samplePatientIdentity);
        when(persistenceService.findPatientIdentityById(2L)).thenReturn(targetIdentity);
        when(persistenceService.savePatientIdentity(any(PatientIdentity.class)))
                .thenReturn(samplePatientIdentity);

        mpiService.mergePatientIdentities(1L, 2L, "admin");

        verify(persistenceService, times(2)).savePatientIdentity(any(PatientIdentity.class));
    }

    @Test
    void testMergePatientIdentities_DifferentTenants() {
        PatientIdentity targetIdentity = new PatientIdentity();
        targetIdentity.setId(2L);
        targetIdentity.setTenantId("T002");

        when(persistenceService.findPatientIdentityById(1L)).thenReturn(samplePatientIdentity);
        when(persistenceService.findPatientIdentityById(2L)).thenReturn(targetIdentity);

        assertThrows(IllegalArgumentException.class, () -> {
            mpiService.mergePatientIdentities(1L, 2L, "admin");
        });
    }

    @Test
    void testRegisterVisitIdentity_NewIdentity() {
        when(persistenceService.findVisitIdentityByExternalId("T001", "HIS_VISIT_ID", "HIS", "VISIT001"))
                .thenReturn(null);
        when(persistenceService.saveVisitIdentity(any(VisitIdentity.class)))
                .thenReturn(sampleVisitIdentity);

        VisitIdentity result = mpiService.registerVisitIdentity("T001", "V001", "P001", "OUTPATIENT", 
                "HIS_VISIT_ID", "VISIT001", "HIS", LocalDate.now(), "DEPT001");

        assertNotNull(result);
        assertEquals("V001", result.getPlatformVisitId());
        verify(persistenceService).saveVisitIdentity(any(VisitIdentity.class));
    }

    @Test
    void testFindVisitIdentities() {
        when(persistenceService.findVisitIdentitiesByPlatformId("T001", "V001"))
                .thenReturn(Arrays.asList(sampleVisitIdentity));

        List<VisitIdentity> result = mpiService.findVisitIdentities("T001", "V001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("V001", result.get(0).getPlatformVisitId());
    }

    @Test
    void testFindPatientVisitIdentities() {
        when(persistenceService.findVisitIdentitiesByPatientId("T001", "P001"))
                .thenReturn(Arrays.asList(sampleVisitIdentity));

        List<VisitIdentity> result = mpiService.findPatientVisitIdentities("T001", "P001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P001", result.get(0).getPlatformPatientId());
    }

    @Test
    void testDetectConflicts() {
        when(persistenceService.detectPatientIdentityConflicts("T001"))
                .thenReturn(Arrays.asList(sampleConflict));

        List<IdentityConflict> result = mpiService.detectConflicts("T001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DUPLICATE_EXTERNAL", result.get(0).getConflictType());
    }

    @Test
    void testGetPendingConflicts() {
        when(persistenceService.findPendingConflicts("T001"))
                .thenReturn(Arrays.asList(sampleConflict));

        List<IdentityConflict> result = mpiService.getPendingConflicts("T001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    @Test
    void testResolveConflict() {
        when(persistenceService.findIdentityConflictById(3L)).thenReturn(sampleConflict);
        doNothing().when(persistenceService).resolveConflict(3L, "MERGE", "合并处理", "admin", 2L);

        mpiService.resolveConflict(3L, "MERGE", "合并处理", "admin", 2L);

        verify(persistenceService).resolveConflict(3L, "MERGE", "合并处理", "admin", 2L);
    }

    @Test
    void testResolveConflict_NotFound() {
        when(persistenceService.findIdentityConflictById(3L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            mpiService.resolveConflict(3L, "MERGE", "合并处理", "admin", 2L);
        });
    }

    @Test
    void testResolveConflict_AlreadyResolved() {
        sampleConflict.setStatus("RESOLVED");
        when(persistenceService.findIdentityConflictById(3L)).thenReturn(sampleConflict);

        assertThrows(IllegalStateException.class, () -> {
            mpiService.resolveConflict(3L, "MERGE", "合并处理", "admin", 2L);
        });
    }

    @Test
    void testSyncPatientIdentitiesFromAdapter() {
        List<Map<String, Object>> externalData = Arrays.asList(
                Map.of("patientId", "EXT001", "patientName", "张三"),
                Map.of("patientId", "EXT002", "patientName", "李四")
        );

        when(adapterHubService.queryExternalData("HIS_ADAPTER", "QUERY_HIS_USERS", null))
                .thenReturn(externalData);
        when(persistenceService.hashId("EXT001")).thenReturn("hash1234567890123456");
        when(persistenceService.hashId("EXT002")).thenReturn("hash2345678901234567");
        when(persistenceService.findPatientIdentityByExternalId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        when(persistenceService.savePatientIdentity(any(PatientIdentity.class)))
                .thenReturn(samplePatientIdentity);

        int count = mpiService.syncPatientIdentitiesFromAdapter("T001", "HIS_ADAPTER", "QUERY_HIS_USERS");

        assertEquals(2, count);
        verify(persistenceService, times(2)).savePatientIdentity(any(PatientIdentity.class));
    }

    @Test
    void testSyncVisitIdentitiesFromAdapter() {
        List<Map<String, Object>> externalData = Arrays.asList(
                Map.of("visitId", "VISIT001", "patientId", "EXT001", "visitType", "OUTPATIENT")
        );

        when(adapterHubService.queryExternalData("HIS_ADAPTER", "QUERY_HIS_VISITS", null))
                .thenReturn(externalData);
        when(persistenceService.findPatientIdentityByExternalId("T001", "HIS_ADAPTER_PATIENT_ID", "HIS_ADAPTER", "EXT001"))
                .thenReturn(samplePatientIdentity);
        when(persistenceService.hashId("VISIT001")).thenReturn("hash3456789012345678");
        when(persistenceService.findVisitIdentityByExternalId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        when(persistenceService.saveVisitIdentity(any(VisitIdentity.class)))
                .thenReturn(sampleVisitIdentity);

        int count = mpiService.syncVisitIdentitiesFromAdapter("T001", "HIS_ADAPTER", "QUERY_HIS_VISITS");

        assertEquals(1, count);
        verify(persistenceService).saveVisitIdentity(any(VisitIdentity.class));
    }
}