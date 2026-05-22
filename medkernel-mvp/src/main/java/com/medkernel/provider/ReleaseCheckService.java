package com.medkernel.provider;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

@Service
public class ReleaseCheckService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseCheckService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public ReleaseCheckService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // =========================================================================
    // 清单管理
    // =========================================================================

    public ReleaseChecklist createChecklist(ReleaseChecklist checklist) {
        checklist.setId(Ids.next());
        if (checklist.getEnabled() == null) {
            checklist.setEnabled("Y");
        }
        if (checklist.getApprovalRequired() == null) {
            checklist.setApprovalRequired("FALSE");
        }
        checklist.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO prov_release_checklist (id, tenant_id, checklist_code, checklist_name, "
                + "resource_type, description, check_items, blocking_rules, approval_required, approval_roles, "
                + "enabled, created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, checklist.getId());
            ps.setLong(i++, checklist.getTenantId());
            ps.setString(i++, checklist.getChecklistCode());
            ps.setString(i++, checklist.getChecklistName());
            ps.setString(i++, checklist.getResourceType());
            ps.setString(i++, checklist.getDescription());
            ps.setString(i++, checklist.getCheckItems());
            ps.setString(i++, checklist.getBlockingRules());
            ps.setString(i++, checklist.getApprovalRequired());
            ps.setString(i++, checklist.getApprovalRoles());
            ps.setString(i++, checklist.getEnabled());
            ps.setString(i++, checklist.getCreatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(checklist.getCreatedTime()));
            ps.setString(i++, checklist.getUpdatedBy());
            ps.setTimestamp(i++, checklist.getUpdatedTime() != null ? Timestamp.valueOf(checklist.getUpdatedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建检查清单失败: " + ex.getMessage(), ex);
        }
        return checklist;
    }

    public ReleaseChecklist updateChecklist(ReleaseChecklist checklist) {
        String sql = "UPDATE prov_release_checklist SET checklist_name=?, resource_type=?, description=?, "
                + "check_items=?, blocking_rules=?, approval_required=?, approval_roles=?, enabled=?, "
                + "updated_by=?, updated_time=? WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, checklist.getChecklistName());
            ps.setString(i++, checklist.getResourceType());
            ps.setString(i++, checklist.getDescription());
            ps.setString(i++, checklist.getCheckItems());
            ps.setString(i++, checklist.getBlockingRules());
            ps.setString(i++, checklist.getApprovalRequired());
            ps.setString(i++, checklist.getApprovalRoles());
            ps.setString(i++, checklist.getEnabled());
            ps.setString(i++, checklist.getUpdatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, checklist.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("更新检查清单失败: " + ex.getMessage(), ex);
        }
        return checklist;
    }

    public List<ReleaseChecklist> listChecklists(Long tenantId, String resourceType, String enabled) {
        StringBuilder sql = new StringBuilder("SELECT * FROM prov_release_checklist WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (resourceType != null && !resourceType.isEmpty()) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (enabled != null && !enabled.isEmpty()) {
            sql.append(" AND enabled = ?");
            params.add(enabled);
        }
        sql.append(" ORDER BY created_time DESC");

        List<ReleaseChecklist> checklists = new ArrayList<ReleaseChecklist>();
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
                    checklists.add(mapChecklist(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询检查清单失败: " + ex.getMessage(), ex);
        }
        return checklists;
    }

    // =========================================================================
    // 检查执行
    // =========================================================================

    public ReleaseCheckResult executeCheck(Long tenantId, String resourceType, String resourceCode,
                                           String resourceVersion, String checkedBy) {
        List<ReleaseChecklist> checklists = listChecklists(tenantId, resourceType, "Y");
        if (checklists.isEmpty()) {
            ReleaseCheckResult result = new ReleaseCheckResult();
            result.setId(Ids.next());
            result.setTenantId(tenantId);
            result.setCheckCode("RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            result.setResourceType(resourceType);
            result.setResourceCode(resourceCode);
            result.setResourceVersion(resourceVersion);
            result.setCheckStatus("PASSED");
            result.setTotalItems(0);
            result.setPassedItems(0);
            result.setFailedItems(0);
            result.setBlockedItems(0);
            result.setCheckedBy(checkedBy);
            result.setCheckedTime(LocalDateTime.now());
            result.setCreatedTime(LocalDateTime.now());
            return result;
        }

        ReleaseChecklist checklist = checklists.get(0);

        int totalItems = 0;
        int passedItems = 0;
        int failedItems = 0;
        int blockedItems = 0;
        StringBuilder detailBuilder = new StringBuilder();
        detailBuilder.append("{\"items\":[");

        String checkItems = checklist.getCheckItems();
        if (checkItems != null && !checkItems.isEmpty()) {
            String[] items = checkItems.split(",");
            for (int idx = 0; idx < items.length; idx++) {
                String item = items[idx].trim();
                if (item.isEmpty()) {
                    continue;
                }
                totalItems++;
                boolean passed = evaluateCheckItem(item, resourceType, resourceCode, resourceVersion);
                if (passed) {
                    passedItems++;
                } else {
                    failedItems++;
                }
                if (idx > 0 && detailBuilder.length() > "{\"items\":[".length()) {
                    detailBuilder.append(",");
                }
                detailBuilder.append("{\"item\":\"").append(item).append("\",\"passed\":").append(passed).append("}");
            }
        }
        detailBuilder.append("]}");

        String blockingRules = checklist.getBlockingRules();
        String blockedReason = null;
        if (blockingRules != null && !blockingRules.isEmpty() && failedItems > 0) {
            blockedItems = failedItems;
            blockedReason = "检查项未通过: " + failedItems + "项失败";
        }

        String checkStatus;
        if (blockedItems > 0) {
            checkStatus = "BLOCKED";
        } else if (failedItems > 0) {
            checkStatus = "FAILED";
        } else {
            checkStatus = "PASSED";
        }

        ReleaseCheckResult result = new ReleaseCheckResult();
        result.setId(Ids.next());
        result.setTenantId(tenantId);
        result.setCheckCode("RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.setChecklistCode(checklist.getChecklistCode());
        result.setChecklistName(checklist.getChecklistName());
        result.setResourceType(resourceType);
        result.setResourceCode(resourceCode);
        result.setResourceVersion(resourceVersion);
        result.setCheckStatus(checkStatus);
        result.setCheckDetail(detailBuilder.toString());
        result.setTotalItems(totalItems);
        result.setPassedItems(passedItems);
        result.setFailedItems(failedItems);
        result.setBlockedItems(blockedItems);
        result.setBlockedReason(blockedReason);
        result.setCheckedBy(checkedBy);
        result.setCheckedTime(LocalDateTime.now());
        result.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO prov_release_check_result (id, tenant_id, check_code, checklist_code, "
                + "checklist_name, resource_type, resource_code, resource_version, check_status, check_detail, "
                + "total_items, passed_items, failed_items, blocked_items, blocked_reason, approved_by, "
                + "approved_time, approval_note, waived_by, waive_reason, checked_by, checked_time, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, result.getId());
            ps.setLong(i++, result.getTenantId());
            ps.setString(i++, result.getCheckCode());
            ps.setString(i++, result.getChecklistCode());
            ps.setString(i++, result.getChecklistName());
            ps.setString(i++, result.getResourceType());
            ps.setString(i++, result.getResourceCode());
            ps.setString(i++, result.getResourceVersion());
            ps.setString(i++, result.getCheckStatus());
            ps.setString(i++, result.getCheckDetail());
            ps.setInt(i++, result.getTotalItems());
            ps.setInt(i++, result.getPassedItems());
            ps.setInt(i++, result.getFailedItems());
            ps.setInt(i++, result.getBlockedItems());
            ps.setString(i++, result.getBlockedReason());
            ps.setString(i++, result.getApprovedBy());
            ps.setTimestamp(i++, result.getApprovedTime() != null ? Timestamp.valueOf(result.getApprovedTime()) : null);
            ps.setString(i++, result.getApprovalNote());
            ps.setString(i++, result.getWaivedBy());
            ps.setString(i++, result.getWaiveReason());
            ps.setString(i++, result.getCheckedBy());
            ps.setTimestamp(i++, result.getCheckedTime() != null ? Timestamp.valueOf(result.getCheckedTime()) : null);
            ps.setTimestamp(i++, Timestamp.valueOf(result.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("保存检查结果失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    public List<ReleaseCheckResult> listCheckResults(Long tenantId, String resourceType, String checkStatus) {
        StringBuilder sql = new StringBuilder("SELECT * FROM prov_release_check_result WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (resourceType != null && !resourceType.isEmpty()) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (checkStatus != null && !checkStatus.isEmpty()) {
            sql.append(" AND check_status = ?");
            params.add(checkStatus);
        }
        sql.append(" ORDER BY created_time DESC");

        List<ReleaseCheckResult> results = new ArrayList<ReleaseCheckResult>();
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
                    results.add(mapCheckResult(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询检查结果失败: " + ex.getMessage(), ex);
        }
        return results;
    }

    public ReleaseCheckResult getCheckResult(Long checkResultId) {
        String sql = "SELECT * FROM prov_release_check_result WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, checkResultId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCheckResult(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询检查结果失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    // =========================================================================
    // 审批管理
    // =========================================================================

    public void approveRelease(Long checkResultId, String approvedBy, String approvalNote) {
        ReleaseCheckResult result = getCheckResult(checkResultId);
        if (result == null) {
            throw new IllegalStateException("检查结果不存在: " + checkResultId);
        }
        String sql = "UPDATE prov_release_check_result SET check_status = 'PASSED', approved_by = ?, "
                + "approved_time = ?, approval_note = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, approvedBy);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, approvalNote);
            ps.setLong(4, checkResultId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审批通过失败: " + ex.getMessage(), ex);
        }
    }

    public void waiveBlock(Long checkResultId, String waivedBy, String waiveReason) {
        ReleaseCheckResult result = getCheckResult(checkResultId);
        if (result == null) {
            throw new IllegalStateException("检查结果不存在: " + checkResultId);
        }
        String sql = "UPDATE prov_release_check_result SET check_status = 'WAIVED', waived_by = ?, "
                + "waive_reason = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, waivedBy);
            ps.setString(2, waiveReason);
            ps.setLong(3, checkResultId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("豁免阻断失败: " + ex.getMessage(), ex);
        }
    }

    public void rejectRelease(Long checkResultId, String approvedBy, String approvalNote) {
        ReleaseCheckResult result = getCheckResult(checkResultId);
        if (result == null) {
            throw new IllegalStateException("检查结果不存在: " + checkResultId);
        }
        String sql = "UPDATE prov_release_check_result SET check_status = 'FAILED', approved_by = ?, "
                + "approved_time = ?, approval_note = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, approvedBy);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, approvalNote);
            ps.setLong(4, checkResultId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("驳回发布失败: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private boolean evaluateCheckItem(String item, String resourceType, String resourceCode, String resourceVersion) {
        return true;
    }

    private ReleaseChecklist mapChecklist(ResultSet rs) throws SQLException {
        ReleaseChecklist checklist = new ReleaseChecklist();
        checklist.setId(rs.getLong("id"));
        checklist.setTenantId(rs.getLong("tenant_id"));
        checklist.setChecklistCode(rs.getString("checklist_code"));
        checklist.setChecklistName(rs.getString("checklist_name"));
        checklist.setResourceType(rs.getString("resource_type"));
        checklist.setDescription(rs.getString("description"));
        checklist.setCheckItems(rs.getString("check_items"));
        checklist.setBlockingRules(rs.getString("blocking_rules"));
        checklist.setApprovalRequired(rs.getString("approval_required"));
        checklist.setApprovalRoles(rs.getString("approval_roles"));
        checklist.setEnabled(rs.getString("enabled"));
        checklist.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            checklist.setCreatedTime(createdTime.toLocalDateTime());
        }
        checklist.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            checklist.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return checklist;
    }

    private ReleaseCheckResult mapCheckResult(ResultSet rs) throws SQLException {
        ReleaseCheckResult result = new ReleaseCheckResult();
        result.setId(rs.getLong("id"));
        result.setTenantId(rs.getLong("tenant_id"));
        result.setCheckCode(rs.getString("check_code"));
        result.setChecklistCode(rs.getString("checklist_code"));
        result.setChecklistName(rs.getString("checklist_name"));
        result.setResourceType(rs.getString("resource_type"));
        result.setResourceCode(rs.getString("resource_code"));
        result.setResourceVersion(rs.getString("resource_version"));
        result.setCheckStatus(rs.getString("check_status"));
        result.setCheckDetail(rs.getString("check_detail"));
        result.setTotalItems(rs.getInt("total_items"));
        result.setPassedItems(rs.getInt("passed_items"));
        result.setFailedItems(rs.getInt("failed_items"));
        result.setBlockedItems(rs.getInt("blocked_items"));
        result.setBlockedReason(rs.getString("blocked_reason"));
        result.setApprovedBy(rs.getString("approved_by"));
        Timestamp approvedTime = rs.getTimestamp("approved_time");
        if (approvedTime != null) {
            result.setApprovedTime(approvedTime.toLocalDateTime());
        }
        result.setApprovalNote(rs.getString("approval_note"));
        result.setWaivedBy(rs.getString("waived_by"));
        result.setWaiveReason(rs.getString("waive_reason"));
        result.setCheckedBy(rs.getString("checked_by"));
        Timestamp checkedTime = rs.getTimestamp("checked_time");
        if (checkedTime != null) {
            result.setCheckedTime(checkedTime.toLocalDateTime());
        }
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            result.setCreatedTime(createdTime.toLocalDateTime());
        }
        return result;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
