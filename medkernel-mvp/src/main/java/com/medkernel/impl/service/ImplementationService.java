package com.medkernel.impl.service;

import com.medkernel.common.TraceContext;
import com.medkernel.impl.entity.*;
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
 * 实施服务：客户实施、培训和试运行管理
 */
@Service
public class ImplementationService {

    private static final Logger log = LoggerFactory.getLogger(ImplementationService.class);

    private final DataSource dataSource;

    public ImplementationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== 清单模板管理 ====================

    public OnboardingChecklistTemplate createTemplate(OnboardingChecklistTemplate template) {
        String sql = "INSERT INTO impl_checklist_template (id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            template.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, template.getTenantId());
            ps.setString(3, template.getTemplateCode());
            ps.setString(4, template.getTemplateName());
            ps.setString(5, template.getDescription());
            ps.setString(6, template.getPhase());
            ps.setString(7, template.getCategory());
            ps.setBoolean(8, template.getIsActive() != null ? template.getIsActive() : true);
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created checklist template: id={}, code={}", id, template.getTemplateCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create checklist template failed: " + ex.getMessage(), ex);
        }
        return template;
    }

    public OnboardingChecklistTemplate updateTemplate(OnboardingChecklistTemplate template) {
        String sql = "UPDATE impl_checklist_template SET template_name = ?, description = ?, "
                + "phase = ?, category = ?, is_active = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, template.getTemplateName());
            ps.setString(2, template.getDescription());
            ps.setString(3, template.getPhase());
            ps.setString(4, template.getCategory());
            ps.setBoolean(5, template.getIsActive() != null ? template.getIsActive() : true);
            ps.setString(6, TraceContext.getUsername());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(8, template.getId());
            ps.setLong(9, template.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update checklist template failed: " + ex.getMessage(), ex);
        }
        return template;
    }

    public List<OnboardingChecklistTemplate> listTemplates(Long tenantId, String phase, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_template WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        if (phase != null && !phase.isEmpty()) {
            sql.append(" AND phase = ?");
            params.add(phase);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }
        sql.append(" ORDER BY created_time DESC");

        List<OnboardingChecklistTemplate> templates = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 2, (String) param);
                } else if (param instanceof Boolean) {
                    ps.setBoolean(i + 2, (Boolean) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    templates.add(mapTemplate(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list checklist templates failed: " + ex.getMessage(), ex);
        }
        return templates;
    }

    public OnboardingChecklistTemplate getTemplate(Long templateId) {
        String sql = "SELECT id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_template WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTemplate(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get checklist template failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 清单检查项管理 ====================

    public OnboardingChecklistItem addChecklistItem(OnboardingChecklistItem item) {
        String sql = "INSERT INTO impl_checklist_item (id, tenant_id, template_id, item_code, item_name, "
                + "description, sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            item.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, item.getTenantId());
            ps.setLong(3, item.getTemplateId());
            ps.setString(4, item.getItemCode());
            ps.setString(5, item.getItemName());
            ps.setString(6, item.getDescription());
            ps.setInt(7, item.getSortOrder() != null ? item.getSortOrder() : 0);
            ps.setBoolean(8, item.getIsRequired() != null ? item.getIsRequired() : true);
            ps.setString(9, item.getCheckMethod());
            ps.setString(10, item.getExpectedResult());
            ps.setString(11, item.getStatus() != null ? item.getStatus() : "PENDING");
            ps.setString(12, item.getCheckedBy());
            if (item.getCheckedTime() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(item.getCheckedTime()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            ps.setString(14, item.getRemark());
            ps.setString(15, TraceContext.getUsername());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Added checklist item: id={}, code={}", id, item.getItemCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("add checklist item failed: " + ex.getMessage(), ex);
        }
        return item;
    }

    public OnboardingChecklistItem updateChecklistItem(OnboardingChecklistItem item) {
        String sql = "UPDATE impl_checklist_item SET item_name = ?, description = ?, "
                + "sort_order = ?, is_required = ?, check_method = ?, expected_result = ?, "
                + "remark = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getDescription());
            ps.setInt(3, item.getSortOrder() != null ? item.getSortOrder() : 0);
            ps.setBoolean(4, item.getIsRequired() != null ? item.getIsRequired() : true);
            ps.setString(5, item.getCheckMethod());
            ps.setString(6, item.getExpectedResult());
            ps.setString(7, item.getRemark());
            ps.setString(8, TraceContext.getUsername());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(10, item.getId());
            ps.setLong(11, item.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update checklist item failed: " + ex.getMessage(), ex);
        }
        return item;
    }

    public OnboardingChecklistItem checkItem(Long itemId, Long tenantId, boolean checked) {
        String sql = "UPDATE impl_checklist_item SET status = ?, checked_by = ?, checked_time = ?, "
                + "updated_by = ?, updated_time = ? WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String status = checked ? "CHECKED" : "PENDING";
            ps.setString(1, status);
            ps.setString(2, checked ? TraceContext.getUsername() : null);
            ps.setTimestamp(3, checked ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(4, TraceContext.getUsername());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, itemId);
            ps.setLong(7, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("check checklist item failed: " + ex.getMessage(), ex);
        }
        return getChecklistItem(itemId);
    }

    public List<OnboardingChecklistItem> listChecklistItems(Long tenantId, Long templateId) {
        String sql = "SELECT id, tenant_id, template_id, item_code, item_name, description, "
                + "sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_item WHERE tenant_id = ? AND template_id = ? "
                + "ORDER BY sort_order, created_time";
        List<OnboardingChecklistItem> items = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapChecklistItem(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list checklist items failed: " + ex.getMessage(), ex);
        }
        return items;
    }

    public OnboardingChecklistItem getChecklistItem(Long itemId) {
        String sql = "SELECT id, tenant_id, template_id, item_code, item_name, description, "
                + "sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_item WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapChecklistItem(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get checklist item failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 培训材料管理 ====================

    public TrainingMaterial createMaterial(TrainingMaterial material) {
        String sql = "INSERT INTO impl_training_material (id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            material.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, material.getTenantId());
            ps.setString(3, material.getMaterialCode());
            ps.setString(4, material.getMaterialName());
            ps.setString(5, material.getDescription());
            ps.setString(6, material.getMaterialType());
            ps.setString(7, material.getCategory());
            ps.setString(8, material.getContentPath());
            ps.setString(9, material.getVersion());
            if (material.getDurationMinutes() != null) {
                ps.setInt(10, material.getDurationMinutes());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.setString(11, material.getTargetAudience());
            ps.setBoolean(12, material.getIsPublished() != null ? material.getIsPublished() : false);
            ps.setString(13, material.getPublishedBy());
            if (material.getPublishedTime() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(material.getPublishedTime()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.setString(15, TraceContext.getUsername());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created training material: id={}, code={}", id, material.getMaterialCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create training material failed: " + ex.getMessage(), ex);
        }
        return material;
    }

    public TrainingMaterial updateMaterial(TrainingMaterial material) {
        String sql = "UPDATE impl_training_material SET material_name = ?, description = ?, "
                + "material_type = ?, category = ?, content_path = ?, version = ?, "
                + "duration_minutes = ?, target_audience = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, material.getMaterialName());
            ps.setString(2, material.getDescription());
            ps.setString(3, material.getMaterialType());
            ps.setString(4, material.getCategory());
            ps.setString(5, material.getContentPath());
            ps.setString(6, material.getVersion());
            if (material.getDurationMinutes() != null) {
                ps.setInt(7, material.getDurationMinutes());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, material.getTargetAudience());
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, material.getId());
            ps.setLong(12, material.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update training material failed: " + ex.getMessage(), ex);
        }
        return material;
    }

    public List<TrainingMaterial> listMaterials(Long tenantId, String category, Boolean isPublished) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_training_material WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (isPublished != null) {
            sql.append(" AND is_published = ?");
            params.add(isPublished);
        }
        sql.append(" ORDER BY created_time DESC");

        List<TrainingMaterial> materials = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 2, (String) param);
                } else if (param instanceof Boolean) {
                    ps.setBoolean(i + 2, (Boolean) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    materials.add(mapMaterial(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list training materials failed: " + ex.getMessage(), ex);
        }
        return materials;
    }

    public TrainingMaterial getMaterial(Long materialId) {
        String sql = "SELECT id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_training_material WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, materialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMaterial(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get training material failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    public TrainingMaterial publishMaterial(Long materialId, Long tenantId) {
        String sql = "UPDATE impl_training_material SET is_published = TRUE, "
                + "published_by = ?, published_time = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String username = TraceContext.getUsername();
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, username);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, username);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setLong(5, materialId);
            ps.setLong(6, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("publish training material failed: " + ex.getMessage(), ex);
        }
        return getMaterial(materialId);
    }

    // ==================== 演示数据包管理 ====================

    public DemoDataPackage createDemoDataPackage(DemoDataPackage pkg) {
        String sql = "INSERT INTO impl_demo_data_package (id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            pkg.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, pkg.getTenantId());
            ps.setString(3, pkg.getPackageCode());
            ps.setString(4, pkg.getPackageName());
            ps.setString(5, pkg.getDescription());
            ps.setString(6, pkg.getDataScope());
            ps.setString(7, pkg.getDataVersion());
            ps.setLong(8, pkg.getRecordCount() != null ? pkg.getRecordCount() : 0L);
            ps.setString(9, pkg.getArtifactPath());
            ps.setString(10, pkg.getArtifactHash());
            ps.setString(11, pkg.getStatus() != null ? pkg.getStatus() : "DRAFT");
            ps.setString(12, TraceContext.getUsername());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created demo data package: id={}, code={}", id, pkg.getPackageCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create demo data package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    public DemoDataPackage updateDemoDataPackage(DemoDataPackage pkg) {
        String sql = "UPDATE impl_demo_data_package SET package_name = ?, description = ?, "
                + "data_scope = ?, data_version = ?, record_count = ?, artifact_path = ?, "
                + "artifact_hash = ?, status = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pkg.getPackageName());
            ps.setString(2, pkg.getDescription());
            ps.setString(3, pkg.getDataScope());
            ps.setString(4, pkg.getDataVersion());
            ps.setLong(5, pkg.getRecordCount() != null ? pkg.getRecordCount() : 0L);
            ps.setString(6, pkg.getArtifactPath());
            ps.setString(7, pkg.getArtifactHash());
            ps.setString(8, pkg.getStatus());
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, pkg.getId());
            ps.setLong(12, pkg.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update demo data package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    public List<DemoDataPackage> listDemoDataPackages(Long tenantId, String status) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time, updated_by, updated_time "
                + "FROM impl_demo_data_package WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<DemoDataPackage> packages = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 2, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    packages.add(mapDemoDataPackage(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list demo data packages failed: " + ex.getMessage(), ex);
        }
        return packages;
    }

    public DemoDataPackage getDemoDataPackage(Long packageId) {
        String sql = "SELECT id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time, updated_by, updated_time "
                + "FROM impl_demo_data_package WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDemoDataPackage(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get demo data package failed: " + ex.getMessage(), ex);
        }
        return null;
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

    private OnboardingChecklistTemplate mapTemplate(ResultSet rs) throws SQLException {
        OnboardingChecklistTemplate t = new OnboardingChecklistTemplate();
        t.setId(rs.getLong("id"));
        t.setTenantId(rs.getLong("tenant_id"));
        t.setTemplateCode(rs.getString("template_code"));
        t.setTemplateName(rs.getString("template_name"));
        t.setDescription(rs.getString("description"));
        t.setPhase(rs.getString("phase"));
        t.setCategory(rs.getString("category"));
        t.setIsActive(rs.getBoolean("is_active"));
        t.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            t.setCreatedTime(createdTime.toLocalDateTime());
        }
        t.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            t.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return t;
    }

    private OnboardingChecklistItem mapChecklistItem(ResultSet rs) throws SQLException {
        OnboardingChecklistItem item = new OnboardingChecklistItem();
        item.setId(rs.getLong("id"));
        item.setTenantId(rs.getLong("tenant_id"));
        item.setTemplateId(rs.getLong("template_id"));
        item.setItemCode(rs.getString("item_code"));
        item.setItemName(rs.getString("item_name"));
        item.setDescription(rs.getString("description"));
        item.setSortOrder(rs.getInt("sort_order"));
        item.setIsRequired(rs.getBoolean("is_required"));
        item.setCheckMethod(rs.getString("check_method"));
        item.setExpectedResult(rs.getString("expected_result"));
        item.setStatus(rs.getString("status"));
        item.setCheckedBy(rs.getString("checked_by"));
        Timestamp checkedTime = rs.getTimestamp("checked_time");
        if (checkedTime != null) {
            item.setCheckedTime(checkedTime.toLocalDateTime());
        }
        item.setRemark(rs.getString("remark"));
        item.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            item.setCreatedTime(createdTime.toLocalDateTime());
        }
        item.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            item.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return item;
    }

    private TrainingMaterial mapMaterial(ResultSet rs) throws SQLException {
        TrainingMaterial m = new TrainingMaterial();
        m.setId(rs.getLong("id"));
        m.setTenantId(rs.getLong("tenant_id"));
        m.setMaterialCode(rs.getString("material_code"));
        m.setMaterialName(rs.getString("material_name"));
        m.setDescription(rs.getString("description"));
        m.setMaterialType(rs.getString("material_type"));
        m.setCategory(rs.getString("category"));
        m.setContentPath(rs.getString("content_path"));
        m.setVersion(rs.getString("version"));
        m.setDurationMinutes(rs.getInt("duration_minutes"));
        m.setTargetAudience(rs.getString("target_audience"));
        m.setIsPublished(rs.getBoolean("is_published"));
        m.setPublishedBy(rs.getString("published_by"));
        Timestamp publishedTime = rs.getTimestamp("published_time");
        if (publishedTime != null) {
            m.setPublishedTime(publishedTime.toLocalDateTime());
        }
        m.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            m.setCreatedTime(createdTime.toLocalDateTime());
        }
        m.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            m.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return m;
    }

    private DemoDataPackage mapDemoDataPackage(ResultSet rs) throws SQLException {
        DemoDataPackage p = new DemoDataPackage();
        p.setId(rs.getLong("id"));
        p.setTenantId(rs.getLong("tenant_id"));
        p.setPackageCode(rs.getString("package_code"));
        p.setPackageName(rs.getString("package_name"));
        p.setDescription(rs.getString("description"));
        p.setDataScope(rs.getString("data_scope"));
        p.setDataVersion(rs.getString("data_version"));
        p.setRecordCount(rs.getLong("record_count"));
        p.setArtifactPath(rs.getString("artifact_path"));
        p.setArtifactHash(rs.getString("artifact_hash"));
        p.setStatus(rs.getString("status"));
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
