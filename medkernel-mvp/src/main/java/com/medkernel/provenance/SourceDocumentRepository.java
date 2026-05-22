package com.medkernel.provenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.PersistenceRepositorySupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class SourceDocumentRepository extends PersistenceRepositorySupport {

    public SourceDocumentRepository(EnginePersistenceProperties properties,
                                     ObjectMapper objectMapper,
                                     DataSource dataSource,
                                     IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    /** 保存或更新来源文档（src_document），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void saveSourceDocument(SourceDocument document) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveSourceDocumentLocal(document);
            return;
        }
        String updateSql = "UPDATE src_document SET title=?, source_type=?, source_uri=?, publisher=?, " +
                "effective_date=?, expiry_date=?, review_status=?, reviewed_by=?, reviewed_time=?, " +
                "content_hash=?, metadata_json=?, created_by=COALESCE(?, created_by), updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND document_code=?";
        String insertSql = "INSERT INTO src_document (id, tenant_id, document_code, title, source_type, source_uri, publisher, " +
                "effective_date, expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, " +
                "created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                setSourceDocumentUpdateValues(ps, document, i);
                i += 12;
                ps.setString(i++, string(document.getTenantId(), "default"));
                ps.setString(i++, document.getDocumentCode());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId(string(document.getTenantId(), "default")));
                    ips.setString(i++, string(document.getTenantId(), "default"));
                    ips.setString(i++, document.getDocumentCode());
                    ips.setString(i++, document.getTitle());
                    ips.setString(i++, document.getSourceType());
                    ips.setString(i++, document.getSourceUri());
                    ips.setString(i++, document.getPublisher());
                    ips.setDate(i++, parseSqlDate(document.getEffectiveDate()));
                    ips.setDate(i++, parseSqlDate(document.getExpiryDate()));
                    ips.setString(i++, document.getReviewStatus());
                    ips.setString(i++, document.getReviewedBy());
                    ips.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
                    ips.setString(i++, document.getContentHash());
                    ips.setString(i++, toJson(document.getMetadata()));
                    ips.setString(i++, document.getCreatedBy());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save source document failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询所有来源文档，按租户、文档编码和更新时间排序。 */
    public List<SourceDocument> listSourceDocuments() {
        if (!enabled()) {
            return new ArrayList<SourceDocument>();
        }
        String sql = "SELECT tenant_id, document_code, title, source_type, source_uri, publisher, effective_date, " +
                "expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, created_by, " +
                "created_time, updated_time FROM src_document ORDER BY tenant_id, document_code, updated_time";
        List<SourceDocument> documents = new ArrayList<SourceDocument>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                documents.add(toSourceDocument(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list source documents failed: " + ex.getMessage(), ex);
        }
        return documents;
    }

    /** 按租户和文档编码查找单条来源文档。 */
    public SourceDocument findSourceDocument(String tenantId, String documentCode) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT tenant_id, document_code, title, source_type, source_uri, publisher, effective_date, " +
                "expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, created_by, " +
                "created_time, updated_time FROM src_document WHERE tenant_id=? AND document_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, string(tenantId, "default"));
            ps.setString(2, documentCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toSourceDocument(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find source document failed: " + ex.getMessage(), ex);
        }
    }

    private void saveSourceDocumentLocal(SourceDocument document) {
        String updateSql = "UPDATE src_document SET title=?, source_type=?, source_uri=?, publisher=?, " +
                "effective_date=?, expiry_date=?, review_status=?, reviewed_by=?, reviewed_time=?, content_hash=?, " +
                "metadata_json=?, created_by=COALESCE(?, created_by), updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND document_code=?";
        String insertSql = "INSERT INTO src_document (id, tenant_id, document_code, title, source_type, source_uri, " +
                "publisher, effective_date, expiry_date, review_status, reviewed_by, reviewed_time, content_hash, " +
                "metadata_json, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                setSourceDocumentUpdateValues(ps, document, i);
                i += 12;
                ps.setString(i++, string(document.getTenantId(), "default"));
                ps.setString(i++, document.getDocumentCode());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(string(document.getTenantId(), "default")));
                    ps.setString(i++, string(document.getTenantId(), "default"));
                    ps.setString(i++, document.getDocumentCode());
                    ps.setString(i++, document.getTitle());
                    ps.setString(i++, document.getSourceType());
                    ps.setString(i++, document.getSourceUri());
                    ps.setString(i++, document.getPublisher());
                    ps.setDate(i++, parseSqlDate(document.getEffectiveDate()));
                    ps.setDate(i++, parseSqlDate(document.getExpiryDate()));
                    ps.setString(i++, document.getReviewStatus());
                    ps.setString(i++, document.getReviewedBy());
                    ps.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
                    ps.setString(i++, document.getContentHash());
                    ps.setString(i++, toJson(document.getMetadata()));
                    ps.setString(i++, document.getCreatedBy());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local source document failed: " + ex.getMessage(), ex);
        }
    }

    private void setSourceDocumentUpdateValues(PreparedStatement ps, SourceDocument document, int startIndex)
            throws SQLException {
        int i = startIndex;
        ps.setString(i++, document.getTitle());
        ps.setString(i++, document.getSourceType());
        ps.setString(i++, document.getSourceUri());
        ps.setString(i++, document.getPublisher());
        ps.setDate(i++, parseSqlDate(document.getEffectiveDate()));
        ps.setDate(i++, parseSqlDate(document.getExpiryDate()));
        ps.setString(i++, document.getReviewStatus());
        ps.setString(i++, document.getReviewedBy());
        ps.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
        ps.setString(i++, document.getContentHash());
        ps.setString(i++, toJson(document.getMetadata()));
        ps.setString(i++, document.getCreatedBy());
    }

    private SourceDocument toSourceDocument(ResultSet rs) throws SQLException {
        SourceDocument document = new SourceDocument();
        document.setTenantId(rs.getString("tenant_id"));
        document.setDocumentCode(rs.getString("document_code"));
        document.setTitle(rs.getString("title"));
        document.setSourceType(rs.getString("source_type"));
        document.setSourceUri(rs.getString("source_uri"));
        document.setPublisher(rs.getString("publisher"));
        document.setEffectiveDate(formatDate(rs.getDate("effective_date")));
        document.setExpiryDate(formatDate(rs.getDate("expiry_date")));
        document.setReviewStatus(rs.getString("review_status"));
        document.setReviewedBy(rs.getString("reviewed_by"));
        document.setReviewedTime(formatTimestamp(rs.getTimestamp("reviewed_time")));
        document.setContentHash(rs.getString("content_hash"));
        String metadataJson = rs.getString("metadata_json");
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            try {
                document.setMetadata(objectMapper.readValue(metadataJson, LinkedHashMap.class));
            } catch (IOException ex) {
                document.setMetadata(new LinkedHashMap<String, Object>());
            }
        }
        document.setCreatedBy(rs.getString("created_by"));
        document.setCreatedTime(formatTimestamp(rs.getTimestamp("created_time")));
        document.setUpdatedTime(formatTimestamp(rs.getTimestamp("updated_time")));
        return document;
    }

    /**
     * 持久化单条 SourceCitation。
     * Oracle 使用 MERGE（UPSERT），H2 使用 UPDATE+INSERT 两阶段。
     * 字段映射：citationId↔citation_code, section↔section_code, clause↔clause_no,
     * page↔page_no, quoteText↔excerpt_text, description↔summary_text, citationType↔evidence_level。
     */
    public void saveSourceCitation(SourceCitation citation) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveSourceCitationLocal(citation);
            return;
        }
        String updateSql = "UPDATE src_citation SET document_code=?, section_code=?, clause_no=?, " +
                "page_no=?, excerpt_text=?, summary_text=?, evidence_level=?, status='ACTIVE' " +
                "WHERE tenant_id=? AND citation_code=?";
        String insertSql = "INSERT INTO src_citation (id, tenant_id, citation_code, document_code, section_code, " +
                "clause_no, page_no, excerpt_text, summary_text, evidence_level, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, citation.getDocumentCode());
                ps.setString(i++, citation.getSection());
                ps.setString(i++, citation.getClause());
                ps.setString(i++, citation.getPage());
                ps.setString(i++, citation.getQuoteText());
                ps.setString(i++, citation.getDescription());
                ps.setString(i++, citation.getCitationType());
                ps.setString(i++, string(citation.getTenantId(), "default"));
                ps.setString(i++, citation.getCitationId());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId(string(citation.getTenantId(), "default")));
                    ips.setString(i++, string(citation.getTenantId(), "default"));
                    ips.setString(i++, citation.getCitationId());
                    ips.setString(i++, citation.getDocumentCode());
                    ips.setString(i++, citation.getSection());
                    ips.setString(i++, citation.getClause());
                    ips.setString(i++, citation.getPage());
                    ips.setString(i++, citation.getQuoteText());
                    ips.setString(i++, citation.getDescription());
                    ips.setString(i++, citation.getCitationType());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save source citation failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 加载所有 SourceCitation，用于启动期重建内存索引。
     */
    public List<SourceCitation> listSourceCitations() {
        if (!enabled()) {
            return new ArrayList<SourceCitation>();
        }
        String sql = "SELECT tenant_id, citation_code, document_code, section_code, clause_no, " +
                "page_no, excerpt_text, summary_text, evidence_level, created_time " +
                "FROM src_citation ORDER BY tenant_id, citation_code";
        List<SourceCitation> citations = new ArrayList<SourceCitation>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                citations.add(toSourceCitation(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list source citations failed: " + ex.getMessage(), ex);
        }
        return citations;
    }

    private void saveSourceCitationLocal(SourceCitation citation) {
        String updateSql = "UPDATE src_citation SET document_code=?, section_code=?, clause_no=?, " +
                "page_no=?, excerpt_text=?, summary_text=?, evidence_level=?, status='ACTIVE' " +
                "WHERE tenant_id=? AND citation_code=?";
        String insertSql = "INSERT INTO src_citation (id, tenant_id, citation_code, document_code, " +
                "section_code, clause_no, page_no, excerpt_text, summary_text, evidence_level, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, citation.getDocumentCode());
                ps.setString(i++, citation.getSection());
                ps.setString(i++, citation.getClause());
                ps.setString(i++, citation.getPage());
                ps.setString(i++, citation.getQuoteText());
                ps.setString(i++, citation.getDescription());
                ps.setString(i++, citation.getCitationType());
                ps.setString(i++, string(citation.getTenantId(), "default"));
                ps.setString(i++, citation.getCitationId());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(string(citation.getTenantId(), "default")));
                    ps.setString(i++, string(citation.getTenantId(), "default"));
                    ps.setString(i++, citation.getCitationId());
                    ps.setString(i++, citation.getDocumentCode());
                    ps.setString(i++, citation.getSection());
                    ps.setString(i++, citation.getClause());
                    ps.setString(i++, citation.getPage());
                    ps.setString(i++, citation.getQuoteText());
                    ps.setString(i++, citation.getDescription());
                    ps.setString(i++, citation.getCitationType());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local source citation failed: " + ex.getMessage(), ex);
        }
    }

    private SourceCitation toSourceCitation(ResultSet rs) throws SQLException {
        SourceCitation citation = new SourceCitation();
        citation.setTenantId(rs.getString("tenant_id"));
        citation.setCitationId(rs.getString("citation_code"));
        citation.setDocumentCode(rs.getString("document_code"));
        citation.setSection(rs.getString("section_code"));
        citation.setClause(rs.getString("clause_no"));
        citation.setPage(rs.getString("page_no"));
        citation.setQuoteText(rs.getString("excerpt_text"));
        citation.setDescription(rs.getString("summary_text"));
        citation.setCitationType(rs.getString("evidence_level"));
        citation.setCreatedTime(formatTimestamp(rs.getTimestamp("created_time")));
        return citation;
    }

    /**
     * 持久化单条 SourceAssetBinding。
     * Oracle 使用 MERGE（UPSERT），H2 使用 UPDATE+INSERT 两阶段。
     * DDL 唯一键：(tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)。
     * 字段映射：assetType↔asset_type, assetCode↔asset_code, citationId↔citation_code,
     * bindingType↔binding_role, documentCode/confidence/description 无 DDL 列，仅内存保留。
     */
    public void saveSourceAssetBinding(SourceAssetBinding binding) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveSourceAssetBindingLocal(binding);
            return;
        }
        String updateSql = "UPDATE src_asset_binding SET status='ACTIVE', created_by=COALESCE(?, created_by) " +
                "WHERE tenant_id=? AND asset_type=? AND asset_code=? AND asset_version=? AND citation_code=? AND binding_role=?";
        String insertSql = "INSERT INTO src_asset_binding (id, tenant_id, asset_type, asset_code, asset_version, " +
                "citation_code, binding_role, status, created_by, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            String tenantId = string(binding.getTenantId(), "default");
            String citationCode = storageCitationCode(binding);
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, binding.getCreatedBy());
                ps.setString(i++, tenantId);
                ps.setString(i++, binding.getAssetType());
                ps.setString(i++, binding.getAssetCode());
                ps.setString(i++, "1");
                ps.setString(i++, citationCode);
                ps.setString(i++, binding.getBindingType());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, extractNumericId(binding.getBindingId()));
                    ips.setString(i++, tenantId);
                    ips.setString(i++, binding.getAssetType());
                    ips.setString(i++, binding.getAssetCode());
                    ips.setString(i++, "1");
                    ips.setString(i++, citationCode);
                    ips.setString(i++, binding.getBindingType());
                    ips.setString(i++, binding.getCreatedBy());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save source asset binding failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 加载所有 SourceAssetBinding，用于启动期重建内存索引。
     * 注意：DDL 不含 documentCode/confidence/description/updated_time 列，这些字段在重建后为 null。
     */
    public List<SourceAssetBinding> listSourceAssetBindings() {
        if (!enabled()) {
            return new ArrayList<SourceAssetBinding>();
        }
        String sql = "SELECT tenant_id, id, asset_type, asset_code, citation_code, binding_role, " +
                "status, created_by, created_time FROM src_asset_binding ORDER BY tenant_id, id";
        List<SourceAssetBinding> bindings = new ArrayList<SourceAssetBinding>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bindings.add(toSourceAssetBinding(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list source asset bindings failed: " + ex.getMessage(), ex);
        }
        return bindings;
    }

    private void saveSourceAssetBindingLocal(SourceAssetBinding binding) {
        String updateSql = "UPDATE src_asset_binding SET status='ACTIVE', created_by=COALESCE(?, created_by) " +
                "WHERE tenant_id=? AND asset_type=? AND asset_code=? AND asset_version=? AND citation_code=? AND binding_role=?";
        String insertSql = "INSERT INTO src_asset_binding (id, tenant_id, asset_type, asset_code, asset_version, " +
                "citation_code, binding_role, status, created_by, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, CURRENT_TIMESTAMP)";
        String tenantId = string(binding.getTenantId(), "default");
        String citationCode = storageCitationCode(binding);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, binding.getCreatedBy());
                ps.setString(i++, tenantId);
                ps.setString(i++, binding.getAssetType());
                ps.setString(i++, binding.getAssetCode());
                ps.setString(i++, "1");
                ps.setString(i++, citationCode);
                ps.setString(i++, binding.getBindingType());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, extractNumericId(binding.getBindingId()));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, binding.getAssetType());
                    ps.setString(i++, binding.getAssetCode());
                    ps.setString(i++, "1");
                    ps.setString(i++, citationCode);
                    ps.setString(i++, binding.getBindingType());
                    ps.setString(i++, binding.getCreatedBy());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local source asset binding failed: " + ex.getMessage(), ex);
        }
    }

    private String storageCitationCode(SourceAssetBinding binding) {
        String citationId = string(binding.getCitationId(), null);
        if (citationId != null) {
            return citationId;
        }
        return "DOC_REF_" + string(binding.getDocumentCode(), "DOCUMENT");
    }

    private SourceAssetBinding toSourceAssetBinding(ResultSet rs) throws SQLException {
        SourceAssetBinding binding = new SourceAssetBinding();
        binding.setTenantId(rs.getString("tenant_id"));
        binding.setBindingId("BIND_" + rs.getLong("id"));
        binding.setAssetType(rs.getString("asset_type"));
        binding.setAssetCode(rs.getString("asset_code"));
        binding.setCitationId(rs.getString("citation_code"));
        binding.setBindingType(rs.getString("binding_role"));
        binding.setCreatedBy(rs.getString("created_by"));
        binding.setCreatedTime(formatTimestamp(rs.getTimestamp("created_time")));
        // DDL 不含 documentCode, confidence, description, updated_time 列，重建后为 null
        return binding;
    }

}
