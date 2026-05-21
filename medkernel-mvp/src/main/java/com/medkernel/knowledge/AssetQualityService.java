package com.medkernel.knowledge;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 候选资产自动质检服务：对知识资产执行自动化质量检测，
 * 识别缺来源、过期、授权不明、规则冲突、低置信度、多候选冲突等问题。
 */
@Service
public class AssetQualityService {

    private static final Logger log = LoggerFactory.getLogger(AssetQualityService.class);

    /** 资产默认有效期（天） */
    private static final int DEFAULT_EXPIRY_DAYS = 365;

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AssetQualityService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 对指定资产执行质检。
     */
    public List<QualityFinding> runQualityCheck(Long tenantId, String assetType, String assetCode) {
        List<QualityFinding> allFindings = new ArrayList<QualityFinding>();
        allFindings.addAll(checkMissingSource(tenantId));
        allFindings.addAll(checkExpired(tenantId));
        allFindings.addAll(checkUnclearAuthorization(tenantId));
        allFindings.addAll(checkRuleConflicts(tenantId));
        allFindings.addAll(checkLowConfidence(tenantId, 0.6));
        allFindings.addAll(checkMultiCandidateConflicts(tenantId));

        // 按指定资产过滤
        List<QualityFinding> filtered = new ArrayList<QualityFinding>();
        for (QualityFinding f : allFindings) {
            if (assetType != null && !assetType.isEmpty() && !assetType.equals(f.getAssetType())) {
                continue;
            }
            if (assetCode != null && !assetCode.isEmpty() && !assetCode.equals(f.getAssetCode())) {
                continue;
            }
            filtered.add(f);
        }

        // 保存过滤后的发现
        for (QualityFinding f : filtered) {
            saveFinding(f);
        }
        return filtered;
    }

    /**
     * 对所有资产执行全量质检。
     */
    public List<QualityFinding> runFullQualityCheck(Long tenantId) {
        List<QualityFinding> allFindings = new ArrayList<QualityFinding>();
        allFindings.addAll(checkMissingSource(tenantId));
        allFindings.addAll(checkExpired(tenantId));
        allFindings.addAll(checkUnclearAuthorization(tenantId));
        allFindings.addAll(checkRuleConflicts(tenantId));
        allFindings.addAll(checkLowConfidence(tenantId, 0.6));
        allFindings.addAll(checkMultiCandidateConflicts(tenantId));

        for (QualityFinding f : allFindings) {
            saveFinding(f);
        }
        return allFindings;
    }

