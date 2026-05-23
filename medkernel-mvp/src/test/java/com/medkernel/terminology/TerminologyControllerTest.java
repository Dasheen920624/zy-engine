package com.medkernel.terminology;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminologyControllerTest {

    @Mock
    private TerminologyService terminologyService;

    @Mock
    private OrganizationContextService organizationContextService;

    @Mock
    private HttpServletRequest httpRequest;

    private TerminologyController controller;

    @BeforeEach
    void setUp() {
        controller = new TerminologyController(terminologyService, organizationContextService);
    }

    // ==================== normalize ====================

    @Test
    void normalize_shouldCallServiceAndReturnResult() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_system", "HIS");
        request.put("source_code", "I21.0");
        request.put("concept_type", "DIAGNOSIS");

        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("matched", true);
        serviceResult.put("standard_code", "AMI_STEMI");
        when(terminologyService.normalize(request)).thenReturn(serviceResult);

        ApiResult<Map<String, Object>> result = controller.normalize(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getData().get("matched"));
        assertEquals("AMI_STEMI", result.getData().get("standard_code"));
        verify(organizationContextService).resolveWithBody(httpRequest, request);
    }

    // ==================== importMappings ====================

    @Test
    void importMappings_shouldCallServiceAndReturnResult() {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("source_system", "SYS1");
        mapping.put("standard_code", "STD1");

        List<Map<String, Object>> serviceResult = new ArrayList<Map<String, Object>>();
        serviceResult.add(mapping);
        when(terminologyService.importMappings(any())).thenReturn(serviceResult);

        Object request = Arrays.asList(mapping);
        ApiResult<List<Map<String, Object>>> result = controller.importMappings(request, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        verify(organizationContextService).resolve(httpRequest);
    }

    // ==================== listMappings ====================

    @Test
    void listMappings_shouldCallServiceAndReturnResult() {
        List<Map<String, Object>> serviceResult = new ArrayList<Map<String, Object>>();
        when(terminologyService.listMappings()).thenReturn(serviceResult);

        ApiResult<List<Map<String, Object>>> result = controller.listMappings(httpRequest);

        assertTrue(result.isSuccess());
        verify(organizationContextService).applyExplicitFilters(any(Map.class), eq(httpRequest));
    }

    // ==================== getMapping ====================

    @Test
    void getMapping_shouldCallServiceAndReturnResult() {
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("standard_code", "AMI_STEMI");
        when(terminologyService.getMapping("HIS", "I21.0", "DIAGNOSIS"))
                .thenReturn(serviceResult);

        ApiResult<Map<String, Object>> result = controller.getMapping("HIS", "I21.0", "DIAGNOSIS", httpRequest);

        assertTrue(result.isSuccess());
        assertEquals("AMI_STEMI", result.getData().get("standard_code"));
        verify(organizationContextService).applyExplicitFilters(any(Map.class), eq(httpRequest));
    }

    @Test
    void getMapping_shouldThrowWhenSourceSystemEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("", "I21.0", "DIAGNOSIS", httpRequest));
    }

    @Test
    void getMapping_shouldThrowWhenSourceCodeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("HIS", "  ", "DIAGNOSIS", httpRequest));
    }

    @Test
    void getMapping_shouldThrowWhenConceptTypeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("HIS", "I21.0", null, httpRequest));
    }

    @Test
    void getMapping_shouldThrowWhenSourceSystemTooLong() {
        String longValue = repeatChar('A', 129);
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping(longValue, "I21.0", "DIAGNOSIS", httpRequest));
    }

    @Test
    void getMapping_shouldThrowWhenSourceSystemHasInvalidChars() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("HIS!@#", "I21.0", "DIAGNOSIS", httpRequest));
    }

    @Test
    void getMapping_shouldAcceptValidCharacters() {
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        when(terminologyService.getMapping("HIS-1", "CODE_2", "TYPE.3"))
                .thenReturn(serviceResult);

        // Letters, numbers, underscore, dash, dot should all be valid
        assertDoesNotThrow(() -> controller.getMapping("HIS-1", "CODE_2", "TYPE.3", httpRequest));
    }

    // ==================== listPendingMappings ====================

    @Test
    void listPendingMappings_shouldCallServiceWithFilters() {
        List<Map<String, Object>> serviceResult = new ArrayList<Map<String, Object>>();
        when(terminologyService.listPendingMappings(any(Map.class))).thenReturn(serviceResult);

        ApiResult<List<Map<String, Object>>> result = controller.listPendingMappings(
                "PENDING_MAPPING", "HIS", "DIAGNOSIS", "50", httpRequest);

        assertTrue(result.isSuccess());
        verify(terminologyService).listPendingMappings(argThat(filters -> {
            return "PENDING_MAPPING".equals(filters.get("governanceStatus"))
                    && "HIS".equals(filters.get("sourceSystem"))
                    && "DIAGNOSIS".equals(filters.get("conceptType"))
                    && "50".equals(filters.get("limit"));
        }));
        verify(organizationContextService).applyExplicitFilters(any(Map.class), eq(httpRequest));
    }

    @Test
    void listPendingMappings_shouldWorkWithNullFilters() {
        List<Map<String, Object>> serviceResult = new ArrayList<Map<String, Object>>();
        when(terminologyService.listPendingMappings(any(Map.class))).thenReturn(serviceResult);

        ApiResult<List<Map<String, Object>>> result = controller.listPendingMappings(
                null, null, null, null, httpRequest);

        assertTrue(result.isSuccess());
        verify(terminologyService).listPendingMappings(argThat(filters -> {
            return filters.get("governanceStatus") == null
                    && filters.get("sourceSystem") == null
                    && filters.get("conceptType") == null
                    && filters.get("limit") == null;
        }));
    }

    // ==================== approvePendingMapping ====================

    @Test
    void approvePendingMapping_shouldCallServiceAndReturnResult() {
        Map<String, Object> approveRequest = new HashMap<String, Object>();
        approveRequest.put("standard_code", "STD1");

        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("governance_status", "APPROVED");
        when(terminologyService.approvePendingMapping("TQ-12345678", approveRequest))
                .thenReturn(serviceResult);

        ApiResult<Map<String, Object>> result = controller.approvePendingMapping(
                "TQ-12345678", approveRequest, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals("APPROVED", result.getData().get("governance_status"));
        verify(organizationContextService).resolve(httpRequest);
    }

    // ==================== rejectPendingMapping ====================

    @Test
    void rejectPendingMapping_shouldCallServiceAndReturnResult() {
        Map<String, Object> rejectRequest = new HashMap<String, Object>();
        rejectRequest.put("reviewed_by", "ADMIN");

        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("governance_status", "REJECTED");
        when(terminologyService.rejectPendingMapping("TQ-12345678", rejectRequest))
                .thenReturn(serviceResult);

        ApiResult<Map<String, Object>> result = controller.rejectPendingMapping(
                "TQ-12345678", rejectRequest, httpRequest);

        assertTrue(result.isSuccess());
        assertEquals("REJECTED", result.getData().get("governance_status"));
        verify(organizationContextService).resolve(httpRequest);
    }

    // ==================== validatePathToken edge cases ====================

    @Test
    void getMapping_shouldAcceptMaxLengthToken() {
        String maxLenValue = repeatChar('A', 128);
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        when(terminologyService.getMapping(maxLenValue, "CODE", "TYPE"))
                .thenReturn(serviceResult);

        assertDoesNotThrow(() -> controller.getMapping(maxLenValue, "CODE", "TYPE", httpRequest));
    }

    @Test
    void getMapping_shouldRejectOverMaxLengthToken() {
        String overLenValue = repeatChar('A', 129);
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping(overLenValue, "CODE", "TYPE", httpRequest));
    }

    @Test
    void getMapping_shouldAcceptDotUnderscoreDash() {
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        when(terminologyService.getMapping("a.b", "c_d", "e-f"))
                .thenReturn(serviceResult);

        assertDoesNotThrow(() -> controller.getMapping("a.b", "c_d", "e-f", httpRequest));
    }

    @Test
    void getMapping_shouldRejectSpaceInToken() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("HIS SYS", "CODE", "TYPE", httpRequest));
    }

    @Test
    void getMapping_shouldRejectUnicodeInToken() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getMapping("中文", "CODE", "TYPE", httpRequest));
    }

    // ==================== Helper ====================

    private static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
