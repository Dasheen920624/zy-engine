package com.medkernel.security.admin;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.PagedResult;
import com.medkernel.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户管理后台 REST 控制器（PR-FINAL-08a）。
 *
 * <p>端点一览：
 * <pre>
 *   GET  /api/admin/users                — 分页列表（keyword / status / role / page / size）
 *   GET  /api/admin/users/{id}           — 用户详情（含身份绑定）
 *   POST /api/admin/users/{id}/status    — 启用 / 禁用（body: {status}）
 *   POST /api/admin/users/{id}/unlock    — 解锁
 *   POST /api/admin/users/{id}/roles     — 分配角色（body: {role_codes:[...]}）
 *   POST /api/admin/users/{id}/reset-password — 重置密码（body: {new_password}）
 *   POST /api/admin/users/import         — CSV 批量导入（multipart/form-data file=csv）
 *   GET  /api/admin/roles                — 可分配角色列表
 * </pre>
 *
 * <p>鉴权：依赖 SecurityFilter 注入 X-Platform-User-Id；生产环境应在 SecurityFilter
 * 中校验 ADMIN 角色，当前 MVP 阶段通过 Header 信任。
 */
@RestController
@RequestMapping("/api/admin")
public class UserAdminController {

    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    // ─── 列表 ───────────────────────────────────────────────────────────────

    /**
     * 分页查询用户列表。
     *
     * @param keyword 模糊关键字（用户名 / 显示名 / 邮箱）
     * @param status  状态过滤（ACTIVE / DISABLED / 空=全部）
     * @param role    角色编码过滤（空=全部）
     * @param page    页码（默认 1）
     * @param size    每页条数（默认 20，上限 200）
     */
    @GetMapping("/users")
    public ApiResult<PagedResult<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        PagedResult<Map<String, Object>> result =
                userAdminService.listUsers(tenantId, keyword, status, role, page, size);
        return ApiResult.success(result);
    }

    // ─── 详情 ───────────────────────────────────────────────────────────────

    /**
     * 查询用户详情（含身份绑定列表）。
     */
    @GetMapping("/users/{id}")
    public ApiResult<Map<String, Object>> getUserDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        Map<String, Object> detail = userAdminService.getUserDetail(id);
        if (detail == null) {
            return ApiResult.notFound("用户不存在: " + id);
        }
        return ApiResult.success(detail);
    }

    // ─── 状态操作 ────────────────────────────────────────────────────────────

    /**
     * 更新用户状态（ACTIVE / DISABLED）。
     *
     * <p>请求体：{@code {"status": "DISABLED"}}
     */
    @PostMapping("/users/{id}/status")
    public ApiResult<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String status = body.get("status");
        if (status == null || status.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "status 字段必填");
        }
        String operator = currentUsername();
        try {
            userAdminService.updateStatus(id, status, operator);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
        return ApiResult.success(null);
    }

    /**
     * 解锁用户账户。
     */
    @PostMapping("/users/{id}/unlock")
    public ApiResult<Void> unlock(
            @PathVariable Long id,
            HttpServletRequest request) {
        userAdminService.unlock(id, currentUsername());
        return ApiResult.success(null);
    }

    // ─── 角色分配 ────────────────────────────────────────────────────────────

    /**
     * 分配用户角色（幂等替换）。
     *
     * <p>请求体：{@code {"role_codes": ["PLATFORM_ADMIN", "CLINICAL_SPECIALIST"]}}
     */
    @PostMapping("/users/{id}/roles")
    public ApiResult<Void> assignRoles(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<String> roleCodes = (List<String>) body.get("role_codes");
        if (roleCodes == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "role_codes 字段必填");
        }
        Long tenantId = resolveTenantId(request);
        userAdminService.assignRoles(id, tenantId, roleCodes, currentUsername());
        return ApiResult.success(null);
    }

    /**
     * 查询可分配角色列表。
     */
    @GetMapping("/roles")
    public ApiResult<List<Map<String, String>>> listRoles(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        return ApiResult.success(userAdminService.listRoles(tenantId));
    }

    // ─── 密码重置 ────────────────────────────────────────────────────────────

    /**
     * 重置用户密码（管理员操作，不需要旧密码）。
     *
     * <p>请求体：{@code {"new_password": "Mk123456"}}
     */
    @PostMapping("/users/{id}/reset-password")
    public ApiResult<Void> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String newPassword = body.get("new_password");
        if (newPassword == null || newPassword.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "new_password 字段必填");
        }
        try {
            userAdminService.resetPassword(id, newPassword, currentUsername());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
        return ApiResult.success(null);
    }

    // ─── CSV 批量导入 ─────────────────────────────────────────────────────────

    /**
     * CSV 批量导入用户（支持 GB18030 / UTF-8）。
     *
     * <p>表单字段：{@code file}（multipart/form-data）
     */
    @PostMapping("/users/import")
    public ApiResult<Map<String, Object>> importUsers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "上传文件不能为空");
        }
        Long tenantId = resolveTenantId(request);
        try {
            Map<String, Object> result =
                    userAdminService.importCsv(tenantId, currentUsername(), file);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("CSV import failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    // ─── 辅助 ────────────────────────────────────────────────────────────────

    private Long resolveTenantId(HttpServletRequest request) {
        // 优先从 SecurityContext（JWT 解析后注入），其次取请求头（演示 / 测试）
        Long fromCtx = SecurityContext.getTenantId();
        if (fromCtx != null) return fromCtx;
        String header = request.getHeader("X-Tenant-Id");
        if (header != null && !header.isEmpty()) {
            try { return Long.parseLong(header); } catch (NumberFormatException e) { log.warn("Invalid tenant ID header: {}", e.getMessage()); }
        }
        return 1L; // 默认租户（本地开发 / 演示）
    }

    private String currentUsername() {
        String username = SecurityContext.getUsername();
        return username != null ? username : "admin";
    }
}
