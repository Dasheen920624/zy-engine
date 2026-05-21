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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 候选配置审核服务：管理 AI 生成候选的提交、审核、批量审核和统计。
 */
@Service
public class AiCandidateReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiCandidateReviewService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AiCandidateReviewService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 提交 AI 候选。
     */
    public AiCandidateReview submitCandidate(AiCandidateReview candidate) {
        candidate.setId(Ids.next());
        if (candidate.getCandidateCode() == null) {
            candidate.setCandidateCode("CAND-" + String.format("%04d", candidate.getId() % 10000));
        }
        candidate.setCreatedTime(LocalDateTime.now());
        if (candidate.getReviewStatus() == null) {
            candidate.setReviewStatus("PENDING");
        }
        if (candidate.getPriority() == null) {
            candidate.setPriority("MEDIUM");
        }

        String sql = "INSERT INTO ai_candidate_review (id, tenant_id, candidate_code, candidate_type, "
                + "candidate_name, source_code, source_name, model_provider, model_name, "
                + "confidence, candidate_content, review_status, priority, "
                + "quality_findings, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, candidate.getId());
            ps.setLong(2, candidate.getTenantId());
            ps.setString(3, candidate.getCandidateCode());
            ps.setString(4, candidate.getCandidateType());
            ps.setString(5, candidate.getCandidateName());
            ps.setString(6, candidate.getSourceCode());
            ps.setString(7, candidate.getSourceName());
            ps.setString(8, candidate.getModelProvider());
            ps.setString(9, candidate.getModelName());
            ps.setObject(10, candidate.getConfidence());
            ps.setString(11, candidate.getCandidateContent());
            ps.setString(12, candidate.getReviewStatus());
            ps.setString(13, candidate.getPriority());
            ps.setString(14, candidate.getQualityFindings());
            ps.setString(15, candidate.getCreatedBy());
            ps.setTimestamp(16, Timestamp.valueOf(candidate.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("提交AI候选失败: " + ex.getMessage(), ex);
        }
        return candidate;
    }

    /**
     * 查询候选列表。
     */
    public List<AiCandidateReview> listCandidates(Long tenantId, String candidateType,
                                                    String reviewStatus, String priority, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_candidate_review WHERE tenant_id = ?");
        List<String> params = new ArrayList<String>();
        params.add(String.valueOf(tenantId));
        if (candidateType != null && !candidateType.isEmpty()) {
            sql.append(" AND candidate_type = ?");
            params.add(candidateType);
        }
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            sql.append(" AND review_status = ?");
            params.add(reviewStatus);
        }
        if (priority != null && !priority.isEmpty()) {
            sql.append(" AND priority = ?");
            params.add(priority);
        }
        sql.append(" ORDER BY created_time DESC");

        List<AiCandidateReview> candidates = new ArrayList<AiCandidateReview>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    candidates.add(mapCandidate(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询候选列表失败: " + ex.getMessage(), ex);
        }
        return candidates;
    }

    /**
     * 获取候选详情。
     */
    public AiCandidateReview getCandidate(Long candidateId) {
        String sql = "SELECT * FROM ai_candidate_review WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCandidate(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询候选详情失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 审核候选。
     */
    public void reviewCandidate(Long candidateId, String reviewStatus, String reviewedBy,
                                  String reviewNote, String modifiedContent) {
        StringBuilder sql = new StringBuilder("UPDATE ai_candidate_review SET review_status = ?, "
                + "reviewed_by = ?, reviewed_time = ?, review_note = ?, updated_time = ?");
        if (modifiedContent != null && !modifiedContent.isEmpty()) {
            sql.append(", modified_content = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, reviewStatus);
            ps.setString(idx++, reviewedBy);
            ps.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(idx++, reviewNote);
            ps.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now()));
            if (modifiedContent != null && !modifiedContent.isEmpty()) {
                ps.setString(idx++, modifiedContent);
            }
            ps.setLong(idx, candidateId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审核候选失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 批量审核。
     */
    public void batchReview(List<Long> candidateIds, String reviewStatus, String reviewedBy, String reviewNote) {
        for (Long candidateId : candidateIds) {
            reviewCandidate(candidateId, reviewStatus, reviewedBy, reviewNote, null);
        }
    }

    /**
     * 审核统计。
     */
    public Map<String, Object> getReviewSummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();

        // 按审核状态统计
        String statusSql = "SELECT review_status, COUNT(*) AS cnt FROM ai_candidate_review "
                + "WHERE tenant_id = ? GROUP BY review_status";
        Map<String, Long> byStatus = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(statusSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byStatus.put(rs.getString("review_status"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计审核状态失败", ex);
        }
        summary.put("byReviewStatus", byStatus);

        // 按候选类型统计
        String typeSql = "SELECT candidate_type, COUNT(*) AS cnt FROM ai_candidate_review "
                + "WHERE tenant_id = ? GROUP BY candidate_type";
        Map<String, Long> byType = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(typeSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byType.put(rs.getString("candidate_type"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计候选类型失败", ex);
        }
        summary.put("byCandidateType", byType);

        // 按优先级统计
        String prioritySql = "SELECT priority, COUNT(*) AS cnt FROM ai_candidate_review "
                + "WHERE tenant_id = ? GROUP BY priority";
        Map<String, Long> byPriority = new LinkedHashMap<String, Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(prioritySql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byPriority.put(rs.getString("priority"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException ex) {
            log.error("统计优先级失败", ex);
        }
        summary.put("byPriority", byPriority);

        // 总数
        long total = 0;
        for (Long cnt : byStatus.values()) {
            total += cnt;
        }
        summary.put("total", total);

        return summary;
    }

    /**
     * 审核历史。
     */
    public List<AiCandidateReview> getReviewHistory(Long tenantId, String reviewedBy, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_candidate_review WHERE tenant_id = ? AND review_status != 'PENDING'");
        List<String> params = new ArrayList<String>();
        params.add(String.valueOf(tenantId));
        if (reviewedBy != null && !reviewedBy.isEmpty()) {
            sql.append(" AND reviewed_by = ?");
            params.add(reviewedBy);
        }
        sql.append(" ORDER BY reviewed_time DESC");

        List<AiCandidateReview> candidates = new ArrayList<AiCandidateReview>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    candidates.add(mapCandidate(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询审核历史失败: " + ex.getMessage(), ex);
        }
        return candidates;
    }

    // ---- 内部方法 ----

    private AiCandidateReview mapCandidate(ResultSet rs) throws SQLException {
        AiCandidateReview candidate = new AiCandidateReview();
        candidate.setId(rs.getLong("id"));
        candidate.setTenantId(rs.getLong("tenant_id"));
        candidate.setCandidateCode(rs.getString("candidate_code"));
        candidate.setCandidateType(rs.getString("candidate_type"));
        candidate.setCandidateName(rs.getString("candidate_name"));
        candidate.setSourceCode(rs.getString("source_code"));
        candidate.setSourceName(rs.getString("source_name"));
        candidate.setModelProvider(rs.getString("model_provider"));
        candidate.setModelName(rs.getString("model_name"));
        double confidence = rs.getDouble("confidence");
        if (!rs.wasNull()) {
            candidate.setConfidence(confidence);
        }
        candidate.setCandidateContent(rs.getString("candidate_content"));
        candidate.setReviewStatus(rs.getString("review_status"));
        candidate.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        if (reviewedTime != null) {
            candidate.setReviewedTime(reviewedTime.toLocalDateTime());
        }
        candidate.setReviewNote(rs.getString("review_note"));
        candidate.setModifiedContent(rs.getString("modified_content"));
        candidate.setQualityFindings(rs.getString("quality_findings"));
        candidate.setPriority(rs.getString("priority"));
        candidate.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            candidate.setCreatedTime(createdTime.toLocalDateTime());
        }
        candidate.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            candidate.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return candidate;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    }
}
