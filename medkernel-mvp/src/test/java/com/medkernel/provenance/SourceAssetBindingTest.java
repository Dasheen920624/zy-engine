package com.medkernel.provenance;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAssetBindingTest {

    @Test
    void shouldSetAndGetAllFields() {
        SourceAssetBinding binding = new SourceAssetBinding();

        binding.setTenantId("tenant1");
        assertEquals("tenant1", binding.getTenantId());

        binding.setBindingId("BIND_001");
        assertEquals("BIND_001", binding.getBindingId());

        binding.setAssetType("RULE");
        assertEquals("RULE", binding.getAssetType());

        binding.setAssetCode("RULE_001");
        assertEquals("RULE_001", binding.getAssetCode());

        binding.setDocumentCode("DOC_001");
        assertEquals("DOC_001", binding.getDocumentCode());

        binding.setCitationId("CIT_001");
        assertEquals("CIT_001", binding.getCitationId());

        binding.setBindingType("EVIDENCE");
        assertEquals("EVIDENCE", binding.getBindingType());

        binding.setConfidence("0.95");
        assertEquals("0.95", binding.getConfidence());

        binding.setDescription("Test description");
        assertEquals("Test description", binding.getDescription());

        binding.setCreatedBy("admin");
        assertEquals("admin", binding.getCreatedBy());

        binding.setCreatedTime("2024-01-01T00:00:00+08:00");
        assertEquals("2024-01-01T00:00:00+08:00", binding.getCreatedTime());

        binding.setUpdatedTime("2024-06-01T00:00:00+08:00");
        assertEquals("2024-06-01T00:00:00+08:00", binding.getUpdatedTime());
    }

    @Test
    void fields_shouldDefaultToNull() {
        SourceAssetBinding binding = new SourceAssetBinding();
        assertNull(binding.getTenantId());
        assertNull(binding.getBindingId());
        assertNull(binding.getAssetType());
        assertNull(binding.getAssetCode());
        assertNull(binding.getDocumentCode());
        assertNull(binding.getCitationId());
        assertNull(binding.getBindingType());
        assertNull(binding.getConfidence());
        assertNull(binding.getDescription());
        assertNull(binding.getCreatedBy());
        assertNull(binding.getCreatedTime());
        assertNull(binding.getUpdatedTime());
    }

    @Test
    void toView_shouldReturnCompleteMap() {
        SourceAssetBinding binding = new SourceAssetBinding();
        binding.setTenantId("tenant1");
        binding.setBindingId("BIND_001");
        binding.setAssetType("RULE");
        binding.setAssetCode("RULE_001");
        binding.setDocumentCode("DOC_001");
        binding.setCitationId("CIT_001");
        binding.setBindingType("EVIDENCE");
        binding.setConfidence("0.95");
        binding.setDescription("description");
        binding.setCreatedBy("admin");
        binding.setCreatedTime("2024-01-01T00:00:00+08:00");
        binding.setUpdatedTime("2024-06-01T00:00:00+08:00");

        Map<String, Object> view = binding.toView();

        assertEquals("tenant1", view.get("tenant_id"));
        assertEquals("BIND_001", view.get("binding_id"));
        assertEquals("RULE", view.get("asset_type"));
        assertEquals("RULE_001", view.get("asset_code"));
        assertEquals("DOC_001", view.get("document_code"));
        assertEquals("CIT_001", view.get("citation_id"));
        assertEquals("EVIDENCE", view.get("binding_type"));
        assertEquals("0.95", view.get("confidence"));
        assertEquals("description", view.get("description"));
        assertEquals("admin", view.get("created_by"));
        assertEquals("2024-01-01T00:00:00+08:00", view.get("created_time"));
        assertEquals("2024-06-01T00:00:00+08:00", view.get("updated_time"));
    }

    @Test
    void toView_shouldContainAllExpectedKeys() {
        SourceAssetBinding binding = new SourceAssetBinding();
        Map<String, Object> view = binding.toView();

        assertTrue(view.containsKey("tenant_id"));
        assertTrue(view.containsKey("binding_id"));
        assertTrue(view.containsKey("asset_type"));
        assertTrue(view.containsKey("asset_code"));
        assertTrue(view.containsKey("document_code"));
        assertTrue(view.containsKey("citation_id"));
        assertTrue(view.containsKey("binding_type"));
        assertTrue(view.containsKey("confidence"));
        assertTrue(view.containsKey("description"));
        assertTrue(view.containsKey("created_by"));
        assertTrue(view.containsKey("created_time"));
        assertTrue(view.containsKey("updated_time"));
    }

    @Test
    void toView_withNullFields_shouldReturnNullValues() {
        SourceAssetBinding binding = new SourceAssetBinding();
        Map<String, Object> view = binding.toView();

        assertNull(view.get("tenant_id"));
        assertNull(view.get("binding_id"));
        assertNull(view.get("asset_type"));
    }
}
