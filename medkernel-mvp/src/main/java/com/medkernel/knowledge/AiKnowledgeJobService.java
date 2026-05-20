package com.medkernel.knowledge;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
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
 * AI 知识生产任务服务：管理知识生产流水线任务和模型调用记录。
 * 支持任务创建、状态更新、审核、重试和模型调用日志记录。
 */
@Service
public class AiKnowledgeJobService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeJobService.class);

    private final EnginePersistenceProperties properties;

    public AiKnowledgeJobService(EnginePersistenceProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建知识生产任务。
     */
    public AiKnowledgeJob createJob(AiKnowledgeJob job) {
        job.setId(Ids.next());
        if (job.getJobCode() == null) {
            job.setJobCode("JOB-" + String.format("%04d", job.getId() % 10000));
        }
        job.setCreatedTime(LocalDateTime.now());
        if (job.getStatus() == null) job.setStatus("PENDING");
        if (job.getReviewStatus() == null) job.setReviewStatus("PENDING");
        if (job.getMaxRetries() == 0) job.setMaxRetries(3);

        String sql = "INSERT INTO ai_knowledge_job (id, tenant_id, job_code, job_name, job_type, "
                + "source_code, subscription_id, model_provider, model_name, prompt_version, "
                + "input_hash, output_hash, input_summary, output_summary, evidence_ids, "
                + "status, review_status, retry_count, max_retries, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, job.getId());
            ps.setLong(2, job.getTenantId());
            ps.setString(3, job.getJobCode());
            ps.setString(4, job.getJobName());
            ps.setString(5, job.getJobType());
            ps.setString(6, job.getSourceCode());
            ps.setString(7, job.getSubscriptionId());
            ps.setString(8, job.getModelProvider());
            ps.setString(9, job.getModelName());
            ps.setString(10, job.getPromptVersion());
            ps.setString(11, job.getInputHash());
            ps.setString(12, job.getOutputHash());
            ps.setString(13, job.getInputSummary());
            ps.setString(14, job.getOutputSummary());
            ps.setString(15, job.getEvidenceIds());
            ps.setString(16, job.getStatus());
            ps.setString(17, job.getReviewStatus());
            ps.setInt(18, job.getRetryCount());
            ps.setInt(19, job.getMaxRetries());
            ps.setString(20, job.getCreatedBy());
            ps.setTimestamp(21, Timestamp.valueOf(job.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建知识生产任务失败: " + ex.getMessage(), ex);
        }
        return job;
    }

    /**
     * 更新任务状态。
     */
    public void updateJobStatus(Long jobId, String status, String errorCode, String errorMessage) {
        String sql;
        if ("RUNNING".equals(status)) {
            sql = "UPDATE ai_knowledge_job SET status = ?, started_time = ?, updated_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(4, jobId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新任务状态失败: " + ex.getMessage(), ex);
            }
        } else if ("SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
            sql = "UPDATE ai_knowledge_job SET status = ?, error_code = ?, error_message = ?, "
                    + "finished_time = ?, duration_ms = ?, updated_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, errorCode);
                ps.setString(3, errorMessage);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(5, 0);
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(7, jobId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新任务状态失败: " + ex.getMessage(), ex);
            }
        } else if ("RETRY".equals(status)) {
            sql = "UPDATE ai_knowledge_job SET status = ?, retry_count = retry_count + 1, updated_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, "PENDING");
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(3, jobId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新任务重试状态失败: " + ex.getMessage(), ex);
            }
        } else {
            sql = "UPDATE ai_knowledge_job SET status = ?, updated_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(3, jobId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新任务状态失败: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 审核任务。
     */
    public void reviewJob(Long jobId, String reviewStatus, String reviewedBy, String reviewComment) {
        String sql = "UPDATE ai_knowledge_job SET review_status = ?, reviewed_by = ?, "
                + "reviewed_time = ?, review_comment = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reviewStatus);
            ps.setString(2, reviewedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, reviewComment);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, jobId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审核任务失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询任务列表。
     */
    public List<AiKnowledgeJob> listJobs(Long tenantId, String jobType, String status, String reviewStatus, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_knowledge_job WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(tenantId));
        if (jobType != null && !jobType.isEmpty()) { sql.append(" AND job_type = ?"); params.add(jobType); }
        if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); params.add(status); }
        if (reviewStatus != null && !reviewStatus.isEmpty()) { sql.append(" AND review_status = ?"); params.add(reviewStatus); }
        sql.append(" ORDER BY created_time DESC");

        List<AiKnowledgeJob> jobs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    jobs.add(mapJob(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询任务列表失败: " + ex.getMessage(), ex);
        }
        return jobs;
    }

    /**
     * 查询单个任务。
     */
    public AiKnowledgeJob getJob(Long jobId) {
        String sql = "SELECT * FROM ai_knowledge_job WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapJob(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询任务失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 记录模型调用日志。
     */
    public AiModelCallLog logModelCall(AiModelCallLog callLog) {
        callLog.setId(Ids.next());
        callLog.setCreatedTime(LocalDateTime.now());
        if (callLog.getCalledTime() == null) callLog.setCalledTime(LocalDateTime.now());

        String sql = "INSERT INTO ai_model_call_log (id, tenant_id, job_id, call_type, "
                + "model_provider, model_name, model_version, prompt_template_id, prompt_version, "
                + "prompt_hash, input_hash, output_hash, input_token_count, output_token_count, "
                + "total_token_count, call_status, error_code, error_message, fallback_used, "
                + "fallback_provider, fallback_model, trace_id, patient_id, encounter_id, "
                + "elapsed_ms, called_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, callLog.getId());
            ps.setLong(2, callLog.getTenantId());
            ps.setObject(3, callLog.getJobId());
            ps.setString(4, callLog.getCallType());
            ps.setString(5, callLog.getModelProvider());
            ps.setString(6, callLog.getModelName());
            ps.setString(7, callLog.getModelVersion());
            ps.setString(8, callLog.getPromptTemplateId());
            ps.setString(9, callLog.getPromptVersion());
            ps.setString(10, callLog.getPromptHash());
            ps.setString(11, callLog.getInputHash());
            ps.setString(12, callLog.getOutputHash());
            ps.setObject(13, callLog.getInputTokenCount());
            ps.setObject(14, callLog.getOutputTokenCount());
            ps.setObject(15, callLog.getTotalTokenCount());
            ps.setString(16, callLog.getCallStatus());
            ps.setString(17, callLog.getErrorCode());
            ps.setString(18, callLog.getErrorMessage());
            ps.setString(19, callLog.getFallbackUsed());
            ps.setString(20, callLog.getFallbackProvider());
            ps.setString(21, callLog.getFallbackModel());
            ps.setString(22, callLog.getTraceId());
            ps.setString(23, callLog.getPatientId());
            ps.setString(24, callLog.getEncounterId());
            ps.setObject(25, callLog.getElapsedMs());
            ps.setTimestamp(26, Timestamp.valueOf(callLog.getCalledTime()));
            ps.setString(27, callLog.getCreatedBy());
            ps.setTimestamp(28, Timestamp.valueOf(callLog.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("记录模型调用日志失败: " + ex.getMessage(), ex);
        }
        return callLog;
    }

    /**
     * 查询模型调用日志。
     */
    public List<AiModelCallLog> listModelCallLogs(Long tenantId, Long jobId, String callType, String callStatus, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_model_call_log WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(tenantId));
        if (jobId != null) { sql.append(" AND job_id = ?"); params.add(String.valueOf(jobId)); }
        if (callType != null && !callType.isEmpty()) { sql.append(" AND call_type = ?"); params.add(callType); }
        if (callStatus != null && !callStatus.isEmpty()) { sql.append(" AND call_status = ?"); params.add(callStatus); }
        sql.append(" ORDER BY called_time DESC");

        List<AiModelCallLog> logs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    logs.add(mapCallLog(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询模型调用日志失败: " + ex.getMessage(), ex);
        }
        return logs;
    }

    /**
     * 模型调用统计汇总。
     */
    public Map<String, Object> summarizeModelCalls(Long tenantId) {
        String sql = "SELECT call_status, COUNT(*) as cnt, AVG(elapsed_ms) as avg_ms "
                + "FROM ai_model_call_log WHERE tenant_id = ? GROUP BY call_status";
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("count", rs.getLong("cnt"));
                    row.put("avgMs", rs.getDouble("avg_ms"));
                    summary.put(rs.getString("call_status"), row);
                }
            }
        } catch (SQLException ex) {
            log.error("统计模型调用失败", ex);
        }
        return summary;
    }

    // ---- 内部方法 ----

    private AiKnowledgeJob mapJob(ResultSet rs) throws SQLException {
        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setId(rs.getLong("id"));
        job.setTenantId(rs.getLong("tenant_id"));
        job.setJobCode(rs.getString("job_code"));
        job.setJobName(rs.getString("job_name"));
        job.setJobType(rs.getString("job_type"));
        job.setSourceCode(rs.getString("source_code"));
        job.setSubscriptionId(rs.getString("subscription_id"));
        job.setModelProvider(rs.getString("model_provider"));
        job.setModelName(rs.getString("model_name"));
        job.setPromptVersion(rs.getString("prompt_version"));
        job.setInputHash(rs.getString("input_hash"));
        job.setOutputHash(rs.getString("output_hash"));
        job.setInputSummary(rs.getString("input_summary"));
        job.setOutputSummary(rs.getString("output_summary"));
        job.setEvidenceIds(rs.getString("evidence_ids"));
        job.setStatus(rs.getString("status"));
        job.setReviewStatus(rs.getString("review_status"));
        job.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        if (reviewedTime != null) job.setReviewedTime(reviewedTime.toLocalDateTime());
        job.setReviewComment(rs.getString("review_comment"));
        job.setErrorCode(rs.getString("error_code"));
        job.setErrorMessage(rs.getString("error_message"));
        job.setRetryCount(rs.getInt("retry_count"));
        job.setMaxRetries(rs.getInt("max_retries"));
        Timestamp startedTime = rs.getTimestamp("started_time");
        if (startedTime != null) job.setStartedTime(startedTime.toLocalDateTime());
        Timestamp finishedTime = rs.getTimestamp("finished_time");
        if (finishedTime != null) job.setFinishedTime(finishedTime.toLocalDateTime());
        job.setDurationMs(rs.getInt("duration_ms"));
        job.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) job.setCreatedTime(createdTime.toLocalDateTime());
        job.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) job.setUpdatedTime(updatedTime.toLocalDateTime());
        return job;
    }

    private AiModelCallLog mapCallLog(ResultSet rs) throws SQLException {
        AiModelCallLog callLog = new AiModelCallLog();
        callLog.setId(rs.getLong("id"));
        callLog.setTenantId(rs.getLong("tenant_id"));
        long jobId = rs.getLong("job_id");
        if (!rs.wasNull()) callLog.setJobId(jobId);
        callLog.setCallType(rs.getString("call_type"));
        callLog.setModelProvider(rs.getString("model_provider"));
        callLog.setModelName(rs.getString("model_name"));
        callLog.setModelVersion(rs.getString("model_version"));
        callLog.setPromptTemplateId(rs.getString("prompt_template_id"));
        callLog.setPromptVersion(rs.getString("prompt_version"));
        callLog.setPromptHash(rs.getString("prompt_hash"));
        callLog.setInputHash(rs.getString("input_hash"));
        callLog.setOutputHash(rs.getString("output_hash"));
        callLog.setInputTokenCount(rs.getInt("input_token_count"));
        callLog.setOutputTokenCount(rs.getInt("output_token_count"));
        callLog.setTotalTokenCount(rs.getInt("total_token_count"));
        callLog.setCallStatus(rs.getString("call_status"));
        callLog.setErrorCode(rs.getString("error_code"));
        callLog.setErrorMessage(rs.getString("error_message"));
        callLog.setFallbackUsed(rs.getString("fallback_used"));
        callLog.setFallbackProvider(rs.getString("fallback_provider"));
        callLog.setFallbackModel(rs.getString("fallback_model"));
        callLog.setTraceId(rs.getString("trace_id"));
        callLog.setPatientId(rs.getString("patient_id"));
        callLog.setEncounterId(rs.getString("encounter_id"));
        callLog.setElapsedMs(rs.getInt("elapsed_ms"));
        Timestamp calledTime = rs.getTimestamp("called_time");
        if (calledTime != null) callLog.setCalledTime(calledTime.toLocalDateTime());
        callLog.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) callLog.setCreatedTime(createdTime.toLocalDateTime());
        return callLog;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
    }
}
