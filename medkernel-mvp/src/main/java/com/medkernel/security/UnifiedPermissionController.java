package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEC-008 统一权限管理 API：菜单权限管理 + 统一权限检查。
 */
@Tag(name = "Unified Permission")
@RestController
@RequestMapping("/api/security/unified-permission")
public class UnifiedPermissionController {

    private final UnifiedPermissionService unifiedPermissionService;
    private final OrganizationContextService organizationContextService;

    public UnifiedPermissionController(UnifiedPermissionService unifiedPermissionService,
                                        OrganizationContextService organizationContextService) {
        this.unifiedPermissionService = unifiedPermissionService;
        this.organizationContextService = organizationContextService;
    }

    // ============================================================
    // 菜单权限管理
    // ============================================================

    /**
     * 创建菜单权限。
     */
    @PostMapping("/menus")
    public ApiResult<Map<String, Object>> createMenuPermission(@RequestBody Map<String, Object> body,
                                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        MenuPermission menu = new MenuPermission();
        menu.setTenantId(resolveTenantId(orgCtx));
        menu.setMenuCode(stringValue(body, "menu_code", "menuCode"));
        menu.setMenuName(stringValue(body, "menu_name", "menuName"));
        menu.setMenuPath(stringValue(body, "menu_path", "menuPath"));
        menu.setMenuIcon(stringValue(body, "menu_icon", "menuIcon"));
        menu.setParentCode(stringValue(body, "parent_code", "parentCode"));
        menu.setSortOrder(intValue(body, "sort_order", "sortOrder", 0));
        menu.setMenuType(stringValue(body, "menu_type", "menuType"));
        menu.setPermissionCode(stringValue(body, "permission_code", "permissionCode"));
        menu.setPermissionName(stringValue(body, "permission_name", "permissionName"));
        menu.setPermissionType(stringValue(body, "permission_type", "permissionType"));
        menu.setDataPermissionCode(stringValue(body, "data_permission_code", "dataPermissionCode"));
        menu.setVisible(stringValue(body, "visible", "visible"));
        menu.setEnabled(stringValue(body, "enabled", "enabled"));
        menu.setCreatedBy(resolveOperator(httpRequest));

        if (menu.getMenuCode() == null || menu.getMenuCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "menu_code is required");
        }
        if (menu.getMenuName() == null || menu.getMenuName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "menu_name is required");
        }

        try {
            MenuPermission created = unifiedPermissionService.createMenuPermission(menu);
            return ApiResult.success(toMenuView(created));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    /**
     * 更新菜单权限。
     */
    @Operation(summary = "更新菜单权限")
    @PutMapping("/menus/{menuId}")
    public ApiResult<Map<String, Object>> updateMenuPermission(@PathVariable Long menuId,
                                                                 @RequestBody Map<String, Object> body,
                                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        MenuPermission menu = new MenuPermission();
        menu.setId(menuId);
        menu.setTenantId(resolveTenantId(orgCtx));
        menu.setMenuName(stringValue(body, "menu_name", "menuName"));
        menu.setMenuPath(stringValue(body, "menu_path", "menuPath"));
        menu.setMenuIcon(stringValue(body, "menu_icon", "menuIcon"));
        menu.setParentCode(stringValue(body, "parent_code", "parentCode"));
        menu.setSortOrder(intValue(body, "sort_order", "sortOrder", 0));
        menu.setMenuType(stringValue(body, "menu_type", "menuType"));
        menu.setPermissionCode(stringValue(body, "permission_code", "permissionCode"));
        menu.setPermissionName(stringValue(body, "permission_name", "permissionName"));
        menu.setPermissionType(stringValue(body, "permission_type", "permissionType"));
        menu.setDataPermissionCode(stringValue(body, "data_permission_code", "dataPermissionCode"));
        menu.setVisible(stringValue(body, "visible", "visible"));
        menu.setEnabled(stringValue(body, "enabled", "enabled"));
        menu.setUpdatedBy(resolveOperator(httpRequest));

        try {
            MenuPermission updated = unifiedPermissionService.updateMenuPermission(menu);
            return ApiResult.success(toMenuView(updated));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 查询菜单权限列表。
     */
    @GetMapping("/menus")
    public ApiResult<List<Map<String, Object>>> listMenuPermissions(
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) String parentCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<MenuPermission> menus = unifiedPermissionService.listMenuPermissions(
                resolveTenantId(orgCtx), menuType, parentCode);
        return ApiResult.success(toMenuViews(menus));
    }

    /**
     * 获取菜单树。
     */
    @GetMapping("/menu-tree")
    public ApiResult<List<Map<String, Object>>> getMenuTree(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> tree = unifiedPermissionService.getMenuTree(resolveTenantId(orgCtx));
        return ApiResult.success(tree);
    }

    // ============================================================
    // 统一权限检查
    // ============================================================

    /**
     * 获取用户菜单树。
     */
    @GetMapping("/user-menu-tree")
    public ApiResult<List<Map<String, Object>>> getUserMenuTree(
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> tree = unifiedPermissionService.getUserMenuTree(
                resolveTenantId(orgCtx), userId);
        return ApiResult.success(tree);
    }

    /**
     * 获取用户按钮权限。
     */
    @Operation(summary = "获取用户按钮权限")
    @GetMapping("/user-buttons")
    public ApiResult<List<Map<String, Object>>> getUserButtonPermissions(
            @RequestParam String userId,
            @RequestParam(required = false) String menuCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> buttons = unifiedPermissionService.getUserButtonPermissions(
                resolveTenantId(orgCtx), userId, menuCode);
        return ApiResult.success(buttons);
    }

    /**
     * 检查菜单权限。
     */
    @PostMapping("/check-menu")
    public ApiResult<Map<String, Object>> checkMenuAccess(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long tenantId = resolveTenantId(orgCtx);
        String userId = stringValue(body, "user_id", "userId");
        String menuCode = stringValue(body, "menu_code", "menuCode");

        if (userId == null || userId.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "user_id is required");
        }
        if (menuCode == null || menuCode.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "menu_code is required");
        }

        boolean allowed = unifiedPermissionService.checkMenuAccess(tenantId, userId, menuCode);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("allowed", allowed);
        result.put("user_id", userId);
        result.put("menu_code", menuCode);
        return ApiResult.success(result);
    }

    /**
     * 检查按钮权限。
     */
    @Operation(summary = "检查按钮权限")
    @PostMapping("/check-button")
    public ApiResult<Map<String, Object>> checkButtonAccess(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long tenantId = resolveTenantId(orgCtx);
        String userId = stringValue(body, "user_id", "userId");
        String permissionCode = stringValue(body, "permission_code", "permissionCode");

        if (userId == null || userId.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "user_id is required");
        }
        if (permissionCode == null || permissionCode.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "permission_code is required");
        }

        boolean allowed = unifiedPermissionService.checkButtonAccess(tenantId, userId, permissionCode);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("allowed", allowed);
        result.put("user_id", userId);
        result.put("permission_code", permissionCode);
        return ApiResult.success(result);
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }

    private String resolveOperator(HttpServletRequest request) {
        String username = request.getHeader("X-Username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "system";
    }

    private String stringValue(Map<String, Object> body, String snakeKey, String camelKey) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Map<String, Object> body, String snakeKey, String camelKey, int defaultValue) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private List<Map<String, Object>> toMenuViews(List<MenuPermission> menus) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (MenuPermission menu : menus) {
            result.add(toMenuView(menu));
        }
        return result;
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
}
