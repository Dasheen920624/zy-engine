package com.medkernel.impl.service;

import com.medkernel.common.TraceContext;
import com.medkernel.impl.entity.IssueFeedback;
import com.medkernel.impl.entity.TrialPlan;
import com.medkernel.impl.entity.TrialRecord;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 试运行服务：试运行计划 + 试运行记录 + 问题反馈 CRUD
 */
@Service
public class TrialRunService {

    private static final Logger log = LoggerFactory.getLogger(TrialRunService.class);

    private final DataSource dataSource;

    public TrialRunService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== 试运行计划管理 ====================

    public TrialPlan createTrialPlan(TrialPlan plan) {
        String sql = "INSERT INTO impl_trial_plan (id, tenant_id, plan_code, plan_name, "
                + "description, start_date, end_date, scope, objectives, responsible_person, "
                + "status, approval_status, approved_by, approved_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            plan.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, plan.getTenantId());
            ps.setString(3, plan.getPlanCode());
            ps.setString(4, plan.getPlanName());
            ps.setString(5, plan.getDescription());
            if (plan.getStartDate() != null) {
                ps.setDate(6, Date.valueOf(plan.getStartDate()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            if (plan.getEndDate() != null) {
                ps.setDate(7, Date.valueOf(plan.getEndDate()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setString(8, plan.getScope());
            ps.setString(9, plan.getObjectives());
            ps.setString(10, plan.getResponsiblePerson());
            ps.setString(11, plan.getStatus() != null ? plan.getStatus() : "DRAFT");
            ps.setString(12, plan.getApprovalStatus() != null ? plan.getApprovalStatus() : "PENDING");
            ps.setString(13, plan.getApprovedBy());
            if (plan.getApprovedTime() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(plan.getApprovedTime()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.setString(15, TraceContext.getUsername());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created trial plan: id={}, code={}", id, plan.getPlanCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create trial plan failed: " + ex.getMessage(), ex);
        }
        return plan;
    }

    public TrialPlan updateTrialPlan(TrialPlan plan) {
        String sql = "UPDATE impl_trial_plan SET plan_name = ?, description = ?, "
                + "start_date = ?, end_date = ?, scope = ?, objectives = ?, "
                + "responsible_person = ?, status = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, plan.getPlanName());
            ps.setString(2, plan.getDescription());
            if (plan.getStartDate() != null) {
                ps.setDate(3, Date.valueOf(plan.getStartDate()));
            } else {
                ps.setNull(3, Types.DATE);
            }
            if (plan.getEndDate() != null) {
                ps.setDate(4, Date.valueOf(plan.getEndDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setString(5, plan.getScope());
            ps.setString(6, plan.getObjectives());
            ps.setString(7, plan.getResponsiblePerson());
            ps.setString(8, plan.getStatus());
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, plan.getId());
            ps.setLong(12, plan.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update trial plan failed: " + ex.getMessage(), ex);
        }
        return plan;
    }

    public List<TrialPlan> listTrialPlans(Long tenantId, String status, String approvalStatus) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, plan_code, plan_name, "
                + "description, start_date, end_date, scope, objectives, responsible_person, "
                + "status, approval_status, approved_by, approved_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_trial_plan WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (approvalStatus != null && !approvalStatus.isEmpty()) {
            sql.append(" AND approval_status = ?");
            params.add(approvalStatus);
        }
        sql.append(" ORDER BY created_time DESC");

        List<TrialPlan> plans = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 2, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plans.add(mapTrialPlan(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list trial plans failed: " + ex.getMessage(), ex);
        }
        return plans;
    }

    public TrialPlan getTrialPlan(Long planId) {
        String sql = "SELECT id, tenant_id, plan_code, plan_name, "
                + "description, start_date, end_date, scope, objectives, responsible_person, "
                + "status, approval_status, approved_by, approved_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_trial_plan WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTrialPlan(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get trial plan failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    public TrialPlan approvePlan(Long planId, Long tenantId, String approvedBy) {
        String sql = "UPDATE impl_trial_plan SET approval_status = 'APPROVED', "
                + "approved_by = ?, approved_time = ?, status = 'ACTIVE', "
                + "updated_by = ?, updated_time = ? WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, approvedBy);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, approvedBy);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setLong(5, planId);
            ps.setLong(6, tenantId);
            ps.executeUpdate();
            log.info("Approved trial plan: planId={}, approvedBy={}", planId, approvedBy);
        } catch (SQLException ex) {
            throw new IllegalStateException("approve trial plan failed: " + ex.getMessage(), ex);
        }
        return getTrialPlan(planId);
    }

    // ==================== 试运行记录管理 ====================

    public TrialRecord addTrialRecord(TrialRecord record) {
        String sql = "INSERT INTO impl_trial_record (id, tenant_id, plan_id, record_date, "
                + "participant_count, issue_count, resolved_count, summary, metrics_json, "
                + "created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            record.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, record.getTenantId());
            ps.setLong(3, record.getPlanId());
            if (record.getRecordDate() != null) {
                ps.setDate(4, Date.valueOf(record.getRecordDate()));
            } else {
                ps.setDate(4, Date.valueOf(java.time.LocalDate.now()));
            }
            ps.setInt(5, record.getParticipantCount() != null ? record.getParticipantCount() : 0);
            ps.setInt(6, record.getIssueCount() != null ? record.getIssueCount() : 0);
            ps.setInt(7, record.getResolvedCount() != null ? record.getResolvedCount() : 0);
            ps.setString(8, record.getSummary());
            ps.setString(9, record.getMetricsJson());
            ps.setString(10, TraceContext.getUsername());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Added trial record: id={}, planId={}", id, record.getPlanId());
        } catch (SQLException ex) {
            throw new IllegalStateException("add trial record failed: " + ex.getMessage(), ex);
        }
        return record;
    }

    public List<TrialRecord> listTrialRecords(Long tenantId, Long planId) {
        String sql = "SELECT id, tenant_id, plan_id, record_date, "
                + "participant_count, issue_count, resolved_count, summary, metrics_json, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_trial_record WHERE tenant_id = ? AND plan_id = ? "
                + "ORDER BY record_date DESC";
        List<TrialRecord> records = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapTrialRecord(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list trial records failed: " + ex.getMessage(), ex);
        }
        return records;
    }

    // ==================== 问题反馈管理 ====================

    public IssueFeedback createIssue(IssueFeedback issue) {
        String sql = "INSERT INTO impl_issue_feedback (id, tenant_id, plan_id, issue_code, title, "
                + "description, issue_type, severity, status, reported_by, reported_time, "
                + "assigned_to, resolved_by, resolved_time, resolution, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            issue.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, issue.getTenantId());
            if (issue.getPlanId() != null) {
                ps.setLong(3, issue.getPlanId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, issue.getIssueCode());
            ps.setString(5, issue.getTitle());
            ps.setString(6, issue.getDescription());
            ps.setString(7, issue.getIssueType());
            ps.setString(8, issue.getSeverity());
            ps.setString(9, issue.getStatus() != null ? issue.getStatus() : "OPEN");
            ps.setString(10, issue.getReportedBy());
            if (issue.getReportedTime() != null) {
                ps.setTimestamp(11, Timestamp.valueOf(issue.getReportedTime()));
            } else {
                ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            }
            ps.setString(12, issue.getAssignedTo());
            ps.setString(13, issue.getResolvedBy());
            if (issue.getResolvedTime() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(issue.getResolvedTime()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.setString(15, issue.getResolution());
            ps.setString(16, TraceContext.getUsername());
            ps.setTimestamp(17, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created issue feedback: id={}, code={}", id, issue.getIssueCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create issue feedback failed: " + ex.getMessage(), ex);
        }
        return issue;
    }

    public IssueFeedback updateIssue(IssueFeedback issue) {
        String sql = "UPDATE impl_issue_feedback SET title = ?, description = ?, "
                + "issue_type = ?, severity = ?, status = ?, assigned_to = ?, "
                + "updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, issue.getTitle());
            ps.setString(2, issue.getDescription());
            ps.setString(3, issue.getIssueType());
            ps.setString(4, issue.getSeverity());
            ps.setString(5, issue.getStatus());
            ps.setString(6, issue.getAssignedTo());
            ps.setString(7, TraceContext.getUsername());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(9, issue.getId());
            ps.setLong(10, issue.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update issue feedback failed: " + ex.getMessage(), ex);
        }
        return issue;
    }

    public List<IssueFeedback> listIssues(Long tenantId, Long planId, String status, String severity) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, plan_id, issue_code, title, "
                + "description, issue_type, severity, status, reported_by, reported_time, "
                + "assigned_to, resolved_by, resolved_time, resolution, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_issue_feedback WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        if (planId != null) {
            sql.append(" AND plan_id = ?");
            params.add(planId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (severity != null && !severity.isEmpty()) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        sql.append(" ORDER BY created_time DESC");

        List<IssueFeedback> issues = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 2, (Long) param);
                } else if (param instanceof String) {
                    ps.setString(i + 2, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    issues.add(mapIssueFeedback(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list issue feedbacks failed: " + ex.getMessage(), ex);
        }
        return issues;
    }

    public IssueFeedback resolveIssue(Long issueId, Long tenantId, String resolvedBy, String resolution) {
        String sql = "UPDATE impl_issue_feedback SET status = 'RESOLVED', "
                + "resolved_by = ?, resolved_time = ?, resolution = ?, "
                + "updated_by = ?, updated_time = ? WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, resolvedBy);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, resolution);
            ps.setString(4, resolvedBy);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setLong(6, issueId);
            ps.setLong(7, tenantId);
            ps.executeUpdate();
            log.info("Resolved issue feedback: issueId={}, resolvedBy={}", issueId, resolvedBy);
        } catch (SQLException ex) {
            throw new IllegalStateException("resolve issue feedback failed: " + ex.getMessage(), ex);
        }
        return getIssue(issueId);
    }

    public IssueFeedback getIssue(Long issueId) {
        String sql = "SELECT id, tenant_id, plan_id, issue_code, title, "
                + "description, issue_type, severity, status, reported_by, reported_time, "
                + "assigned_to, resolved_by, resolved_time, resolution, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_issue_feedback WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIssueFeedback(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get issue feedback failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== ResultSet 映射 ====================

    private TrialPlan mapTrialPlan(ResultSet rs) throws SQLException {
        TrialPlan p = new TrialPlan();
        p.setId(rs.getLong("id"));
        p.setTenantId(rs.getLong("tenant_id"));
        p.setPlanCode(rs.getString("plan_code"));
        p.setPlanName(rs.getString("plan_name"));
        p.setDescription(rs.getString("description"));
        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            p.setStartDate(startDate.toLocalDate());
        }
        Date endDate = rs.getDate("end_date");
        if (endDate != null) {
            p.setEndDate(endDate.toLocalDate());
        }
        p.setScope(rs.getString("scope"));
        p.setObjectives(rs.getString("objectives"));
        p.setResponsiblePerson(rs.getString("responsible_person"));
        p.setStatus(rs.getString("status"));
        p.setApprovalStatus(rs.getString("approval_status"));
        p.setApprovedBy(rs.getString("approved_by"));
        Timestamp approvedTime = rs.getTimestamp("approved_time");
        if (approvedTime != null) {
            p.setApprovedTime(approvedTime.toLocalDateTime());
        }
        p.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            p.setCreatedTime(createdTime.toLocalDateTime());
        }
        p.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            p.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return p;
    }

    private TrialRecord mapTrialRecord(ResultSet rs) throws SQLException {
        TrialRecord r = new TrialRecord();
        r.setId(rs.getLong("id"));
        r.setTenantId(rs.getLong("tenant_id"));
        r.setPlanId(rs.getLong("plan_id"));
        Date recordDate = rs.getDate("record_date");
        if (recordDate != null) {
            r.setRecordDate(recordDate.toLocalDate());
        }
        r.setParticipantCount(rs.getInt("participant_count"));
        r.setIssueCount(rs.getInt("issue_count"));
        r.setResolvedCount(rs.getInt("resolved_count"));
        r.setSummary(rs.getString("summary"));
        r.setMetricsJson(rs.getString("metrics_json"));
        r.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            r.setCreatedTime(createdTime.toLocalDateTime());
        }
        r.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            r.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return r;
    }

    private IssueFeedback mapIssueFeedback(ResultSet rs) throws SQLException {
        IssueFeedback f = new IssueFeedback();
        f.setId(rs.getLong("id"));
        f.setTenantId(rs.getLong("tenant_id"));
        long planId = rs.getLong("plan_id");
        if (!rs.wasNull()) {
            f.setPlanId(planId);
        }
        f.setIssueCode(rs.getString("issue_code"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        f.setIssueType(rs.getString("issue_type"));
        f.setSeverity(rs.getString("severity"));
        f.setStatus(rs.getString("status"));
        f.setReportedBy(rs.getString("reported_by"));
        Timestamp reportedTime = rs.getTimestamp("reported_time");
        if (reportedTime != null) {
            f.setReportedTime(reportedTime.toLocalDateTime());
        }
        f.setAssignedTo(rs.getString("assigned_to"));
        f.setResolvedBy(rs.getString("resolved_by"));
        Timestamp resolvedTime = rs.getTimestamp("resolved_time");
        if (resolvedTime != null) {
            f.setResolvedTime(resolvedTime.toLocalDateTime());
        }
        f.setResolution(rs.getString("resolution"));
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

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