    /**
     * 检测缺来源的资产：查询无来源绑定的规则/术语映射。
     */
    public List<QualityFinding> checkMissingSource(Long tenantId) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        // 检查规则：没有对应 source_asset_binding 的规则
        String ruleSql = "SELECT r.rule_code, r.rule_name, r.version FROM rule_definition r "
                + "WHERE r.tenant_id = ? "
                + "AND NOT EXISTS ("
                + "  SELECT 1 FROM source_asset_binding b "
                + "  WHERE b.asset_type = 'RULE' AND b.asset_code = r.rule_code AND b.tenant_id = ?"
                + ")";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(ruleSql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("MISSING_SOURCE");
                    f.setSeverity("WARNING");
                    f.setAssetType("RULE");
                    f.setAssetCode(rs.getString("rule_code"));
                    f.setAssetName(rs.getString("rule_name"));
                    f.setAssetVersion(rs.getString("version"));
                    f.setDescription("规则 [" + rs.getString("rule_code") + "] 缺少来源绑定");
                    f.setDetectionRule("规则必须有至少一个来源绑定（source_asset_binding）");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());
                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测缺来源规则失败", ex);
        }

        // 检查术语映射：没有对应 source_asset_binding 的术语映射
        String termSql = "SELECT t.source_code, t.source_name, t.concept_type FROM concept_mapping t "
                + "WHERE t.tenant_id = ? "
                + "AND NOT EXISTS ("
                + "  SELECT 1 FROM source_asset_binding b "
                + "  WHERE b.asset_type = 'TERMINOLOGY_MAPPING' AND b.asset_code = t.source_code AND b.tenant_id = ?"
                + ")";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(termSql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("MISSING_SOURCE");
                    f.setSeverity("WARNING");
                    f.setAssetType("TERMINOLOGY_MAPPING");
                    f.setAssetCode(rs.getString("source_code"));
                    f.setAssetName(rs.getString("source_name"));
                    f.setAssetVersion(null);
                    f.setDescription("术语映射 [" + rs.getString("source_code") + "] 缺少来源绑定");
                    f.setDetectionRule("术语映射必须有至少一个来源绑定（source_asset_binding）");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());
                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测缺来源术语映射失败", ex);
        }

        return findings;
    }

    /**
     * 检测过期资产：查询超过有效期的资产（基于 created_time + 有效期配置）。
     */
    public List<QualityFinding> checkExpired(Long tenantId) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        // 检查规则过期
        String ruleSql = "SELECT rule_code, rule_name, version, created_time FROM rule_definition "
                + "WHERE tenant_id = ? AND created_time < ?";
        LocalDateTime expiryThreshold = LocalDateTime.now().minusDays(DEFAULT_EXPIRY_DAYS);
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(ruleSql)) {
            ps.setLong(1, tenantId);
            ps.setTimestamp(2, Timestamp.valueOf(expiryThreshold));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("EXPIRED");
                    f.setSeverity("CRITICAL");
                    f.setAssetType("RULE");
                    f.setAssetCode(rs.getString("rule_code"));
                    f.setAssetName(rs.getString("rule_name"));
                    f.setAssetVersion(rs.getString("version"));
                    f.setDescription("规则 [" + rs.getString("rule_code") + "] 已超过有效期（" + DEFAULT_EXPIRY_DAYS + " 天）");
                    f.setDetectionRule("资产创建时间超过 " + DEFAULT_EXPIRY_DAYS + " 天视为过期");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());
                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测过期规则失败", ex);
        }

        // 检查知识来源文档过期
        String sourceSql = "SELECT document_code, document_name, valid_until FROM source_document "
                + "WHERE tenant_id = ? AND valid_until IS NOT NULL AND valid_until < ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sourceSql)) {
            ps.setLong(1, tenantId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("EXPIRED");
                    f.setSeverity("CRITICAL");
                    f.setAssetType("KNOWLEDGE_ASSET");
                    f.setAssetCode(rs.getString("document_code"));
                    f.setAssetName(rs.getString("document_name"));
                    f.setAssetVersion(null);
                    f.setDescription("知识来源 [" + rs.getString("document_code") + "] 已超过有效期限");
                    f.setDetectionRule("知识来源文档 valid_until 早于当前时间视为过期");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());
                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测过期知识来源失败", ex);
        }

        return findings;
    }

    /**
     * 检测授权不明的资产：查询缺少授权范围或版权信息的知识来源。
     */
    public List<QualityFinding> checkUnclearAuthorization(Long tenantId) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        String sql = "SELECT document_code, document_name, source_type FROM source_document "
                + "WHERE tenant_id = ? "
                + "AND (license_scope IS NULL OR license_scope = '' "
                + "  OR copyright_info IS NULL OR copyright_info = '')";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("UNCLEAR_AUTH");
                    f.setSeverity("WARNING");
                    f.setAssetType("KNOWLEDGE_ASSET");
                    f.setAssetCode(rs.getString("document_code"));
                    f.setAssetName(rs.getString("document_name"));
                    f.setAssetVersion(null);
                    f.setDescription("知识来源 [" + rs.getString("document_code") + "] 缺少授权范围或版权信息");
                    f.setDetectionRule("知识来源文档必须填写 license_scope 和 copyright_info");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());
                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测授权不明资产失败", ex);
        }

        return findings;
    }

    /**
     * 检测规则冲突：同一场景下条件重叠的规则。
     */
    public List<QualityFinding> checkRuleConflicts(Long tenantId) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        // 查找同一场景下条件重叠的规则对
        String sql = "SELECT r1.rule_code AS code1, r1.rule_name AS name1, r1.version AS ver1, "
                + "r2.rule_code AS code2, r2.rule_name AS name2, r2.version AS ver2 "
                + "FROM rule_definition r1 "
                + "JOIN rule_definition r2 ON r1.tenant_id = r2.tenant_id "
                + "  AND r1.rule_code < r2.rule_code "
                + "  AND r1.status = 'ACTIVE' AND r2.status = 'ACTIVE' "
                + "WHERE r1.tenant_id = ? "
                + "AND EXISTS ("
                + "  SELECT 1 FROM rule_scenario rs1 "
                + "  JOIN rule_scenario rs2 ON rs1.scenario_code = rs2.scenario_code "
                + "  WHERE rs1.rule_code = r1.rule_code AND rs2.rule_code = r2.rule_code"
                + ")";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code1 = rs.getString("code1");
                    String code2 = rs.getString("code2");

                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("RULE_CONFLICT");
                    f.setSeverity("CRITICAL");
                    f.setAssetType("RULE");
                    f.setAssetCode(code1);
                    f.setAssetName(rs.getString("name1"));
                    f.setAssetVersion(rs.getString("ver1"));
                    f.setDescription("规则 [" + code1 + "] 与 [" + code2 + "] 在同一场景下存在条件重叠");
                    f.setDetectionRule("同一场景下不应存在条件重叠的活跃规则");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());

                    Map<String, String> detail = new LinkedHashMap<String, String>();
                    detail.put("conflictRuleCode", code2);
                    detail.put("conflictRuleName", rs.getString("name2"));
                    f.setDetailJson(toJsonString(detail));

                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测规则冲突失败", ex);
        }

        return findings;
    }

    /**
     * 检测低置信度资产：查询 AI 生成结果中置信度低于阈值的记录。
     */
    public List<QualityFinding> checkLowConfidence(Long tenantId, double threshold) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        // 检查 AI 知识生产任务中低置信度的结果
        String jobSql = "SELECT j.job_code, j.job_name, j.job_type, j.output_summary "
                + "FROM ai_knowledge_job j "
                + "WHERE j.tenant_id = ? AND j.status = 'SUCCESS' "
                + "AND j.review_status = 'PENDING'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(jobSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String outputSummary = rs.getString("output_summary");
                    Double confidence = extractConfidence(outputSummary);
                    if (confidence != null && confidence < threshold) {
                        QualityFinding f = new QualityFinding();
                        f.setTenantId(tenantId);
                        f.setFindingType("LOW_CONFIDENCE");
                        f.setSeverity("WARNING");
                        f.setAssetType("KNOWLEDGE_ASSET");
                        f.setAssetCode(rs.getString("job_code"));
                        f.setAssetName(rs.getString("job_name"));
                        f.setAssetVersion(null);
                        f.setDescription("AI 生成结果 [" + rs.getString("job_code") + "] 置信度 " + String.format("%.2f", confidence) + " 低于阈值 " + String.format("%.2f", threshold));
                        f.setDetectionRule("AI 生成结果置信度低于 " + String.format("%.2f", threshold) + " 需人工审核");
                        f.setStatus("OPEN");
                        f.setCreatedBy("system");
                        f.setCreatedTime(LocalDateTime.now());

                        Map<String, Object> detail = new LinkedHashMap<String, Object>();
                        detail.put("confidence", confidence);
                        detail.put("threshold", threshold);
                        f.setDetailJson(toJsonString(detail));

                        findings.add(f);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("检测低置信度资产失败", ex);
        }

        return findings;
    }

    /**
     * 检测多候选冲突：查询同一输入有多个候选结果且无确认的记录。
     */
    public List<QualityFinding> checkMultiCandidateConflicts(Long tenantId) {
        List<QualityFinding> findings = new ArrayList<QualityFinding>();

        // 检查术语映射中同一源编码有多个映射且无确认
        String termSql = "SELECT source_code, source_name, concept_type, COUNT(*) AS cnt "
                + "FROM concept_mapping "
                + "WHERE tenant_id = ? AND governance_status <> 'APPROVED' "
                + "GROUP BY source_code, source_name, concept_type "
                + "HAVING COUNT(*) > 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(termSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sourceCode = rs.getString("source_code");
                    int cnt = rs.getInt("cnt");

                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("MULTI_CANDIDATE_CONFLICT");
                    f.setSeverity("WARNING");
                    f.setAssetType("TERMINOLOGY_MAPPING");
                    f.setAssetCode(sourceCode);
                    f.setAssetName(rs.getString("source_name"));
                    f.setAssetVersion(null);
                    f.setDescription("术语 [" + sourceCode + "] 存在 " + cnt + " 个候选映射且无确认");
                    f.setDetectionRule("同一源编码不应存在多个未确认的候选映射");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());

                    Map<String, Object> detail = new LinkedHashMap<String, Object>();
                    detail.put("candidateCount", cnt);
                    detail.put("conceptType", rs.getString("concept_type"));
                    f.setDetailJson(toJsonString(detail));

                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测多候选冲突失败", ex);
        }

        // 检查 AI 知识生产任务中同一输入有多个候选结果
        String jobSql = "SELECT input_hash, COUNT(*) AS cnt "
                + "FROM ai_knowledge_job "
                + "WHERE tenant_id = ? AND status = 'SUCCESS' AND review_status = 'PENDING' "
                + "GROUP BY input_hash "
                + "HAVING COUNT(*) > 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(jobSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String inputHash = rs.getString("input_hash");
                    int cnt = rs.getInt("cnt");

                    QualityFinding f = new QualityFinding();
                    f.setTenantId(tenantId);
                    f.setFindingType("MULTI_CANDIDATE_CONFLICT");
                    f.setSeverity("WARNING");
                    f.setAssetType("KNOWLEDGE_ASSET");
                    f.setAssetCode(inputHash != null ? inputHash : "N/A");
                    f.setAssetName("AI生成候选集");
                    f.setAssetVersion(null);
                    f.setDescription("同一输入存在 " + cnt + " 个候选 AI 生成结果且无确认");
                    f.setDetectionRule("同一输入不应存在多个未确认的 AI 生成结果");
                    f.setStatus("OPEN");
                    f.setCreatedBy("system");
                    f.setCreatedTime(LocalDateTime.now());

                    Map<String, Object> detail = new LinkedHashMap<String, Object>();
                    detail.put("candidateCount", cnt);
                    detail.put("inputHash", inputHash);
                    f.setDetailJson(toJsonString(detail));

                    findings.add(f);
                }
            }
        } catch (SQLException ex) {
            log.error("检测AI多候选冲突失败", ex);
        }

        return findings;
    }

    /**
     * 保存质检发现。
     */
    public QualityFinding saveFinding(QualityFinding finding) {
        if (finding.getId() == null) {
            finding.setId(Ids.next());
        }
        if (finding.getFindingCode() == null) {
            finding.setFindingCode("QF-" + String.format("%04d", finding.getId() % 10000));
        }
        if (finding.getCreatedTime() == null) {
            finding.setCreatedTime(LocalDateTime.now());
        }
        if (finding.getStatus() == null) {
            finding.setStatus("OPEN");
        }

        // 检查是否已存在相同发现（同 tenant + findingType + assetType + assetCode + status=OPEN），避免重复
        String checkSql = "SELECT id FROM quality_finding "
                + "WHERE tenant_id = ? AND finding_type = ? AND asset_type = ? AND asset_code = ? AND status = 'OPEN'";
        try (Connection connection = connection();
             PreparedStatement checkPs = connection.prepareStatement(checkSql)) {
            checkPs.setLong(1, finding.getTenantId());
            checkPs.setString(2, finding.getFindingType());
            checkPs.setString(3, finding.getAssetType());
            checkPs.setString(4, finding.getAssetCode());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    // 已存在相同的 OPEN 发现，跳过
                    return finding;
                }
            }
        } catch (SQLException ex) {
            log.warn("检查重复质检发现失败，继续插入", ex);
        }

        String sql = "INSERT INTO quality_finding (id, tenant_id, finding_code, finding_type, severity, "
                + "asset_type, asset_code, asset_name, asset_version, description, detail_json, "
                + "detection_rule, status, resolved_by, resolution_note, resolved_time, "
                + "created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, finding.getId());
            ps.setLong(2, finding.getTenantId());
            ps.setString(3, finding.getFindingCode());
            ps.setString(4, finding.getFindingType());
            ps.setString(5, finding.getSeverity());
            ps.setString(6, finding.getAssetType());
            ps.setString(7, finding.getAssetCode());
            ps.setString(8, finding.getAssetName());
            ps.setString(9, finding.getAssetVersion());
            ps.setString(10, finding.getDescription());
            ps.setString(11, finding.getDetailJson());
            ps.setString(12, finding.getDetectionRule());
            ps.setString(13, finding.getStatus());
            ps.setString(14, finding.getResolvedBy());
            ps.setString(15, finding.getResolutionNote());
            ps.setTimestamp(16, finding.getResolvedTime() != null ? Timestamp.valueOf(finding.getResolvedTime()) : null);
            ps.setString(17, finding.getCreatedBy());
            ps.setTimestamp(18, Timestamp.valueOf(finding.getCreatedTime()));
            ps.setString(19, finding.getUpdatedBy());
            ps.setTimestamp(20, finding.getUpdatedTime() != null ? Timestamp.valueOf(finding.getUpdatedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("保存质检发现失败: " + ex.getMessage(), ex);
        }
        return finding;
    }

    /**
     * 查询质检发现。
     */
    public List<QualityFinding> listFindings(Long tenantId, String findingType, String severity, String status, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM quality_finding WHERE tenant_id = ?");
        List<String> params = new ArrayList<String>();
        params.add(String.valueOf(tenantId));
        if (findingType != null && !findingType.isEmpty()) {
            sql.append(" AND finding_type = ?");
            params.add(findingType);
        }
        if (severity != null && !severity.isEmpty()) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<QualityFinding> findings = new ArrayList<QualityFinding>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    findings.add(mapFinding(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询质检发现失败: " + ex.getMessage(), ex);
        }
        return findings;
    }

    /**
     * 解决质检发现。
     */
    public void resolveFinding(Long findingId, String resolvedBy, String resolutionNote) {
        String sql = "UPDATE quality_finding SET status = 'RESOLVED', resolved_by = ?, "
                + "resolution_note = ?, resolved_time = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolvedBy);
            ps.setString(2, resolutionNote);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, findingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("解决质检发现失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 获取质检摘要统计。
     */
    public Map<String, Object> getQualitySummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);
        summary.put("generatedAt", LocalDateTime.now().toString());

        // 按发现类型统计
        String typeSql = "SELECT finding_type, COUNT(*) AS cnt FROM quality_finding "
                + "WHERE tenant_id = ? GROUP BY finding_type";
        Map<String, Long> byType = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(typeSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byType.put(rs.getString("finding_type"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计质检发现类型失败", ex);
        }
        summary.put("byType", byType);

        // 按严重程度统计
        String severitySql = "SELECT severity, COUNT(*) AS cnt FROM quality_finding "
                + "WHERE tenant_id = ? GROUP BY severity";
        Map<String, Long> bySeverity = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(severitySql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bySeverity.put(rs.getString("severity"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计质检严重程度失败", ex);
        }
        summary.put("bySeverity", bySeverity);

        // 按状态统计
        String statusSql = "SELECT status, COUNT(*) AS cnt FROM quality_finding "
                + "WHERE tenant_id = ? GROUP BY status";
        Map<String, Long> byStatus = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(statusSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byStatus.put(rs.getString("status"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计质检状态失败", ex);
        }
        summary.put("byStatus", byStatus);

        // 总数
        long total = 0;
        for (Long cnt : byType.values()) {
            total += cnt;
        }
        summary.put("total", total);

        return summary;
    }

    // ---- 内部方法 ----

    private QualityFinding mapFinding(ResultSet rs) throws SQLException {
        QualityFinding f = new QualityFinding();
        f.setId(rs.getLong("id"));
        f.setTenantId(rs.getLong("tenant_id"));
        f.setFindingCode(rs.getString("finding_code"));
        f.setFindingType(rs.getString("finding_type"));
        f.setSeverity(rs.getString("severity"));
        f.setAssetType(rs.getString("asset_type"));
        f.setAssetCode(rs.getString("asset_code"));
        f.setAssetName(rs.getString("asset_name"));
        f.setAssetVersion(rs.getString("asset_version"));
        f.setDescription(rs.getString("description"));
        f.setDetailJson(rs.getString("detail_json"));
        f.setDetectionRule(rs.getString("detection_rule"));
        f.setStatus(rs.getString("status"));
        f.setResolvedBy(rs.getString("resolved_by"));
        f.setResolutionNote(rs.getString("resolution_note"));
        Timestamp resolvedTime = rs.getTimestamp("resolved_time");
        if (resolvedTime != null) {
            f.setResolvedTime(resolvedTime.toLocalDateTime());
        }
        f.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            f.setCreatedTime(createdTime.toLocalDateTime());
        }
        f.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            f.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return f;
    }

    /**
     * 从 output_summary 中提取置信度值。
     * output_summary 可能包含 JSON 格式的 confidence 字段。
     */
    private Double extractConfidence(String outputSummary) {
        if (outputSummary == null || outputSummary.isEmpty()) {
            return null;
        }
        // 尝试从 JSON 中提取 confidence 字段
        String lower = outputSummary.toLowerCase();
        int idx = lower.indexOf("\"confidence\"");
        if (idx < 0) {
            idx = lower.indexOf("confidence");
        }
        if (idx >= 0) {
            // 找到 confidence 后面的数值
            String tail = outputSummary.substring(idx);
            int colonIdx = tail.indexOf(':');
            if (colonIdx >= 0) {
                String valuePart = tail.substring(colonIdx + 1).trim();
                // 提取数字部分
                StringBuilder numBuilder = new StringBuilder();
                for (int i = 0; i < valuePart.length(); i++) {
                    char c = valuePart.charAt(i);
                    if (c == '.' || (c >= '0' && c <= '9') || (numBuilder.length() == 0 && c == '-')) {
                        numBuilder.append(c);
                    } else if (numBuilder.length() > 0) {
                        break;
                    }
                }
                if (numBuilder.length() > 0) {
                    try {
                        return Double.parseDouble(numBuilder.toString());
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 简单的 Map 转 JSON 字符串（不依赖外部 JSON 库）。
     */
    private String toJsonString(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
        return dataSource.getConnection();
    }
}
