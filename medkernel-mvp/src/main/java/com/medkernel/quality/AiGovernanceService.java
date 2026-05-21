package com.medkernel.quality;

import com.medkernel.llm.AiModelRegistry;
import com.medkernel.llm.ModelEvalTask;
import com.medkernel.llm.PromptTemplate;
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

@Service
public class AiGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(AiGovernanceService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AiGovernanceService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // =========================================================================
    // 模型注册管理
    // =========================================================================

    public AiModelRegistry registerModel(AiModelRegistry model) {
        model.setId(Ids.next());
        if (model.getStatus() == null) {
            model.setStatus("REGISTERED");
        }
        if (model.getReviewStatus() == null) {
            model.setReviewStatus("PENDING");
        }
        if (model.getEnabled() == null) {
            model.setEnabled("Y");
        }
        model.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO ai_model_registry (id, tenant_id, model_code, model_name, model_provider, "
                + "model_version, model_type, endpoint_url, api_key_ref, timeout_ms, max_tokens, temperature, "
                + "status, review_status, reviewed_by, reviewed_time, review_note, enabled, description, "
                + "created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, model.getId());
            ps.setLong(i++, model.getTenantId());
            ps.setString(i++, model.getModelCode());
            ps.setString(i++, model.getModelName());
            ps.setString(i++, model.getModelProvider());
            ps.setString(i++, model.getModelVersion());
            ps.setString(i++, model.getModelType());
            ps.setString(i++, model.getEndpointUrl());
            ps.setString(i++, model.getApiKeyRef());
            ps.setInt(i++, model.getTimeoutMs());
            ps.setInt(i++, model.getMaxTokens());
            ps.setDouble(i++, model.getTemperature());
            ps.setString(i++, model.getStatus());
            ps.setString(i++, model.getReviewStatus());
            ps.setString(i++, model.getReviewedBy());
            ps.setTimestamp(i++, model.getReviewedTime() != null ? Timestamp.valueOf(model.getReviewedTime()) : null);
            ps.setString(i++, model.getReviewNote());
            ps.setString(i++, model.getEnabled());
            ps.setString(i++, model.getDescription());
            ps.setString(i++, model.getCreatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(model.getCreatedTime()));
            ps.setString(i++, model.getUpdatedBy());
            ps.setTimestamp(i++, model.getUpdatedTime() != null ? Timestamp.valueOf(model.getUpdatedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("注册模型失败: " + ex.getMessage(), ex);
        }
        return model;
    }

    public AiModelRegistry updateModel(AiModelRegistry model) {
        String sql = "UPDATE ai_model_registry SET model_name=?, model_provider=?, model_version=?, "
                + "model_type=?, endpoint_url=?, api_key_ref=?, timeout_ms=?, max_tokens=?, temperature=?, "
                + "description=?, updated_by=?, updated_time=? WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, model.getModelName());
            ps.setString(i++, model.getModelProvider());
            ps.setString(i++, model.getModelVersion());
            ps.setString(i++, model.getModelType());
            ps.setString(i++, model.getEndpointUrl());
            ps.setString(i++, model.getApiKeyRef());
            ps.setInt(i++, model.getTimeoutMs());
            ps.setInt(i++, model.getMaxTokens());
            ps.setDouble(i++, model.getTemperature());
            ps.setString(i++, model.getDescription());
            ps.setString(i++, model.getUpdatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, model.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("更新模型失败: " + ex.getMessage(), ex);
        }
        return model;
    }

    public List<AiModelRegistry> listModels(Long tenantId, String modelType, String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_model_registry WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (modelType != null && !modelType.isEmpty()) {
            sql.append(" AND model_type = ?");
            params.add(modelType);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<AiModelRegistry> models = new ArrayList<AiModelRegistry>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    models.add(mapModel(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询模型列表失败: " + ex.getMessage(), ex);
        }
        return models;
    }

    public AiModelRegistry getModel(Long modelId) {
        String sql = "SELECT * FROM ai_model_registry WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapModel(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询模型失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    public void reviewModel(Long modelId, String reviewStatus, String reviewedBy, String reviewNote) {
        String sql = "UPDATE ai_model_registry SET review_status = ?, reviewed_by = ?, "
                + "reviewed_time = ?, review_note = ?, status = CASE WHEN ? = 'APPROVED' THEN 'APPROVED' ELSE status END, "
                + "updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, reviewStatus);
            ps.setString(2, reviewedBy);
            ps.setTimestamp(3, now);
            ps.setString(4, reviewNote);
            ps.setString(5, reviewStatus);
            ps.setTimestamp(6, now);
            ps.setLong(7, modelId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审核模型失败: " + ex.getMessage(), ex);
        }
    }

    public void onlineModel(Long modelId) {
        AiModelRegistry model = getModel(modelId);
        if (model == null) {
            throw new IllegalStateException("模型不存在: " + modelId);
        }
        if (!"APPROVED".equals(model.getReviewStatus())) {
            throw new IllegalStateException("模型未通过审核，无法上线");
        }
        String sql = "UPDATE ai_model_registry SET status = 'ONLINE', enabled = 'Y', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, modelId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("模型上线失败: " + ex.getMessage(), ex);
        }
    }

    public void offlineModel(Long modelId) {
        String sql = "UPDATE ai_model_registry SET status = 'OFFLINE', enabled = 'N', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, modelId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("模型下线失败: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // 提示词模板管理
    // =========================================================================

    public PromptTemplate savePromptTemplate(PromptTemplate template) {
        if (template.getId() == null) {
            template.setId(Ids.next());
            if (template.getStatus() == null) {
                template.setStatus("DRAFT");
            }
            if (template.getReviewStatus() == null) {
                template.setReviewStatus("PENDING");
            }
            if (template.getEnabled() == null) {
                template.setEnabled("Y");
            }
            template.setCreatedTime(LocalDateTime.now());

            String sql = "INSERT INTO ai_prompt_template (id, tenant_id, template_code, template_name, template_type, "
                    + "model_type, content, version, variables, hash, status, review_status, reviewed_by, "
                    + "reviewed_time, review_note, enabled, description, created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, template.getId());
                ps.setLong(i++, template.getTenantId());
                ps.setString(i++, template.getTemplateCode());
                ps.setString(i++, template.getTemplateName());
                ps.setString(i++, template.getTemplateType());
                ps.setString(i++, template.getModelType());
                ps.setString(i++, template.getContent());
                ps.setString(i++, template.getVersion());
                ps.setString(i++, template.getVariables());
                ps.setString(i++, template.getHash());
                ps.setString(i++, template.getStatus());
                ps.setString(i++, template.getReviewStatus());
                ps.setString(i++, template.getReviewedBy());
                ps.setTimestamp(i++, template.getReviewedTime() != null ? Timestamp.valueOf(template.getReviewedTime()) : null);
                ps.setString(i++, template.getReviewNote());
                ps.setString(i++, template.getEnabled());
                ps.setString(i++, template.getDescription());
                ps.setString(i++, template.getCreatedBy());
                ps.setTimestamp(i++, Timestamp.valueOf(template.getCreatedTime()));
                ps.setString(i++, template.getUpdatedBy());
                ps.setTimestamp(i++, template.getUpdatedTime() != null ? Timestamp.valueOf(template.getUpdatedTime()) : null);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("保存提示词模板失败: " + ex.getMessage(), ex);
            }
        } else {
            String sql = "UPDATE ai_prompt_template SET template_name=?, template_type=?, model_type=?, "
                    + "content=?, version=?, variables=?, hash=?, description=?, updated_by=?, updated_time=? "
                    + "WHERE id=?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, template.getTemplateName());
                ps.setString(i++, template.getTemplateType());
                ps.setString(i++, template.getModelType());
                ps.setString(i++, template.getContent());
                ps.setString(i++, template.getVersion());
                ps.setString(i++, template.getVariables());
                ps.setString(i++, template.getHash());
                ps.setString(i++, template.getDescription());
                ps.setString(i++, template.getUpdatedBy());
                ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(i++, template.getId());
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新提示词模板失败: " + ex.getMessage(), ex);
            }
        }
        return template;
    }

    public List<PromptTemplate> listPromptTemplates(Long tenantId, String templateType, String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_prompt_template WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (templateType != null && !templateType.isEmpty()) {
            sql.append(" AND template_type = ?");
            params.add(templateType);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<PromptTemplate> templates = new ArrayList<PromptTemplate>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    templates.add(mapPromptTemplate(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询提示词模板列表失败: " + ex.getMessage(), ex);
        }
        return templates;
    }

    public PromptTemplate getPromptTemplate(Long templateId) {
        String sql = "SELECT * FROM ai_prompt_template WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPromptTemplate(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询提示词模板失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    public void reviewPromptTemplate(Long templateId, String reviewStatus, String reviewedBy, String reviewNote) {
        String sql = "UPDATE ai_prompt_template SET review_status = ?, reviewed_by = ?, "
                + "reviewed_time = ?, review_note = ?, status = CASE WHEN ? = 'APPROVED' THEN 'APPROVED' ELSE status END, "
                + "updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, reviewStatus);
            ps.setString(2, reviewedBy);
            ps.setTimestamp(3, now);
            ps.setString(4, reviewNote);
            ps.setString(5, reviewStatus);
            ps.setTimestamp(6, now);
            ps.setLong(7, templateId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审核提示词模板失败: " + ex.getMessage(), ex);
        }
    }

    public void publishPromptTemplate(Long templateId) {
        PromptTemplate template = getPromptTemplate(templateId);
        if (template == null) {
            throw new IllegalStateException("提示词模板不存在: " + templateId);
        }
        if (!"APPROVED".equals(template.getReviewStatus())) {
            throw new IllegalStateException("提示词模板未通过审核，无法发布");
        }
        String sql = "UPDATE ai_prompt_template SET status = 'PUBLISHED', enabled = 'Y', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, templateId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("发布提示词模板失败: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // 评测任务管理
    // =========================================================================

    public ModelEvalTask createEvalTask(ModelEvalTask task) {
        task.setId(Ids.next());
        if (task.getTaskCode() == null) {
            task.setTaskCode("EVAL-" + String.format("%04d", task.getId() % 10000));
        }
        if (task.getStatus() == null) {
            task.setStatus("PENDING");
        }
        task.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO ai_model_eval_task (id, tenant_id, task_code, task_name, model_code, "
                + "model_version, prompt_template_code, prompt_version, benchmark_code, benchmark_name, "
                + "sample_size, status, accuracy_score, latency_ms, pass_rate, result_summary, detail_json, "
                + "created_by, created_time, completed_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, task.getId());
            ps.setLong(i++, task.getTenantId());
            ps.setString(i++, task.getTaskCode());
            ps.setString(i++, task.getTaskName());
            ps.setString(i++, task.getModelCode());
            ps.setString(i++, task.getModelVersion());
            ps.setString(i++, task.getPromptTemplateCode());
            ps.setString(i++, task.getPromptVersion());
            ps.setString(i++, task.getBenchmarkCode());
            ps.setString(i++, task.getBenchmarkName());
            ps.setInt(i++, task.getSampleSize());
            ps.setString(i++, task.getStatus());
            ps.setObject(i++, task.getAccuracyScore());
            ps.setObject(i++, task.getLatencyMs());
            ps.setObject(i++, task.getPassRate());
            ps.setString(i++, task.getResultSummary());
            ps.setString(i++, task.getDetailJson());
            ps.setString(i++, task.getCreatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(task.getCreatedTime()));
            ps.setTimestamp(i++, task.getCompletedTime() != null ? Timestamp.valueOf(task.getCompletedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建评测任务失败: " + ex.getMessage(), ex);
        }
        return task;
    }

    public void updateEvalTaskStatus(Long taskId, String status, Double accuracyScore, Double latencyMs,
                                     Double passRate, String resultSummary) {
        boolean isTerminal = "COMPLETED".equals(status) || "FAILED".equals(status);
        String sql;
        if (isTerminal) {
            sql = "UPDATE ai_model_eval_task SET status = ?, accuracy_score = ?, latency_ms = ?, "
                    + "pass_rate = ?, result_summary = ?, completed_time = ? WHERE id = ?";
        } else {
            sql = "UPDATE ai_model_eval_task SET status = ?, accuracy_score = ?, latency_ms = ?, "
                    + "pass_rate = ?, result_summary = ? WHERE id = ?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, status);
            ps.setObject(i++, accuracyScore);
            ps.setObject(i++, latencyMs);
            ps.setObject(i++, passRate);
            ps.setString(i++, resultSummary);
            if (isTerminal) {
                ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            }
            ps.setLong(i++, taskId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("更新评测任务状态失败: " + ex.getMessage(), ex);
        }
    }

    public List<ModelEvalTask> listEvalTasks(Long tenantId, String modelCode, String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_model_eval_task WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (modelCode != null && !modelCode.isEmpty()) {
            sql.append(" AND model_code = ?");
            params.add(modelCode);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<ModelEvalTask> tasks = new ArrayList<ModelEvalTask>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapEvalTask(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询评测任务列表失败: " + ex.getMessage(), ex);
        }
        return tasks;
    }

    public Map<String, Object> getEvalSummary(Long tenantId, String modelCode) {
        String sql = "SELECT model_code, COUNT(*) as total_tasks, "
                + "AVG(accuracy_score) as avg_accuracy, AVG(latency_ms) as avg_latency, "
                + "AVG(pass_rate) as avg_pass_rate "
                + "FROM ai_model_eval_task WHERE tenant_id = ?";
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (modelCode != null && !modelCode.isEmpty()) {
            sql += " AND model_code = ?";
            params.add(modelCode);
        }
        sql += " GROUP BY model_code";

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);
        List<Map<String, Object>> modelSummaries = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("modelCode", rs.getString("model_code"));
                    row.put("totalTasks", rs.getLong("total_tasks"));
                    row.put("avgAccuracy", rs.getDouble("avg_accuracy"));
                    row.put("avgLatency", rs.getDouble("avg_latency"));
                    row.put("avgPassRate", rs.getDouble("avg_pass_rate"));
                    modelSummaries.add(row);
                }
            }
        } catch (SQLException ex) {
            log.error("获取评测摘要失败", ex);
        }
        summary.put("models", modelSummaries);
        return summary;
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private AiModelRegistry mapModel(ResultSet rs) throws SQLException {
        AiModelRegistry model = new AiModelRegistry();
        model.setId(rs.getLong("id"));
        model.setTenantId(rs.getLong("tenant_id"));
        model.setModelCode(rs.getString("model_code"));
        model.setModelName(rs.getString("model_name"));
        model.setModelProvider(rs.getString("model_provider"));
        model.setModelVersion(rs.getString("model_version"));
        model.setModelType(rs.getString("model_type"));
        model.setEndpointUrl(rs.getString("endpoint_url"));
        model.setApiKeyRef(rs.getString("api_key_ref"));
        model.setTimeoutMs(rs.getInt("timeout_ms"));
        model.setMaxTokens(rs.getInt("max_tokens"));
        model.setTemperature(rs.getDouble("temperature"));
        model.setStatus(rs.getString("status"));
        model.setReviewStatus(rs.getString("review_status"));
        model.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        if (reviewedTime != null) {
            model.setReviewedTime(reviewedTime.toLocalDateTime());
        }
        model.setReviewNote(rs.getString("review_note"));
        model.setEnabled(rs.getString("enabled"));
        model.setDescription(rs.getString("description"));
        model.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            model.setCreatedTime(createdTime.toLocalDateTime());
        }
        model.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            model.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return model;
    }

    private PromptTemplate mapPromptTemplate(ResultSet rs) throws SQLException {
        PromptTemplate template = new PromptTemplate();
        template.setId(rs.getLong("id"));
        template.setTenantId(rs.getLong("tenant_id"));
        template.setTemplateCode(rs.getString("template_code"));
        template.setTemplateName(rs.getString("template_name"));
        template.setTemplateType(rs.getString("template_type"));
        template.setModelType(rs.getString("model_type"));
        template.setContent(rs.getString("content"));
        template.setVersion(rs.getString("version"));
        template.setVariables(rs.getString("variables"));
        template.setHash(rs.getString("hash"));
        template.setStatus(rs.getString("status"));
        template.setReviewStatus(rs.getString("review_status"));
        template.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        if (reviewedTime != null) {
            template.setReviewedTime(reviewedTime.toLocalDateTime());
        }
        template.setReviewNote(rs.getString("review_note"));
        template.setEnabled(rs.getString("enabled"));
        template.setDescription(rs.getString("description"));
        template.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            template.setCreatedTime(createdTime.toLocalDateTime());
        }
        template.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            template.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return template;
    }

    private ModelEvalTask mapEvalTask(ResultSet rs) throws SQLException {
        ModelEvalTask task = new ModelEvalTask();
        task.setId(rs.getLong("id"));
        task.setTenantId(rs.getLong("tenant_id"));
        task.setTaskCode(rs.getString("task_code"));
        task.setTaskName(rs.getString("task_name"));
        task.setModelCode(rs.getString("model_code"));
        task.setModelVersion(rs.getString("model_version"));
        task.setPromptTemplateCode(rs.getString("prompt_template_code"));
        task.setPromptVersion(rs.getString("prompt_version"));
        task.setBenchmarkCode(rs.getString("benchmark_code"));
        task.setBenchmarkName(rs.getString("benchmark_name"));
        task.setSampleSize(rs.getInt("sample_size"));
        task.setStatus(rs.getString("status"));
        double accuracyScore = rs.getDouble("accuracy_score");
        if (!rs.wasNull()) {
            task.setAccuracyScore(accuracyScore);
        }
        double latencyMs = rs.getDouble("latency_ms");
        if (!rs.wasNull()) {
            task.setLatencyMs(latencyMs);
        }
        double passRate = rs.getDouble("pass_rate");
        if (!rs.wasNull()) {
            task.setPassRate(passRate);
        }
        task.setResultSummary(rs.getString("result_summary"));
        task.setDetailJson(rs.getString("detail_json"));
        task.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            task.setCreatedTime(createdTime.toLocalDateTime());
        }
        Timestamp completedTime = rs.getTimestamp("completed_time");
        if (completedTime != null) {
            task.setCompletedTime(completedTime.toLocalDateTime());
        }
        return task;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
        return dataSource.getConnection();
    }
}
