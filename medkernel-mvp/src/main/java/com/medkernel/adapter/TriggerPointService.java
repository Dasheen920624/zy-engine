package com.medkernel.adapter;

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
 * CDSS 触发点服务：管理院内业务触发点配置和匹配。
 * 支持医嘱、病历、检查、入径、医保结算前等触发点，
 * 兼容 API/iframe/CDS Hooks/消息 四种接入策略。
 */
@Service
public class TriggerPointService {

    private static final Logger log = LoggerFactory.getLogger(TriggerPointService.class);

    private final EnginePersistenceProperties properties;
    private final AdapterHubService adapterHubService;
    private final DataSource dataSource;

    public TriggerPointService(EnginePersistenceProperties properties,
                                AdapterHubService adapterHubService,
                                DataSource dataSource) {
        this.properties = properties;
        this.adapterHubService = adapterHubService;
        this.dataSource = dataSource;
    }

    /**
     * 注册触发点。
     */
    public CdssTriggerPoint registerTrigger(CdssTriggerPoint trigger) {
        trigger.setId(Ids.next());
        trigger.setCreatedTime(LocalDateTime.now());
        if (trigger.getEnabled() == null) trigger.setEnabled("TRUE");
        if (trigger.getTimeoutMs() == 0) trigger.setTimeoutMs(5000);
        if (trigger.getRiskLevel() == null) trigger.setRiskLevel("LOW");

        String sql = "INSERT INTO cdss_trigger_point (id, tenant_id, trigger_code, trigger_name, "
                + "trigger_type, business_scenario, access_strategy, adapter_code, endpoint_url, "
                + "rule_codes, pathway_codes, priority, risk_level, timeout_ms, enabled, "
                + "description, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trigger.getId());
            ps.setLong(2, trigger.getTenantId());
            ps.setString(3, trigger.getTriggerCode());
            ps.setString(4, trigger.getTriggerName());
            ps.setString(5, trigger.getTriggerType());
            ps.setString(6, trigger.getBusinessScenario());
            ps.setString(7, trigger.getAccessStrategy());
            ps.setString(8, trigger.getAdapterCode());
            ps.setString(9, trigger.getEndpointUrl());
            ps.setString(10, trigger.getRuleCodes());
            ps.setString(11, trigger.getPathwayCodes());
            ps.setInt(12, trigger.getPriority());
            ps.setString(13, trigger.getRiskLevel());
            ps.setInt(14, trigger.getTimeoutMs());
            ps.setString(15, trigger.getEnabled());
            ps.setString(16, trigger.getDescription());
            ps.setString(17, trigger.getCreatedBy());
            ps.setTimestamp(18, Timestamp.valueOf(trigger.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("注册触发点失败: " + ex.getMessage(), ex);
        }
        return trigger;
    }

    /**
     * 更新触发点。
     */
    public void updateTrigger(CdssTriggerPoint trigger) {
        String sql = "UPDATE cdss_trigger_point SET trigger_name = ?, trigger_type = ?, "
                + "business_scenario = ?, access_strategy = ?, adapter_code = ?, endpoint_url = ?, "
                + "rule_codes = ?, pathway_codes = ?, priority = ?, risk_level = ?, "
                + "timeout_ms = ?, enabled = ?, description = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, trigger.getTriggerName());
            ps.setString(2, trigger.getTriggerType());
            ps.setString(3, trigger.getBusinessScenario());
            ps.setString(4, trigger.getAccessStrategy());
            ps.setString(5, trigger.getAdapterCode());
            ps.setString(6, trigger.getEndpointUrl());
            ps.setString(7, trigger.getRuleCodes());
            ps.setString(8, trigger.getPathwayCodes());
            ps.setInt(9, trigger.getPriority());
            ps.setString(10, trigger.getRiskLevel());
            ps.setInt(11, trigger.getTimeoutMs());
            ps.setString(12, trigger.getEnabled());
            ps.setString(13, trigger.getDescription());
            ps.setString(14, trigger.getUpdatedBy());
            ps.setTimestamp(15, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(16, trigger.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("更新触发点失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询触发点列表。
     */
    public List<CdssTriggerPoint> listTriggers(Long tenantId, String businessScenario, String accessStrategy) {
        StringBuilder sql = new StringBuilder("SELECT * FROM cdss_trigger_point WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(tenantId));
        if (businessScenario != null && !businessScenario.isEmpty()) {
            sql.append(" AND business_scenario = ?");
            params.add(businessScenario);
        }
        if (accessStrategy != null && !accessStrategy.isEmpty()) {
            sql.append(" AND access_strategy = ?");
            params.add(accessStrategy);
        }
        sql.append(" ORDER BY priority");

        List<CdssTriggerPoint> triggers = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    triggers.add(mapTrigger(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询触发点列表失败: " + ex.getMessage(), ex);
        }
        return triggers;
    }

    /**
     * 匹配触发点：根据业务场景和事件数据匹配适用的触发点。
     */
    public List<Map<String, Object>> matchTriggers(Long tenantId, String businessScenario, Map<String, Object> eventData) {
        List<CdssTriggerPoint> triggers = listTriggers(tenantId, businessScenario, null);
        List<Map<String, Object>> matched = new ArrayList<>();

        for (CdssTriggerPoint trigger : triggers) {
            if (!"TRUE".equals(trigger.getEnabled())) continue;

            Map<String, Object> matchResult = new LinkedHashMap<String, Object>();
            matchResult.put("triggerCode", trigger.getTriggerCode());
            matchResult.put("triggerName", trigger.getTriggerName());
            matchResult.put("triggerType", trigger.getTriggerType());
            matchResult.put("accessStrategy", trigger.getAccessStrategy());
            matchResult.put("riskLevel", trigger.getRiskLevel());
            matchResult.put("timeoutMs", trigger.getTimeoutMs());

            // 根据接入策略生成调用信息
            switch (trigger.getAccessStrategy().toUpperCase()) {
                case "API":
                    matchResult.put("endpoint", trigger.getEndpointUrl());
                    matchResult.put("method", "POST");
                    break;
                case "IFRAME":
                    matchResult.put("iframeUrl", trigger.getEndpointUrl());
                    break;
                case "CDS_HOOKS":
                    matchResult.put("hook", trigger.getTriggerCode());
                    matchResult.put("hookUrl", trigger.getEndpointUrl());
                    break;
                case "MESSAGE":
                    matchResult.put("messageType", trigger.getTriggerType());
                    break;
            }

            matched.add(matchResult);
        }

        return matched;
    }

    /**
     * 执行触发点：通过适配器调用外部系统。
     */
    public Map<String, Object> executeTrigger(Long tenantId, String triggerCode, Map<String, Object> eventData) {
        CdssTriggerPoint trigger = findByCode(tenantId, triggerCode);
        if (trigger == null) {
            throw new IllegalArgumentException("触发点不存在: " + triggerCode);
        }
        if (!"TRUE".equals(trigger.getEnabled())) {
            throw new IllegalStateException("触发点已禁用: " + triggerCode);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("triggerCode", triggerCode);
        result.put("accessStrategy", trigger.getAccessStrategy());

        // 通过适配器调用
        if (trigger.getAdapterCode() != null && !trigger.getAdapterCode().isEmpty()) {
            try {
                Map<String, Object> request = new HashMap<String, Object>();
                request.put("adapter_code", trigger.getAdapterCode());
                request.put("query_code", "CDSS_TRIGGER");
                request.put("triggerCode", triggerCode);
                request.put("tenantId", String.valueOf(tenantId));
                request.putAll(eventData);

                Map<String, Object> adapterResult = adapterHubService.query(
                        request, String.valueOf(tenantId), "DEFAULT_HOSPITAL");
                result.put("status", "SUCCESS");
                result.put("data", adapterResult);
            } catch (Exception ex) {
                result.put("status", "ERROR");
                result.put("error", ex.getMessage());
            }
        } else {
            result.put("status", "NO_ADAPTER");
            result.put("message", "触发点未配置适配器");
        }

        return result;
    }

    private CdssTriggerPoint findByCode(Long tenantId, String triggerCode) {
        String sql = "SELECT * FROM cdss_trigger_point WHERE tenant_id = ? AND trigger_code = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, triggerCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTrigger(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询触发点失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private CdssTriggerPoint mapTrigger(ResultSet rs) throws SQLException {
        CdssTriggerPoint trigger = new CdssTriggerPoint();
        trigger.setId(rs.getLong("id"));
        trigger.setTenantId(rs.getLong("tenant_id"));
        trigger.setTriggerCode(rs.getString("trigger_code"));
        trigger.setTriggerName(rs.getString("trigger_name"));
        trigger.setTriggerType(rs.getString("trigger_type"));
        trigger.setBusinessScenario(rs.getString("business_scenario"));
        trigger.setAccessStrategy(rs.getString("access_strategy"));
        trigger.setAdapterCode(rs.getString("adapter_code"));
        trigger.setEndpointUrl(rs.getString("endpoint_url"));
        trigger.setRuleCodes(rs.getString("rule_codes"));
        trigger.setPathwayCodes(rs.getString("pathway_codes"));
        trigger.setPriority(rs.getInt("priority"));
        trigger.setRiskLevel(rs.getString("risk_level"));
        trigger.setTimeoutMs(rs.getInt("timeout_ms"));
        trigger.setEnabled(rs.getString("enabled"));
        trigger.setDescription(rs.getString("description"));
        trigger.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) trigger.setCreatedTime(createdTime.toLocalDateTime());
        trigger.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) trigger.setUpdatedTime(updatedTime.toLocalDateTime());
        return trigger;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
        return dataSource.getConnection();
    }
}
