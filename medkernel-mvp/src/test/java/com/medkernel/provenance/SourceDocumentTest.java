package com.medkernel.provenance;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceDocumentTest {

    @Test
    void shouldSetAndGetAllFields() {
        SourceDocument doc = new SourceDocument();

        doc.setTenantId("tenant1");
        assertEquals("tenant1", doc.getTenantId());

        doc.setDocumentCode("DOC_001");
        assertEquals("DOC_001", doc.getDocumentCode());

        doc.setTitle("Test Title");
        assertEquals("Test Title", doc.getTitle());

        doc.setSourceType("GUIDELINE");
        assertEquals("GUIDELINE", doc.getSourceType());

        doc.setSourceUri("http://example.com");
        assertEquals("http://example.com", doc.getSourceUri());

        doc.setPublisher("PublisherA");
        assertEquals("PublisherA", doc.getPublisher());

        doc.setEffectiveDate("2024-01-01");
        assertEquals("2024-01-01", doc.getEffectiveDate());

        doc.setExpiryDate("2030-12-31");
        assertEquals("2030-12-31", doc.getExpiryDate());

        doc.setReviewStatus("APPROVED");
        assertEquals("APPROVED", doc.getReviewStatus());

        doc.setReviewedBy("reviewer1");
        assertEquals("reviewer1", doc.getReviewedBy());

        doc.setReviewedTime("2024-06-01T10:00:00+08:00");
        assertEquals("2024-06-01T10:00:00+08:00", doc.getReviewedTime());

        doc.setContentHash("abc123");
        assertEquals("abc123", doc.getContentHash());

        doc.setCreatedBy("admin");
        assertEquals("admin", doc.getCreatedBy());

        doc.setCreatedTime("2024-01-01T00:00:00+08:00");
        assertEquals("2024-01-01T00:00:00+08:00", doc.getCreatedTime());

        doc.setUpdatedTime("2024-06-01T00:00:00+08:00");
        assertEquals("2024-06-01T00:00:00+08:00", doc.getUpdatedTime());
    }

    @Test
    void metadata_shouldDefaultToEmptyMap() {
        SourceDocument doc = new SourceDocument();
        assertNotNull(doc.getMetadata());
        assertTrue(doc.getMetadata().isEmpty());
    }

    @Test
    void metadata_shouldSetAndGetCustomValues() {
        SourceDocument doc = new SourceDocument();
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);
        doc.setMetadata(metadata);

        assertEquals("value1", doc.getMetadata().get("key1"));
        assertEquals(42, doc.getMetadata().get("key2"));
    }

    @Test
    void fields_shouldDefaultToNull() {
        SourceDocument doc = new SourceDocument();
        assertNull(doc.getTenantId());
        assertNull(doc.getDocumentCode());
        assertNull(doc.getTitle());
        assertNull(doc.getSourceType());
        assertNull(doc.getSourceUri());
        assertNull(doc.getPublisher());
        assertNull(doc.getEffectiveDate());
        assertNull(doc.getExpiryDate());
        assertNull(doc.getReviewStatus());
        assertNull(doc.getReviewedBy());
        assertNull(doc.getReviewedTime());
        assertNull(doc.getContentHash());
        assertNull(doc.getCreatedBy());
        assertNull(doc.getCreatedTime());
        assertNull(doc.getUpdatedTime());
    }
}
