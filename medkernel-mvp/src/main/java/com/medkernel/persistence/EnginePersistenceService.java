package com.medkernel.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.audit.AuditLogRepository;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.dify.workflow.DifyWorkflowTemplate;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.dto.RuleResult;
import com.medkernel.pathway.PathwayInstanceRepository;
import com.medkernel.provenance.SourceAssetBinding;
import com.medkernel.provenance.SourceCitation;
import com.medkernel.provenance.SourceDocument;
import com.medkernel.provenance.SourceDocumentRepository;
import com.medkernel.rule.RuleDefinition;
import com.medkernel.rule.RuleExecLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnginePersistenceService extends PersistenceRepositorySupport {
    private final PathwayInstanceRepository pathwayInstanceRepository;
    private final RuleExecLogRepository ruleExecLogRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public EnginePersistenceService(EnginePersistenceProperties properties,
                                    ObjectMapper objectMapper,
                                    DataSource dataSource,
                                    IdAllocatorRepository idAllocatorRepository,
                                    PathwayInstanceRepository pathwayInstanceRepository,
                                    RuleExecLogRepository ruleExecLogRepository,
                                    SourceDocumentRepository sourceDocumentRepository,
                                    AuditLogRepository auditLogRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
        this.pathwayInstanceRepository = pathwayInstanceRepository;
        this.ruleExecLogRepository = ruleExecLogRepository;
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public EnginePersistenceService(EnginePersistenceProperties properties,
                                    ObjectMapper objectMapper,
                                    DataSource dataSource) {
        this(properties, objectMapper, dataSource, new IdAllocatorRepository());
    }

    private EnginePersistenceService(EnginePersistenceProperties properties,
                                     ObjectMapper objectMapper,
                                     DataSource dataSource,
                                     IdAllocatorRepository idAllocatorRepository) {
        this(properties, objectMapper, dataSource, idAllocatorRepository,
                new PathwayInstanceRepository(properties, objectMapper, dataSource, idAllocatorRepository),
                new RuleExecLogRepository(properties, objectMapper, dataSource, idAllocatorRepository),
                new SourceDocumentRepository(properties, objectMapper, dataSource, idAllocatorRepository),
                new AuditLogRepository(properties, objectMapper, dataSource, idAllocatorRepository));
    }
    /** 返回当前持久化提供者名称，未启用时返回 IN_MEMORY。 */
    public String providerName() {
        return enabled() ? properties.providerName() : "IN_MEMORY";
    }

    /** 初始化本地文件数据库的 Schema（H2 DDL），仅在 localFileDatabase 且 initSchema 开启时执行。 */
    @PostConstruct
    public void initializeLocalSchema() {
        if (!enabled() || !properties.localFileDatabase() || !properties.isInitSchema()) {
            return;
        }
        List<String> statements = loadLocalSchemaStatements();
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("initialize local database schema failed: " + ex.getMessage(), ex);
        }
    }

    /** 保存或更新路径草稿（pe_pathway_def），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void savePathwayDraft(String pathwayCode, Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            savePathwayDraftLocal(pathwayCode, config);
            return;
        }
        String updateSql = "UPDATE pe_pathway_def SET pathway_name=?, specialty_code=?, disease_code=?, status='DRAFT', updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND pathway_code=?";
        String insertSql = "INSERT INTO pe_pathway_def (id, tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            String tenantId = string(config.get("tenant_id"), "default");
            String orgCode = string(config.get("org_code"), "ZYHOSPITAL");
            String name = string(config.get("pathway_name"), pathwayCode);
            String specialty = string(config.get("specialty_code"), null);
            String disease = string(config.get("disease_code"), pathwayCode);
            long id = nextId(tenantId);
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, name);
                ps.setString(i++, specialty);
                ps.setString(i++, disease);
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, pathwayCode);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, id);
                    ips.setString(i++, tenantId);
                    ips.setString(i++, orgCode);
                    ips.setString(i++, pathwayCode);
                    ips.setString(i++, name);
                    ips.setString(i++, specialty);
                    ips.setString(i++, disease);
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    /** 删除指定路径的草稿记录。 */
    public void deletePathwayDraft(String pathwayCode) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            deletePathwayDraftLocal(pathwayCode);
            return;
        }
        String sql = "DELETE FROM pe_pathway_def WHERE pathway_code=? AND status='DRAFT'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pathwayCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    /** 保存或更新路径版本（pe_pathway_version），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void savePathwayVersion(String pathwayCode, String versionNo, String status, Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            savePathwayVersionLocal(pathwayCode, versionNo, status, config);
            return;
        }
        String updateSql = "UPDATE pe_pathway_version SET status=?, config_json=? " +
                "WHERE pathway_code=? AND version_no=?";
        String insertSql = "INSERT INTO pe_pathway_version (id, pathway_code, version_no, status, config_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP)";
        String json = toJson(config);
        try (Connection connection = connection()) {
            long id = nextId();
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, status);
                ps.setString(i++, json);
                ps.setString(i++, pathwayCode);
                ps.setString(i++, versionNo);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, id);
                    ips.setString(i++, pathwayCode);
                    ips.setString(i++, versionNo);
                    ips.setString(i++, status);
                    ips.setString(i++, json);
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save pathway version failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 启动期/重启后用于从 DB 重建 PathwayService 的内存索引。
     * 返回所有路径主表条目，键为 pathway_code，值为最小化的 config Map（包含 tenant_id/org_code/pathway_name/status 等元数据）。
     * 配合 loadAllPathwayPublishedVersions 一起使用，避免重启导致已发布路径在 list/get 接口中"消失"。
     */
    public Map<String, Map<String, Object>> loadAllPathwayDrafts() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
        if (!enabled()) {
            return result;
        }
        String sql = "SELECT tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status " +
                "FROM pe_pathway_def";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> config = new LinkedHashMap<String, Object>();
                config.put("tenant_id", rs.getString("tenant_id"));
                config.put("org_code", rs.getString("org_code"));
                config.put("pathway_code", rs.getString("pathway_code"));
                config.put("pathway_name", rs.getString("pathway_name"));
                config.put("specialty_code", rs.getString("specialty_code"));
                config.put("disease_code", rs.getString("disease_code"));
                config.put("status", rs.getString("status"));
                result.put(rs.getString("pathway_code"), config);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load pathway drafts failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 加载所有路径版本，返回 List 以保留 (pathway_code, version_no) 顺序。
     * 每项包含 pathway_code/version_no/status/config（来自 config_json 反序列化）。
     */
    public List<Map<String, Object>> loadAllPathwayPublishedVersions() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (!enabled()) {
            return result;
        }
        String sql = "SELECT pathway_code, version_no, status, config_json FROM pe_pathway_version " +
                "ORDER BY pathway_code, created_time";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("pathway_code", rs.getString("pathway_code"));
                row.put("version_no", rs.getString("version_no"));
                row.put("status", rs.getString("status"));
                String configJson = rs.getString("config_json");
                if (configJson != null && !configJson.isEmpty()) {
                    try {
                        Map<String, Object> config = objectMapper.readValue(configJson, LinkedHashMap.class);
                        row.put("config", config);
                    } catch (IOException ex) {
                        // 单条解析失败不阻断整体重建，记录占位即可，业务侧 list 仍能看到版本号。
                        row.put("config", new LinkedHashMap<String, Object>());
                    }
                } else {
                    row.put("config", new LinkedHashMap<String, Object>());
                }
                result.add(row);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load pathway versions failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /** 更新路径主表状态（pe_pathway_def.status），发布/停用时调用。 */
    public void updatePathwayStatus(String pathwayCode, String status) {
        updatePathwayStatus(pathwayCode, status, null, null);
    }

    /** 更新路径主表状态（pe_pathway_def.status），支持指定租户和机构。 */
    public void updatePathwayStatus(String pathwayCode, String status, String tenantId, String orgCode) {
        if (!enabled()) {
            return;
        }
        String resolvedTenant = string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID);
        String resolvedOrg = string(orgCode, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE);
        String sql = "UPDATE pe_pathway_def SET status=?, updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND pathway_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            // 路径主表用于配置中心筛选当前可用路径，发布版本时需要同步主表状态。
            ps.setString(1, status);
            ps.setString(2, resolvedTenant);
            ps.setString(3, resolvedOrg);
            ps.setString(4, pathwayCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update pathway status failed: " + ex.getMessage(), ex);
        }
    }

    /** 保存推荐卡记录（pe_recommendation_record），仅 INSERT。 */
    public void saveRecommendation(RecommendationCard card) {
        pathwayInstanceRepository.saveRecommendation(card);
    }

    /** 保存或更新患者路径实例（pe_patient_instance），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void savePatientInstance(PatientPathwayInstance instance, String admittedBy) {
        pathwayInstanceRepository.savePatientInstance(instance, admittedBy);
    }

    /** 更新节点状态（pe_patient_node_state），nodeName 默认取 nodeCode。 */
    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String status) {
        pathwayInstanceRepository.updateNodeState(instance, nodeCode, status);
    }

    /** 保存节点状态记录（pe_patient_node_state），仅 INSERT。 */
    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String nodeName, String status) {
        pathwayInstanceRepository.updateNodeState(instance, nodeCode, nodeName, status);
    }

    /** 保存或更新任务状态（pe_patient_task_state），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void saveTaskState(PatientTaskState taskState) {
        pathwayInstanceRepository.saveTaskState(taskState);
    }

    /** 保存变异记录（pe_variation_record），仅 INSERT。 */
    public void saveVariationRecord(PathwayVariationRecord variation) {
        pathwayInstanceRepository.saveVariationRecord(variation);
    }

    /** 保存或更新规则定义（re_rule_def），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void saveRuleDefinition(RuleDefinition definition, String approvedBy) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveRuleDefinitionLocal(definition, approvedBy);
            return;
        }
        String updateSql = "UPDATE re_rule_def SET rule_name=?, rule_type=?, status=?, severity=?, rule_json=?, approved_by=?, " +
                "approved_time=CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE approved_time END " +
                "WHERE tenant_id=? AND org_code=? AND rule_code=? AND version_no=?";
        String insertSql = "INSERT INTO re_rule_def (id, tenant_id, org_code, rule_code, rule_name, rule_type, version_no, status, severity, rule_json, created_time, approved_by, approved_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE NULL END)";
        try (Connection connection = connection()) {
            String json = toJson(definition.getRuleJson());
            String tenantId = string(definition.getTenantId(), "default");
            String orgCode = string(definition.getLegacyOrgCode(), string(definition.getHospitalCode(), "ZYHOSPITAL"));
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, definition.getRuleName());
                ps.setString(i++, definition.getRuleType());
                ps.setString(i++, definition.getStatus());
                ps.setString(i++, definition.getSeverity());
                ps.setString(i++, json);
                ps.setString(i++, approvedBy);
                ps.setString(i++, definition.getStatus());
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, definition.getRuleCode());
                ps.setString(i++, definition.getVersionNo());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId(tenantId));
                    ips.setString(i++, tenantId);
                    ips.setString(i++, orgCode);
                    ips.setString(i++, definition.getRuleCode());
                    ips.setString(i++, definition.getRuleName());
                    ips.setString(i++, definition.getRuleType());
                    ips.setString(i++, definition.getVersionNo());
                    ips.setString(i++, definition.getStatus());
                    ips.setString(i++, definition.getSeverity());
                    ips.setString(i++, json);
                    ips.setString(i++, approvedBy);
                    ips.setString(i++, definition.getStatus());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save rule definition failed: " + ex.getMessage(), ex);
        }
    }

    /** 保存规则执行日志（re_rule_exec_log），仅 INSERT。不含机构字段的重载版本。 */
    public void saveRuleExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                long elapsedMs, String resultStatus, String errorCode, String errorMessage) {
        ruleExecLogRepository.saveRuleExecLog(result, ruleVersion, patientContext, elapsedMs, resultStatus,
                errorCode, errorMessage);
    }

    /** 保存规则执行日志（re_rule_exec_log），仅 INSERT。支持机构字段。 */
    public void saveRuleExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                long elapsedMs, String resultStatus, String errorCode, String errorMessage,
                                Map<String, Object> orgFields) {
        ruleExecLogRepository.saveRuleExecLog(result, ruleVersion, patientContext, elapsedMs, resultStatus,
                errorCode, errorMessage, orgFields);
    }

    /** 保存或更新来源文档（src_document），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void saveSourceDocument(SourceDocument document) {
        sourceDocumentRepository.saveSourceDocument(document);
    }

    /** 查询所有来源文档，按租户、文档编码和更新时间排序。 */
    public List<SourceDocument> listSourceDocuments() {
        return sourceDocumentRepository.listSourceDocuments();
    }

    /** 按租户和文档编码查找单条来源文档。 */
    public SourceDocument findSourceDocument(String tenantId, String documentCode) {
        return sourceDocumentRepository.findSourceDocument(tenantId, documentCode);
    }

    /** 保存审计日志（engine_audit_log），仅 INSERT。同时写入内存缓冲区。 */
    public void saveAuditLog(String engineType, String actionType, String targetType, String targetCode,
                             String patientId, String encounterId, String operatorId, Map<String, Object> detail) {
        auditLogRepository.saveAuditLog(engineType, actionType, targetType, targetCode,
                patientId, encounterId, operatorId, detail);
    }

    /** 查询审计日志，支持多维度过滤（traceId/engineType/actionType/targetType/targetCode/patientId/encounterId/operatorId 及机构字段）。 */
    public List<Map<String, Object>> listAuditLogs(Map<String, String> filters) {
        return auditLogRepository.listAuditLogs(filters);
    }

    /** 按维度汇总审计日志，返回各维度的计数统计。 */
    public Map<String, Object> summarizeAuditLogs(Map<String, String> filters) {
        return auditLogRepository.summarizeAuditLogs(filters);
    }

    private void savePathwayDraftLocal(String pathwayCode, Map<String, Object> config) {
        String updateSql = "UPDATE pe_pathway_def SET pathway_name=?, specialty_code=?, disease_code=?, status='DRAFT', updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND pathway_code=?";
        String insertSql = "INSERT INTO pe_pathway_def (id, tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', CURRENT_TIMESTAMP)";
        String tenantId = string(config.get("tenant_id"), "default");
        String orgCode = string(config.get("org_code"), "ZYHOSPITAL");
        String name = string(config.get("pathway_name"), pathwayCode);
        String specialty = string(config.get("specialty_code"), null);
        String disease = string(config.get("disease_code"), pathwayCode);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, name);
                ps.setString(i++, specialty);
                ps.setString(i++, disease);
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, pathwayCode);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, pathwayCode);
                    ps.setString(i++, name);
                    ps.setString(i++, specialty);
                    ps.setString(i++, disease);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    private void deletePathwayDraftLocal(String pathwayCode) {
        String sql = "DELETE FROM pe_pathway_def WHERE pathway_code=? AND status='DRAFT'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pathwayCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete local pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    private void savePathwayVersionLocal(String pathwayCode, String versionNo, String status, Map<String, Object> config) {
        String updateSql = "UPDATE pe_pathway_version SET status=?, config_json=? WHERE pathway_code=? AND version_no=?";
        String insertSql = "INSERT INTO pe_pathway_version (id, pathway_code, version_no, status, config_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        String json = toJson(config);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, status);
                ps.setString(i++, json);
                ps.setString(i++, pathwayCode);
                ps.setString(i++, versionNo);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId());
                    ps.setString(i++, pathwayCode);
                    ps.setString(i++, versionNo);
                    ps.setString(i++, status);
                    ps.setString(i++, json);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local pathway version failed: " + ex.getMessage(), ex);
        }
    }

    private void saveRuleDefinitionLocal(RuleDefinition definition, String approvedBy) {
        String updateSql = "UPDATE re_rule_def SET rule_name=?, rule_type=?, status=?, severity=?, rule_json=?, approved_by=?, " +
                "approved_time=? WHERE tenant_id=? AND org_code=? AND rule_code=? AND version_no=?";
        String insertSql = "INSERT INTO re_rule_def " +
                "(id, tenant_id, org_code, rule_code, rule_name, rule_type, version_no, status, severity, rule_json, created_time, approved_by, approved_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)";
        String json = toJson(definition.getRuleJson());
        String tenantId = string(definition.getTenantId(), "default");
        String orgCode = string(definition.getLegacyOrgCode(), string(definition.getHospitalCode(), "ZYHOSPITAL"));
        Timestamp approvedTime = "PUBLISHED".equals(definition.getStatus()) ? new Timestamp(System.currentTimeMillis()) : null;
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, definition.getRuleName());
                ps.setString(i++, definition.getRuleType());
                ps.setString(i++, definition.getStatus());
                ps.setString(i++, definition.getSeverity());
                ps.setString(i++, json);
                ps.setString(i++, approvedBy);
                ps.setTimestamp(i++, approvedTime);
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, definition.getRuleCode());
                ps.setString(i++, definition.getVersionNo());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, definition.getRuleCode());
                    ps.setString(i++, definition.getRuleName());
                    ps.setString(i++, definition.getRuleType());
                    ps.setString(i++, definition.getVersionNo());
                    ps.setString(i++, definition.getStatus());
                    ps.setString(i++, definition.getSeverity());
                    ps.setString(i++, json);
                    ps.setString(i++, approvedBy);
                    ps.setTimestamp(i++, approvedTime);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local rule definition failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")

    // =========================================================================
    // SRC_CITATION 持久化
    // =========================================================================

    /**
     * 持久化单条 SourceCitation。
     * Oracle 使用 MERGE（UPSERT），H2 使用 UPDATE+INSERT 两阶段。
     * 字段映射：citationId↔citation_code, section↔section_code, clause↔clause_no,
     * page↔page_no, quoteText↔excerpt_text, description↔summary_text, citationType↔evidence_level。
     */
    public void saveSourceCitation(SourceCitation citation) {
        sourceDocumentRepository.saveSourceCitation(citation);
    }

    /**
     * 加载所有 SourceCitation，用于启动期重建内存索引。
     */
    public List<SourceCitation> listSourceCitations() {
        return sourceDocumentRepository.listSourceCitations();
    }

    // =========================================================================
    // SRC_ASSET_BINDING 持久化
    // =========================================================================

    /**
     * 持久化单条 SourceAssetBinding。
     * Oracle 使用 MERGE（UPSERT），H2 使用 UPDATE+INSERT 两阶段。
     * DDL 唯一键：(tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)。
     * 字段映射：assetType↔asset_type, assetCode↔asset_code, citationId↔citation_code,
     * bindingType↔binding_role, documentCode/confidence/description 无 DDL 列，仅内存保留。
     */
    public void saveSourceAssetBinding(SourceAssetBinding binding) {
        sourceDocumentRepository.saveSourceAssetBinding(binding);
    }

    /**
     * 加载所有 SourceAssetBinding，用于启动期重建内存索引。
     * 注意：DDL 不含 documentCode/confidence/description/updated_time 列，这些字段在重建后为 null。
     */
    public List<SourceAssetBinding> listSourceAssetBindings() {
        return sourceDocumentRepository.listSourceAssetBindings();
    }

    // =========================================================================
    // DIFY TEMPLATE 持久化
    // =========================================================================

    /**
     * 持久化单条 DifyWorkflowTemplate。
     * Oracle 使用 MERGE（UPSERT），H2 使用 UPDATE+INSERT 两阶段。
     * 唯一键：(workflow_code, workflow_version)。
     * 复杂嵌套字段（inputDefaults/inputMappings/requiredInputs/degradedOutputs）序列化为 template_json。
     */
    public void saveDifyTemplate(DifyWorkflowTemplate template) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveDifyTemplateLocal(template);
            return;
        }
        String updateSql = "UPDATE src_dify_template SET tenant_id=?, workflow_name=?, description=?, " +
                "dify_app_code=?, timeout_ms=?, retry_count=?, template_json=?, " +
                "reference_document_code=?, reference_binding_type=?, status='ACTIVE' " +
                "WHERE workflow_code=? AND workflow_version=?";
        String insertSql = "INSERT INTO src_dify_template (id, tenant_id, workflow_code, workflow_version, workflow_name, " +
                "description, dify_app_code, timeout_ms, retry_count, template_json, " +
                "reference_document_code, reference_binding_type, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            String tenantId = "default";
            String templateJson = toTemplateJson(template);
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, tenantId);
                ps.setString(i++, template.getWorkflowName());
                ps.setString(i++, template.getDescription());
                ps.setString(i++, template.getDifyAppCode());
                ps.setObject(i++, template.getTimeoutMs());
                ps.setObject(i++, template.getRetryCount());
                ps.setString(i++, templateJson);
                ps.setString(i++, template.getReferenceDocumentCode());
                ps.setString(i++, template.getReferenceBindingType());
                ps.setString(i++, template.getWorkflowCode());
                ps.setString(i++, template.getWorkflowVersion());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId(tenantId));
                    ips.setString(i++, tenantId);
                    ips.setString(i++, template.getWorkflowCode());
                    ips.setString(i++, template.getWorkflowVersion());
                    ips.setString(i++, template.getWorkflowName());
                    ips.setString(i++, template.getDescription());
                    ips.setString(i++, template.getDifyAppCode());
                    ips.setObject(i++, template.getTimeoutMs());
                    ips.setObject(i++, template.getRetryCount());
                    ips.setString(i++, templateJson);
                    ips.setString(i++, template.getReferenceDocumentCode());
                    ips.setString(i++, template.getReferenceBindingType());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save dify template failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 加载所有 DifyWorkflowTemplate，用于启动期重建内存索引。
     */
    public List<DifyWorkflowTemplate> listDifyTemplates() {
        if (!enabled()) {
            return new ArrayList<DifyWorkflowTemplate>();
        }
        String sql = "SELECT tenant_id, workflow_code, workflow_version, workflow_name, description, " +
                "dify_app_code, timeout_ms, retry_count, template_json, reference_document_code, " +
                "reference_binding_type, created_time FROM src_dify_template ORDER BY workflow_code, workflow_version";
        List<DifyWorkflowTemplate> templates = new ArrayList<DifyWorkflowTemplate>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                templates.add(toDifyTemplate(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list dify templates failed: " + ex.getMessage(), ex);
        }
        return templates;
    }

    private void saveDifyTemplateLocal(DifyWorkflowTemplate template) {
        String updateSql = "UPDATE src_dify_template SET tenant_id=?, workflow_name=?, description=?, " +
                "dify_app_code=?, timeout_ms=?, retry_count=?, template_json=?, " +
                "reference_document_code=?, reference_binding_type=?, status='ACTIVE' " +
                "WHERE workflow_code=? AND workflow_version=?";
        String insertSql = "INSERT INTO src_dify_template (id, tenant_id, workflow_code, workflow_version, " +
                "workflow_name, description, dify_app_code, timeout_ms, retry_count, template_json, " +
                "reference_document_code, reference_binding_type, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)";
        String templateJson = toTemplateJson(template);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, "default");
                ps.setString(i++, template.getWorkflowName());
                ps.setString(i++, template.getDescription());
                ps.setString(i++, template.getDifyAppCode());
                ps.setObject(i++, template.getTimeoutMs());
                ps.setObject(i++, template.getRetryCount());
                ps.setString(i++, templateJson);
                ps.setString(i++, template.getReferenceDocumentCode());
                ps.setString(i++, template.getReferenceBindingType());
                ps.setString(i++, template.getWorkflowCode());
                ps.setString(i++, template.getWorkflowVersion());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId("default"));
                    ps.setString(i++, "default");
                    ps.setString(i++, template.getWorkflowCode());
                    ps.setString(i++, template.getWorkflowVersion());
                    ps.setString(i++, template.getWorkflowName());
                    ps.setString(i++, template.getDescription());
                    ps.setString(i++, template.getDifyAppCode());
                    ps.setObject(i++, template.getTimeoutMs());
                    ps.setObject(i++, template.getRetryCount());
                    ps.setString(i++, templateJson);
                    ps.setString(i++, template.getReferenceDocumentCode());
                    ps.setString(i++, template.getReferenceBindingType());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local dify template failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private DifyWorkflowTemplate toDifyTemplate(ResultSet rs) throws SQLException {
        DifyWorkflowTemplate template = new DifyWorkflowTemplate();
        template.setWorkflowCode(rs.getString("workflow_code"));
        template.setWorkflowVersion(rs.getString("workflow_version"));
        template.setWorkflowName(rs.getString("workflow_name"));
        template.setDescription(rs.getString("description"));
        template.setDifyAppCode(rs.getString("dify_app_code"));
        template.setTimeoutMs((Integer) rs.getObject("timeout_ms"));
        template.setRetryCount((Integer) rs.getObject("retry_count"));
        template.setReferenceDocumentCode(rs.getString("reference_document_code"));
        template.setReferenceBindingType(rs.getString("reference_binding_type"));
        String templateJson = rs.getString("template_json");
        if (templateJson != null && !templateJson.trim().isEmpty()) {
            try {
                Map<String, Object> config = objectMapper.readValue(templateJson, LinkedHashMap.class);
                Object defaults = config.get("input_defaults");
                if (defaults instanceof Map) {
                    template.setInputDefaults(new LinkedHashMap<String, Object>((Map<String, Object>) defaults));
                }
                Object mappings = config.get("input_mappings");
                if (mappings instanceof Map) {
                    Map<String, String> mappingConfig = new LinkedHashMap<String, String>();
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) mappings).entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            mappingConfig.put(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                    }
                    template.setInputMappings(mappingConfig);
                }
                Object required = config.get("required_inputs");
                if (required instanceof Collection) {
                    List<String> requiredList = new ArrayList<String>();
                    for (Object item : (Collection<?>) required) {
                        if (item != null) {
                            requiredList.add(String.valueOf(item));
                        }
                    }
                    template.setRequiredInputs(requiredList);
                }
                Object degraded = config.get("degraded_outputs");
                if (degraded instanceof Map) {
                    template.setDegradedOutputs(new LinkedHashMap<String, Object>((Map<String, Object>) degraded));
                }
            } catch (IOException ex) {
                // 解析失败不阻断重建，保留空配置
            }
        }
        return template;
    }

    private String toTemplateJson(DifyWorkflowTemplate template) {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        if (template.getInputDefaults() != null && !template.getInputDefaults().isEmpty()) {
            config.put("input_defaults", template.getInputDefaults());
        }
        if (template.getInputMappings() != null && !template.getInputMappings().isEmpty()) {
            config.put("input_mappings", template.getInputMappings());
        }
        if (template.getRequiredInputs() != null && !template.getRequiredInputs().isEmpty()) {
            config.put("required_inputs", template.getRequiredInputs());
        }
        if (template.getDegradedOutputs() != null && !template.getDegradedOutputs().isEmpty()) {
            config.put("degraded_outputs", template.getDegradedOutputs());
        }
        return config.isEmpty() ? null : toJson(config);
    }

    private List<String> loadLocalSchemaStatements() {
        // 按顺序加载本地开发库 DDL：核心表 + 业务追加表。新增 DDL 需要在此显式注册。
        String[] resources = new String[] {
                "/db/local/h2_core_ddl.sql",
                "/db/local/re_rule_eval_result_ddl.sql",
                "/db/local/sec_ddl.sql",
                "/db/local/sec_sso_ddl.sql",
                "/db/local/sec_user_sync_ddl.sql",
                "/db/local/sec_multi_identity_ddl.sql",
                "/db/local/sec_audit_chain_ddl.sql",
                "/db/local/notify_ddl.sql",
                "/db/local/wf_ddl.sql",
                "/db/local/tenant_onboarding_ddl.sql",
                "/db/local/interop_ddl.sql",
                "/db/local/mpi_ddl.sql",
                "/db/local/ai_knowledge_job_ddl.sql",
                "/db/local/ai_knowledge_sync_log_ddl.sql",
                "/db/local/cdss_trigger_point_ddl.sql",
                "/db/local/model_provider_config_ddl.sql",
                "/db/local/cdss_override_log_ddl.sql",
                "/db/local/quality_finding_ddl.sql",
                "/db/local/ops_ddl.sql",
                "/db/local/ops_sync_task_ddl.sql",
                "/db/local/data_governance_ddl.sql",
                "/db/local/ai_governance_ddl.sql",
                "/db/local/clinical_safety_ddl.sql",
                "/db/local/knowledge_package_ddl.sql",
                "/db/local/ai_candidate_review_ddl.sql",
                "/db/local/cdss_safety_red_line_ddl.sql",
                "/db/local/ai_safety_ddl.sql",
                "/db/local/sec_data_permission_ddl.sql",
                "/db/local/sec_menu_permission_ddl.sql",
                "/db/local/prov_release_check_ddl.sql",
                "/db/local/qa_acceptance_ddl.sql",
                "/db/local/ops_deployment_ddl.sql",
                "/db/local/config_package_rollback_ddl.sql",
                "/db/local/impl_onboarding_ddl.sql"
        };
        List<String> statements = new ArrayList<String>();
        for (String resource : resources) {
            statements.addAll(loadLocalSchemaResource(resource));
        }
        return statements;
    }

    private List<String> loadLocalSchemaResource(String resource) {
        InputStream stream = EnginePersistenceService.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("local database schema resource not found: " + resource);
        }
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("read local database schema failed: " + resource, ex);
        }
        List<String> statements = new ArrayList<String>();
        String[] parts = sql.toString().split(";");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                statements.add(part);
            }
        }
        return statements;
    }

    /**
     * 提供给其他 Service 复用的安全 JSON 序列化：null 返回 null，序列化异常返回 null 不抛出，
     * 避免持久化层因输入异常打断业务主链路。
     */

    /**
     * 解析 Object 为 double，null 或不可解析时返回 defaultValue。
     * 用于 TERM-002 等场景下从 Map 提取置信度等浮点字段。
     */

    // PR-FINAL-15: 已删 shouldRetryConnection / sleepQuietly。
    // HikariCP 自带连接获取超时（connectionTimeoutMs=3s）+ 失效检测，
    // ORA-12518 等监听器拒绝由 pool 层重试，业务层不再关心。

    // =========================================================================
    // 术语治理队列持久化
    // =========================================================================

    /**
     * 保存或更新未映射治理队列记录。
     * 同一 tenant+sourceSystem+sourceCode+conceptType+governanceStatus 只保留一条，
     * 重复出现时 occurrence_count +1 并更新 last_occurrence_time。
     */
    public void saveUnmappedQueueEntry(Map<String, Object> entry) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveUnmappedQueueEntryLocal(entry);
            return;
        }
        String updateSql = "UPDATE tm_unmapped_queue SET occurrence_count=occurrence_count+1, " +
                "last_occurrence_time=SYSTIMESTAMP, source_name=COALESCE(?, source_name), " +
                "updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND source_system=? AND source_code=? AND concept_type=? AND governance_status=?";
        String insertSql = "INSERT INTO tm_unmapped_queue (id, tenant_id, queue_id, source_system, source_code, source_name, " +
                "concept_type, governance_status, proposed_standard_code, proposed_standard_name, " +
                "proposed_confidence, proposed_mapping_source, occurrence_count, last_occurrence_time, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, string(entry.get("source_name"), null));
                ps.setString(i++, string(entry.get("tenant_id"), "default"));
                ps.setString(i++, string(entry.get("source_system"), ""));
                ps.setString(i++, string(entry.get("source_code"), ""));
                ps.setString(i++, string(entry.get("concept_type"), ""));
                ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId());
                    ips.setString(i++, string(entry.get("tenant_id"), "default"));
                    ips.setString(i++, string(entry.get("queue_id"), ""));
                    ips.setString(i++, string(entry.get("source_system"), ""));
                    ips.setString(i++, string(entry.get("source_code"), ""));
                    ips.setString(i++, string(entry.get("source_name"), null));
                    ips.setString(i++, string(entry.get("concept_type"), ""));
                    ips.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                    ips.setString(i++, string(entry.get("proposed_standard_code"), null));
                    ips.setString(i++, string(entry.get("proposed_standard_name"), null));
                    ips.setDouble(i++, doubleValue(entry.get("proposed_confidence"), 0));
                    ips.setString(i++, string(entry.get("proposed_mapping_source"), null));
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save unmapped queue entry failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询未映射治理队列。
     * 支持按 governance_status / source_system / concept_type 过滤。
     */
    public List<Map<String, Object>> listUnmappedQueue(String tenantId, String governanceStatus,
                                                        String sourceSystem, String conceptType, int limit) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, queue_id, source_system, source_code, source_name, concept_type, " +
                "governance_status, proposed_standard_code, proposed_standard_name, proposed_confidence, " +
                "proposed_mapping_source, reviewed_by, reviewed_time, review_comment, " +
                "occurrence_count, last_occurrence_time, created_time, updated_time " +
                "FROM tm_unmapped_queue WHERE tenant_id=?");
        List<String> params = new ArrayList<String>();
        params.add(string(tenantId, "default"));
        if (governanceStatus != null && !governanceStatus.isEmpty()) {
            sql.append(" AND governance_status=?");
            params.add(governanceStatus);
        }
        if (sourceSystem != null && !sourceSystem.isEmpty()) {
            sql.append(" AND source_system=?");
            params.add(sourceSystem);
        }
        if (conceptType != null && !conceptType.isEmpty()) {
            sql.append(" AND concept_type=?");
            params.add(conceptType);
        }
        sql.append(" ORDER BY last_occurrence_time DESC");
        if (limit > 0) {
            sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
        }
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toUnmappedQueueEntry(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list unmapped queue failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * 更新治理队列记录状态（审批/驳回）。
     */
    public void updateUnmappedQueueStatus(String queueId, String tenantId, String governanceStatus,
                                            String reviewedBy, String reviewComment) {
        if (!enabled()) {
            return;
        }
        String sql = "UPDATE tm_unmapped_queue SET governance_status=?, reviewed_by=?, " +
                "reviewed_time=CURRENT_TIMESTAMP, review_comment=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE queue_id=? AND tenant_id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, governanceStatus);
            ps.setString(2, reviewedBy);
            ps.setString(3, reviewComment);
            ps.setString(4, queueId);
            ps.setString(5, string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update unmapped queue status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 删除未映射治理队列中的指定条目（用于 REJECTED 后清理，防止表膨胀）。
     */
    public void deleteUnmappedQueueEntry(String queueId, String tenantId) {
        if (!enabled()) {
            return;
        }
        String sql = "DELETE FROM tm_unmapped_queue WHERE queue_id=? AND tenant_id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, queueId);
            ps.setString(2, string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete unmapped queue entry failed: " + ex.getMessage(), ex);
        }
    }

    private void saveUnmappedQueueEntryLocal(Map<String, Object> entry) {
        // H2 不支持 Oracle MERGE 的 dual 写法，统一用 UPDATE + INSERT 两阶段。
        // 用事务包裹避免：UPDATE 受影响 0 行 → INSERT 期间另一线程已插入相同 (tenant,source,system,code,type)
        // 导致 INSERT 撞 UNIQUE 约束，或反过来 INSERT 成功后 UPDATE 计数丢失。
        String updateSql = "UPDATE tm_unmapped_queue SET occurrence_count=occurrence_count+1, " +
                "last_occurrence_time=CURRENT_TIMESTAMP, source_name=COALESCE(?, source_name), " +
                "updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND source_system=? AND source_code=? AND concept_type=? AND governance_status=?";
        String insertSql = "INSERT INTO tm_unmapped_queue (id, tenant_id, queue_id, source_system, source_code, " +
                "source_name, concept_type, governance_status, proposed_standard_code, proposed_standard_name, " +
                "proposed_confidence, proposed_mapping_source, occurrence_count, last_occurrence_time, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        Connection connection = null;
        boolean prevAutoCommit = true;
        try {
            connection = connection();
            prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, string(entry.get("source_name"), null));
                ps.setString(i++, string(entry.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
                ps.setString(i++, string(entry.get("source_system"), ""));
                ps.setString(i++, string(entry.get("source_code"), ""));
                ps.setString(i++, string(entry.get("concept_type"), ""));
                ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId());
                    ps.setString(i++, string(entry.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
                    ps.setString(i++, string(entry.get("queue_id"), ""));
                    ps.setString(i++, string(entry.get("source_system"), ""));
                    ps.setString(i++, string(entry.get("source_code"), ""));
                    ps.setString(i++, string(entry.get("source_name"), null));
                    ps.setString(i++, string(entry.get("concept_type"), ""));
                    ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                    ps.setString(i++, string(entry.get("proposed_standard_code"), null));
                    ps.setString(i++, string(entry.get("proposed_standard_name"), null));
                    ps.setDouble(i++, doubleValue(entry.get("proposed_confidence"), 0));
                    ps.setString(i++, string(entry.get("proposed_mapping_source"), null));
                    ps.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException rollbackEx) { /* ignore rollback failure */ }
            }
            throw new IllegalStateException("save unmapped queue entry local failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(prevAutoCommit);
                    connection.close();
                } catch (SQLException closeEx) { /* ignore */ }
            }
        }
    }

    private Map<String, Object> toUnmappedQueueEntry(ResultSet rs) throws SQLException {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("id", rs.getLong("id"));
        entry.put("tenant_id", rs.getString("tenant_id"));
        entry.put("queue_id", rs.getString("queue_id"));
        entry.put("source_system", rs.getString("source_system"));
        entry.put("source_code", rs.getString("source_code"));
        entry.put("source_name", rs.getString("source_name"));
        entry.put("concept_type", rs.getString("concept_type"));
        entry.put("governance_status", rs.getString("governance_status"));
        entry.put("proposed_standard_code", rs.getString("proposed_standard_code"));
        entry.put("proposed_standard_name", rs.getString("proposed_standard_name"));
        entry.put("proposed_confidence", rs.getDouble("proposed_confidence"));
        entry.put("proposed_mapping_source", rs.getString("proposed_mapping_source"));
        entry.put("reviewed_by", rs.getString("reviewed_by"));
        entry.put("reviewed_time", formatTimestamp(rs.getTimestamp("reviewed_time")));
        entry.put("review_comment", rs.getString("review_comment"));
        entry.put("occurrence_count", rs.getInt("occurrence_count"));
        entry.put("last_occurrence_time", formatTimestamp(rs.getTimestamp("last_occurrence_time")));
        entry.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        entry.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return entry;
    }
}
