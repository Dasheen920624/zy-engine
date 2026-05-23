package com.medkernel.provenance;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceCitationTest {

    @Test
    void shouldSetAndGetAllFields() {
        SourceCitation citation = new SourceCitation();

        citation.setTenantId("tenant1");
        assertEquals("tenant1", citation.getTenantId());

        citation.setCitationId("CIT_001");
        assertEquals("CIT_001", citation.getCitationId());

        citation.setDocumentCode("DOC_001");
        assertEquals("DOC_001", citation.getDocumentCode());

        citation.setSection("3.1");
        assertEquals("3.1", citation.getSection());

        citation.setPage("42");
        assertEquals("42", citation.getPage());

        citation.setClause("5.2.1");
        assertEquals("5.2.1", citation.getClause());

        citation.setQuoteText("some quoted text");
        assertEquals("some quoted text", citation.getQuoteText());

        citation.setCitationType("SECTION");
        assertEquals("SECTION", citation.getCitationType());

        citation.setDescription("Test description");
        assertEquals("Test description", citation.getDescription());

        citation.setCreatedBy("admin");
        assertEquals("admin", citation.getCreatedBy());

        citation.setCreatedTime("2024-01-01T00:00:00+08:00");
        assertEquals("2024-01-01T00:00:00+08:00", citation.getCreatedTime());

        citation.setUpdatedTime("2024-06-01T00:00:00+08:00");
        assertEquals("2024-06-01T00:00:00+08:00", citation.getUpdatedTime());
    }

    @Test
    void fields_shouldDefaultToNull() {
        SourceCitation citation = new SourceCitation();
        assertNull(citation.getTenantId());
        assertNull(citation.getCitationId());
        assertNull(citation.getDocumentCode());
        assertNull(citation.getSection());
        assertNull(citation.getPage());
        assertNull(citation.getClause());
        assertNull(citation.getQuoteText());
        assertNull(citation.getCitationType());
        assertNull(citation.getDescription());
        assertNull(citation.getCreatedBy());
        assertNull(citation.getCreatedTime());
        assertNull(citation.getUpdatedTime());
    }

    @Test
    void toView_shouldReturnCompleteMap() {
        SourceCitation citation = new SourceCitation();
        citation.setTenantId("tenant1");
        citation.setCitationId("CIT_001");
        citation.setDocumentCode("DOC_001");
        citation.setSection("3.1");
        citation.setPage("42");
        citation.setClause("5.2.1");
        citation.setQuoteText("quoted text");
        citation.setCitationType("SECTION");
        citation.setDescription("description");
        citation.setCreatedBy("admin");
        citation.setCreatedTime("2024-01-01T00:00:00+08:00");
        citation.setUpdatedTime("2024-06-01T00:00:00+08:00");

        Map<String, Object> view = citation.toView();

        assertEquals("tenant1", view.get("tenant_id"));
        assertEquals("CIT_001", view.get("citation_id"));
        assertEquals("DOC_001", view.get("document_code"));
        assertEquals("3.1", view.get("section"));
        assertEquals("42", view.get("page"));
        assertEquals("5.2.1", view.get("clause"));
        assertEquals("quoted text", view.get("quote_text"));
        assertEquals("SECTION", view.get("citation_type"));
        assertEquals("description", view.get("description"));
        assertEquals("admin", view.get("created_by"));
        assertEquals("2024-01-01T00:00:00+08:00", view.get("created_time"));
        assertEquals("2024-06-01T00:00:00+08:00", view.get("updated_time"));
    }

    @Test
    void toView_shouldContainAllExpectedKeys() {
        SourceCitation citation = new SourceCitation();
        Map<String, Object> view = citation.toView();

        assertTrue(view.containsKey("tenant_id"));
        assertTrue(view.containsKey("citation_id"));
        assertTrue(view.containsKey("document_code"));
        assertTrue(view.containsKey("section"));
        assertTrue(view.containsKey("page"));
        assertTrue(view.containsKey("clause"));
        assertTrue(view.containsKey("quote_text"));
        assertTrue(view.containsKey("citation_type"));
        assertTrue(view.containsKey("description"));
        assertTrue(view.containsKey("created_by"));
        assertTrue(view.containsKey("created_time"));
        assertTrue(view.containsKey("updated_time"));
    }

    @Test
    void toView_withNullFields_shouldReturnNullValues() {
        SourceCitation citation = new SourceCitation();
        Map<String, Object> view = citation.toView();

        assertNull(view.get("tenant_id"));
        assertNull(view.get("citation_id"));
        assertNull(view.get("document_code"));
    }
}
