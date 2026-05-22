package com.medkernel.security;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
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
 * SEC-008 统一权限服务：菜单权限管理 + 统一权限检查。
 * 使用 raw JDBC 操作 sec_menu_permission 表，关联 sec_user_role / sec_role_permission 实现权限检查。
 */
@Service
public class UnifiedPermissionService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedPermissionService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public UnifiedPermissionService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // ============================================================
    // 菜单权限管理
    // ============================================================

    /**
     * 创建菜单权限。
     */
    public MenuPermission createMenuPermission(MenuPermission menu) {
        if (menu.getId() == null) {
            menu.setId(Ids.next());
        }
        if (menu.getVisible() == null) {
            menu.setVisible("TRUE");
        }
        if (menu.getEnabled() == null) {
            menu.setEnabled("Y");
        }
        menu.setCreatedTime(LocalDateTime.now());
        menu.setUpdatedTime(LocalDateTime.now());

        String sql = "INSERT INTO sec_menu_permission "
                + "(id, tenant_id, menu_code, menu_name, menu_path, menu_icon, parent_code, "
                + "sort_order, menu_type, permission_code, permission_name, permission_type, "
                + "data_permission_code, visible, enabled, created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, menu.getId());
            ps.setLong(i++, menu.getTenantId());
            ps.setString(i++, menu.getMenuCode());
            ps.setString(i++, menu.getMenuName());
            ps.setString(i++, menu.getMenuPath());
            ps.setString(i++, menu.getMenuIcon());
            ps.setString(i++, menu.getParentCode());
            ps.setInt(i++, menu.getSortOrder());
            ps.setString(i++, menu.getMenuType());
            ps.setString(i++, menu.getPermissionCode());
            ps.setString(i++, menu.getPermissionName());
            ps.setString(i++, menu.getPermissionType());
            ps.setString(i++, menu.getDataPermissionCode());
            ps.setString(i++, menu.getVisible());
            ps.setString(i++, menu.getEnabled());
            ps.setString(i++, menu.getCreatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(menu.getCreatedTime()));
            ps.setString(i++, menu.getUpdatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(menu.getUpdatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create menu permission failed: " + ex.getMessage(), ex);
        }
        return menu;
    }

    /**
     * 更新菜单权限。
     */
    public MenuPermission updateMenuPermission(MenuPermission menu) {
        String sql = "UPDATE sec_menu_permission SET "
                + "menu_name = ?, menu_path = ?, menu_icon = ?, parent_code = ?, "
                + "sort_order = ?, menu_type = ?, permission_code = ?, permission_name = ?, "
                + "permission_type = ?, data_permission_code = ?, visible = ?, enabled = ?, "
                + "updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";

        menu.setUpdatedTime(LocalDateTime.now());

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, menu.getMenuName());
            ps.setString(i++, menu.getMenuPath());
            ps.setString(i++, menu.getMenuIcon());
            ps.setString(i++, menu.getParentCode());
            ps.setInt(i++, menu.getSortOrder());
            ps.setString(i++, menu.getMenuType());
            ps.setString(i++, menu.getPermissionCode());
            ps.setString(i++, menu.getPermissionName());
            ps.setString(i++, menu.getPermissionType());
            ps.setString(i++, menu.getDataPermissionCode());
            ps.setString(i++, menu.getVisible());
            ps.setString(i++, menu.getEnabled());
            ps.setString(i++, menu.getUpdatedBy());
            ps.setTimestamp(i++, Timestamp.valueOf(menu.getUpdatedTime()));
            ps.setLong(i++, menu.getId());
            ps.setLong(i++, menu.getTenantId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("menu permission not found: id=" + menu.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update menu permission failed: " + ex.getMessage(), ex);
        }
        return menu;
    }

    /**
     * 查询菜单权限列表。
     */
    public List<MenuPermission> listMenuPermissions(Long tenantId, String menuType, String parentCode) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, menu_code, menu_name, menu_path, menu_icon, parent_code, "
                        + "sort_order, menu_type, permission_code, permission_name, permission_type, "
                        + "data_permission_code, visible, enabled, created_by, created_time, updated_by, updated_time "
                        + "FROM sec_menu_permission WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);

        if (menuType != null && !menuType.isEmpty()) {
            sql.append(" AND menu_type = ?");
            params.add(menuType);
        }
        if (parentCode != null && !parentCode.isEmpty()) {
            sql.append(" AND parent_code = ?");
            params.add(parentCode);
        }
        sql.append(" ORDER BY sort_order, menu_code");

        List<MenuPermission> result = new ArrayList<MenuPermission>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapMenuPermission(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list menu permissions failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 获取菜单树（全量，不按用户过滤）。
     */
    public List<Map<String, Object>> getMenuTree(Long tenantId) {
        List<MenuPermission> all = listMenuPermissions(tenantId, null, null);
        return buildTree(all, null);
    }

    // ============================================================
    // 统一权限检查
    // ============================================================

    /**
     * 检查菜单访问权限：用户是否拥有指定 menuCode 对应的权限。
     */
    public boolean checkMenuAccess(Long tenantId, String userId, String menuCode) {
        // 1. 查找菜单权限记录
        MenuPermission menuPerm = findMenuPermissionByCode(tenantId, menuCode);
        if (menuPerm == null) {
            return false;
        }
        if (!"Y".equals(menuPerm.getEnabled())) {
            return false;
        }
        // 2. 查找用户角色
        List<Long> roleIds = findUserRoleIds(tenantId, userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        // 3. 查找角色是否拥有该菜单权限
        String permissionCode = menuPerm.getPermissionCode();
        if (permissionCode == null || permissionCode.isEmpty()) {
            // 无权限编码的菜单默认可见
            return true;
        }
        return checkRolePermission(tenantId, roleIds, permissionCode);
    }

    /**
     * 检查按钮权限：用户是否拥有指定 permissionCode 对应的按钮权限。
     */
    public boolean checkButtonAccess(Long tenantId, String userId, String permissionCode) {
        List<Long> roleIds = findUserRoleIds(tenantId, userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        return checkRolePermission(tenantId, roleIds, permissionCode);
    }

    /**
     * 获取用户所有权限编码。
     */
    public List<String> getUserPermissions(Long tenantId, String userId) {
        List<Long> roleIds = findUserRoleIds(tenantId, userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<String>();
        }
        return findRolePermissionCodes(tenantId, roleIds);
    }

    /**
     * 获取用户可见菜单树。
     */
    public List<Map<String, Object>> getUserMenuTree(Long tenantId, String userId) {
        List<MenuPermission> all = listMenuPermissions(tenantId, null, null);
        List<String> userPerms = getUserPermissions(tenantId, userId);

        // 过滤：仅保留用户有权限且可见的菜单
        List<MenuPermission> filtered = new ArrayList<MenuPermission>();
        for (MenuPermission mp : all) {
            if (!"Y".equals(mp.getEnabled())) {
                continue;
            }
            if (!"TRUE".equals(mp.getVisible())) {
                continue;
            }
            // DIRECTORY 类型始终可见（作为容器）
            if ("DIRECTORY".equals(mp.getMenuType())) {
                filtered.add(mp);
                continue;
            }
            // MENU/BUTTON 需要检查权限
            String permCode = mp.getPermissionCode();
            if (permCode == null || permCode.isEmpty() || userPerms.contains(permCode)) {
                filtered.add(mp);
            }
        }

        // 递归清除空目录（没有子菜单的目录不应显示）
        filtered = pruneEmptyDirectories(filtered);

        return buildTree(filtered, null);
    }

    /**
     * 获取用户在某菜单下的按钮权限。
     */
    public List<Map<String, Object>> getUserButtonPermissions(Long tenantId, String userId, String menuCode) {
        List<String> userPerms = getUserPermissions(tenantId, userId);

        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, menu_code, menu_name, menu_path, menu_icon, parent_code, "
                        + "sort_order, menu_type, permission_code, permission_name, permission_type, "
                        + "data_permission_code, visible, enabled, created_by, created_time, updated_by, updated_time "
                        + "FROM sec_menu_permission WHERE tenant_id = ? AND menu_type = 'BUTTON' AND enabled = 'Y'");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);

        if (menuCode != null && !menuCode.isEmpty()) {
            sql.append(" AND parent_code = ?");
            params.add(menuCode);
        }
        sql.append(" ORDER BY sort_order, menu_code");

        List<Map<String, Object>> buttons = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuPermission mp = mapMenuPermission(rs);
                    String permCode = mp.getPermissionCode();
                    boolean hasPermission = permCode == null || permCode.isEmpty() || userPerms.contains(permCode);
                    Map<String, Object> btn = new LinkedHashMap<String, Object>();
                    btn.put("menu_code", mp.getMenuCode());
                    btn.put("menu_name", mp.getMenuName());
                    btn.put("permission_code", mp.getPermissionCode());
                    btn.put("permission_name", mp.getPermissionName());
                    btn.put("data_permission_code", mp.getDataPermissionCode());
                    btn.put("has_permission", hasPermission);
                    buttons.add(btn);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get user button permissions failed: " + ex.getMessage(), ex);
        }
        return buttons;
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private MenuPermission findMenuPermissionByCode(Long tenantId, String menuCode) {
        String sql = "SELECT id, tenant_id, menu_code, menu_name, menu_path, menu_icon, parent_code, "
                + "sort_order, menu_type, permission_code, permission_name, permission_type, "
                + "data_permission_code, visible, enabled, created_by, created_time, updated_by, updated_time "
                + "FROM sec_menu_permission WHERE tenant_id = ? AND menu_code = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, menuCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMenuPermission(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find menu permission by code failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private List<Long> findUserRoleIds(Long tenantId, String userId) {
        String sql = "SELECT r.id FROM sec_user_role ur "
                + "JOIN sec_role r ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id "
                + "WHERE ur.tenant_id = ? AND ur.user_id = ? AND r.status = 'ACTIVE'";

        List<Long> roleIds = new ArrayList<Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, Long.parseLong(userId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roleIds.add(rs.getLong(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find user role ids failed: " + ex.getMessage(), ex);
        } catch (NumberFormatException ex) {
            log.warn("invalid userId format: {}", userId);
            return roleIds;
        }
        return roleIds;
    }

    private boolean checkRolePermission(Long tenantId, List<Long> roleIds, String permissionCode) {
        if (roleIds.isEmpty() || permissionCode == null || permissionCode.isEmpty()) {
            return false;
        }

        // 先从 sec_permission 查找 permission_id
        String permSql = "SELECT id FROM sec_permission WHERE tenant_id = ? AND permission_code = ?";
        Long permissionId = null;
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(permSql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, permissionCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    permissionId = rs.getLong(1);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find permission by code failed: " + ex.getMessage(), ex);
        }

        if (permissionId == null) {
            // 权限编码不存在于 sec_permission 表，尝试从 sec_menu_permission 直接匹配
            return checkMenuPermissionDirect(tenantId, roleIds, permissionCode);
        }

        // 从 sec_role_permission 检查角色是否拥有该权限
        StringBuilder rpSql = new StringBuilder(
                "SELECT COUNT(*) FROM sec_role_permission WHERE tenant_id = ? AND permission_id = ? AND role_id IN (");
        for (int i = 0; i < roleIds.size(); i++) {
            if (i > 0) {
                rpSql.append(",");
            }
            rpSql.append("?");
        }
        rpSql.append(")");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(rpSql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            ps.setLong(idx++, permissionId);
            for (Long roleId : roleIds) {
                ps.setLong(idx++, roleId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("check role permission failed: " + ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * 当 sec_permission 中不存在该权限编码时，回退到 sec_menu_permission 直接匹配。
     * 检查角色是否通过 sec_role_permission 关联了 sec_menu_permission 中的权限。
     */
    private boolean checkMenuPermissionDirect(Long tenantId, List<Long> roleIds, String permissionCode) {
        // 查找 sec_menu_permission 中的权限编码对应的记录
        String mpSql = "SELECT id FROM sec_menu_permission WHERE tenant_id = ? AND permission_code = ? AND enabled = 'Y'";
        List<Long> menuPermIds = new ArrayList<Long>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(mpSql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, permissionCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menuPermIds.add(rs.getLong(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find menu permission by code failed: " + ex.getMessage(), ex);
        }

        if (menuPermIds.isEmpty()) {
            return false;
        }

        // 检查角色是否关联了这些菜单权限
        for (Long menuPermId : menuPermIds) {
            StringBuilder rpSql = new StringBuilder(
                    "SELECT COUNT(*) FROM sec_role_permission WHERE tenant_id = ? AND permission_id = ? AND role_id IN (");
            for (int i = 0; i < roleIds.size(); i++) {
                if (i > 0) {
                    rpSql.append(",");
                }
                rpSql.append("?");
            }
            rpSql.append(")");

            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(rpSql.toString())) {
                int idx = 1;
                ps.setLong(idx++, tenantId);
                ps.setLong(idx++, menuPermId);
                for (Long roleId : roleIds) {
                    ps.setLong(idx++, roleId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("check menu permission direct failed: " + ex.getMessage(), ex);
            }
        }
        return false;
    }

    private List<String> findRolePermissionCodes(Long tenantId, List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return new ArrayList<String>();
        }

        // 从 sec_role_permission + sec_permission 获取权限编码
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT p.permission_code FROM sec_role_permission rp "
                        + "JOIN sec_permission p ON rp.permission_id = p.id AND rp.tenant_id = p.tenant_id "
                        + "WHERE rp.tenant_id = ? AND rp.role_id IN (");
        for (int i = 0; i < roleIds.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(")");

        List<String> codes = new ArrayList<String>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            for (Long roleId : roleIds) {
                ps.setLong(idx++, roleId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find role permission codes failed: " + ex.getMessage(), ex);
        }

        // 同时从 sec_menu_permission 获取关联的权限编码
        StringBuilder mpSql = new StringBuilder(
                "SELECT DISTINCT mp.permission_code FROM sec_role_permission rp "
                        + "JOIN sec_menu_permission mp ON rp.permission_id = mp.id AND rp.tenant_id = mp.tenant_id "
                        + "WHERE rp.tenant_id = ? AND mp.permission_code IS NOT NULL AND mp.enabled = 'Y' AND rp.role_id IN (");
        for (int i = 0; i < roleIds.size(); i++) {
            if (i > 0) {
                mpSql.append(",");
            }
            mpSql.append("?");
        }
        mpSql.append(")");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(mpSql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            for (Long roleId : roleIds) {
                ps.setLong(idx++, roleId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    if (code != null && !code.isEmpty() && !codes.contains(code)) {
                        codes.add(code);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find menu permission codes failed: " + ex.getMessage(), ex);
        }

        return codes;
    }

    private List<MenuPermission> pruneEmptyDirectories(List<MenuPermission> menus) {
        // 收集所有存在的 menuCode
        java.util.Set<String> codes = new java.util.HashSet<String>();
        for (MenuPermission mp : menus) {
            codes.add(mp.getMenuCode());
        }
        // 清除 parent_code 不在 codes 中的非根目录项
        List<MenuPermission> pruned = new ArrayList<MenuPermission>();
        for (MenuPermission mp : menus) {
            String parentCode = mp.getParentCode();
            if (parentCode == null || parentCode.isEmpty() || codes.contains(parentCode)) {
                pruned.add(mp);
            }
        }
        return pruned;
    }

    private List<Map<String, Object>> buildTree(List<MenuPermission> menus, String parentCode) {
        List<Map<String, Object>> tree = new ArrayList<Map<String, Object>>();
        for (MenuPermission mp : menus) {
            String pc = mp.getParentCode();
            boolean matches = (parentCode == null && (pc == null || pc.isEmpty()))
                    || (parentCode != null && parentCode.equals(pc));
            if (!matches) {
                continue;
            }
            Map<String, Object> node = toMenuView(mp);
            List<Map<String, Object>> children = buildTree(menus, mp.getMenuCode());
            if (!children.isEmpty()) {
                node.put("children", children);
            }
            tree.add(node);
        }
        return tree;
    }

    private Map<String, Object> toMenuView(MenuPermission mp) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", mp.getId());
        view.put("tenant_id", mp.getTenantId());
        view.put("menu_code", mp.getMenuCode());
        view.put("menu_name", mp.getMenuName());
        view.put("menu_path", mp.getMenuPath());
        view.put("menu_icon", mp.getMenuIcon());
        view.put("parent_code", mp.getParentCode());
        view.put("sort_order", mp.getSortOrder());
        view.put("menu_type", mp.getMenuType());
        view.put("permission_code", mp.getPermissionCode());
        view.put("permission_name", mp.getPermissionName());
        view.put("permission_type", mp.getPermissionType());
        view.put("data_permission_code", mp.getDataPermissionCode());
        view.put("visible", mp.getVisible());
        view.put("enabled", mp.getEnabled());
        view.put("created_by", mp.getCreatedBy());
        view.put("created_time", mp.getCreatedTime());
        view.put("updated_by", mp.getUpdatedBy());
        view.put("updated_time", mp.getUpdatedTime());
        return view;
    }

    private MenuPermission mapMenuPermission(ResultSet rs) throws SQLException {
        MenuPermission mp = new MenuPermission();
        mp.setId(rs.getLong("id"));
        mp.setTenantId(rs.getLong("tenant_id"));
        mp.setMenuCode(rs.getString("menu_code"));
        mp.setMenuName(rs.getString("menu_name"));
        mp.setMenuPath(rs.getString("menu_path"));
        mp.setMenuIcon(rs.getString("menu_icon"));
        mp.setParentCode(rs.getString("parent_code"));
        mp.setSortOrder(rs.getInt("sort_order"));
        mp.setMenuType(rs.getString("menu_type"));
        mp.setPermissionCode(rs.getString("permission_code"));
        mp.setPermissionName(rs.getString("permission_name"));
        mp.setPermissionType(rs.getString("permission_type"));
        mp.setDataPermissionCode(rs.getString("data_permission_code"));
        mp.setVisible(rs.getString("visible"));
        mp.setEnabled(rs.getString("enabled"));
        mp.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) {
            mp.setCreatedTime(created.toLocalDateTime());
        }
        mp.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) {
            mp.setUpdatedTime(updated.toLocalDateTime());
        }
        return mp;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
