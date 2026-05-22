package com.medkernel.security.admin;

import com.medkernel.common.PagedResult;
import com.medkernel.persistence.Ids;
import com.medkernel.security.SecurityPersistenceService;
import com.medkernel.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理后台业务服务（PR-FINAL-08a）。
 *
 * <p>职责：
 * <ul>
 *   <li>分页列出用户（含角色 / 状态筛选）</li>
 *   <li>查用户详情（含身份绑定）</li>
 *   <li>启用 / 禁用 / 解锁用户</li>
 *   <li>重置密码（BCrypt pepper = operatorUsername）</li>
 *   <li>分配角色</li>
 *   <li>批量 CSV 导入（GB18030，卫健委系统常见编码）</li>
 * </ul>
 */
@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private static final int DEFAULT_TEMP_PASSWORD_LENGTH = 12;
    private static final Charset GB18030 = Charset.forName("GB18030");

    private final SecurityPersistenceService persistence;
    private final BCryptPasswordEncoder encoder;

    public UserAdminService(SecurityPersistenceService persistence) {
        this.persistence = persistence;
        this.encoder = new BCryptPasswordEncoder();
    }

    // ──────────────────── 列表 ────────────────────

    /**
     * 分页查询用户，返回去除密码哈希的视图列表。
     *
     * @param tenantId 租户 ID
     * @param keyword  模糊搜索（用户名 / 显示名 / 邮箱）
     * @param status   状态过滤（ACTIVE / DISABLED / LOCKED / 空=全部）
     * @param role     角色过滤（role_code / 空=全部）
     * @param page     页码（从 1 开始）
     * @param size     每页条数（1-200）
     */
    public PagedResult<Map<String, Object>> listUsers(Long tenantId, String keyword,
                                                       String status, String role,
                                                       int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(200, Math.max(1, size));
        long total = persistence.countUsers(tenantId, keyword, status, role);
        List<SecurityUser> users = persistence.listUsers(tenantId, keyword, status, role, safePage, safeSize);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (SecurityUser u : users) {
            views.add(toListView(u));
        }
        return new PagedResult<Map<String, Object>>(views, total, safePage, safeSize);
    }

    // ──────────────────── 详情 ────────────────────

    /**
     * 查询用户详情（包含身份绑定、组织范围）。
     */
    public Map<String, Object> getUserDetail(Long userId) {
        SecurityUser user = persistence.findById(userId);
        if (user == null) return null;
        Map<String, Object> view = toListView(user);
        List<com.medkernel.security.SsoIdentityBinding> bindings =
                persistence.findIdentityBindingsByUser(user.getTenantId(), userId);
        List<Map<String, Object>> bindingViews = new ArrayList<Map<String, Object>>();
        for (com.medkernel.security.SsoIdentityBinding b : bindings) {
            Map<String, Object> bv = new LinkedHashMap<String, Object>();
            bv.put("provider_id", b.getProviderId());
            bv.put("external_subject", b.getExternalSubject());
            bv.put("external_display_name", b.getExternalDisplayName());
            bv.put("external_org_code", b.getExternalOrgCode());
            bv.put("binding_status", b.getBindingStatus());
            bv.put("last_verified_time", b.getLastVerifiedTime());
            bindingViews.add(bv);
        }
        view.put("identity_bindings", bindingViews);
        view.put("org_scopes", user.getOrgScopes());
        return view;
    }

    // ──────────────────── 状态操作 ────────────────────

    /**
     * 更新用户状态（ACTIVE / DISABLED）。
     */
    public void updateStatus(Long userId, String status, String operatorUsername) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("status 只允许 ACTIVE / DISABLED");
        }
        persistence.updateUserStatus(userId, status, operatorUsername);
        log.info("[user-admin] {} set user {} status={}", operatorUsername, userId, status);
    }

    /**
     * 解锁用户账户（清锁定时间 + 重置连续失败次数）。
     */
    public void unlock(Long userId, String operatorUsername) {
        persistence.unlockUser(userId, operatorUsername);
        log.info("[user-admin] {} unlocked user {}", operatorUsername, userId);
    }

    // ──────────────────── 角色分配 ────────────────────

    /**
     * 替换用户角色（幂等：先清后插）。
     */
    public void assignRoles(Long userId, Long tenantId, List<String> roleCodes, String operatorUsername) {
        persistence.replaceUserRoles(userId, tenantId, roleCodes, operatorUsername);
        log.info("[user-admin] {} assigned roles {} to user {}", operatorUsername, roleCodes, userId);
    }

    /**
     * 列出租户下所有可分配角色。
     */
    public List<Map<String, String>> listRoles(Long tenantId) {
        return persistence.listRoles(tenantId);
    }

    // ──────────────────── 密码重置 ────────────────────

    /**
     * 重置用户密码。
     *
     * @param userId            目标用户 ID
     * @param newPassword       明文密码（调用方已做基本校验）
     * @param operatorUsername  操作人用户名（用于审计）
     */
    public void resetPassword(Long userId, String newPassword, String operatorUsername) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("密码至少 8 位");
        }
        String hash = encoder.encode(newPassword);
        persistence.resetPassword(userId, hash, operatorUsername);
        log.info("[user-admin] {} reset password for user {}", operatorUsername, userId);
    }

    // ──────────────────── CSV 批量导入 ────────────────────

    /**
     * 批量导入用户（CSV，支持 GB18030 / UTF-8）。
     *
     * <p>CSV 格式（首行表头，列顺序不敏感）：
     * <pre>username,display_name,email,phone,user_type,employee_id,password</pre>
     *
     * <p>password 列可省略；省略时自动生成 8 位临时密码（格式 Mk + 6位随机数字）。
     *
     * @return 导入结果摘要
     */
    public Map<String, Object> importCsv(Long tenantId, String operatorUsername,
                                          MultipartFile file) throws IOException {
        // 先尝试 GB18030，回退 UTF-8
        Charset charset = GB18030;
        try {
            new InputStreamReader(file.getInputStream(), GB18030);
        } catch (Exception e) {
            log.warn("Failed to read file with GB18030, falling back to UTF-8: {}", e.getMessage());
            charset = java.nio.charset.StandardCharsets.UTF_8;
        }

        List<String[]> rows = parseCsv(file, charset);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 文件无数据行");
        }

        String[] header = rows.get(0);
        int iUsername = indexOf(header, "username");
        int iDisplayName = indexOf(header, "display_name");
        int iEmail = indexOf(header, "email");
        int iPhone = indexOf(header, "phone");
        int iUserType = indexOf(header, "user_type");
        int iEmployeeId = indexOf(header, "employee_id");
        int iPassword = indexOf(header, "password");

        if (iUsername < 0 || iDisplayName < 0) {
            throw new IllegalArgumentException("CSV 必须包含 username 和 display_name 列");
        }

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<String>();

        for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
            String[] cols = rows.get(rowIdx);
            try {
                String username = get(cols, iUsername);
                String displayName = get(cols, iDisplayName);
                if (username == null || username.isEmpty()) {
                    errors.add("第 " + (rowIdx + 1) + " 行 username 为空，已跳过");
                    skipped++;
                    continue;
                }
                if (persistence.usernameExists(tenantId, username)) {
                    errors.add("第 " + (rowIdx + 1) + " 行 username=" + username + " 已存在，已跳过");
                    skipped++;
                    continue;
                }
                String email = get(cols, iEmail);
                String phone = get(cols, iPhone);
                String userType = get(cols, iUserType);
                if (userType == null || userType.isEmpty()) userType = "STAFF";
                String employeeId = get(cols, iEmployeeId);
                String password = get(cols, iPassword);
                if (password == null || password.isEmpty()) {
                    password = "Mk" + String.format("%06d", (int) (Math.random() * 1_000_000));
                }
                String hash = encoder.encode(password);
                persistence.createUserWithPassword(tenantId, username, displayName, email, phone,
                        userType, employeeId, hash, operatorUsername);
                created++;
            } catch (Exception ex) {
                errors.add("第 " + (rowIdx + 1) + " 行导入失败: " + ex.getMessage());
                skipped++;
            }
        }

        log.info("[user-admin] CSV import by {}: created={} skipped={}", operatorUsername, created, skipped);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    // ──────────────────── 私有辅助 ────────────────────

    private Map<String, Object> toListView(SecurityUser u) {
        Map<String, Object> v = new LinkedHashMap<String, Object>();
        v.put("id", u.getId());
        v.put("tenant_id", u.getTenantId());
        v.put("username", u.getUsername());
        v.put("display_name", u.getDisplayName());
        v.put("email", u.getEmail());
        v.put("phone", u.getPhone());
        v.put("avatar_url", u.getAvatarUrl());
        v.put("status", u.getStatus());
        v.put("user_type", u.getUserType());
        v.put("employee_id", u.getEmployeeId());
        v.put("last_login_time", u.getLastLoginTime());
        v.put("last_login_ip", u.getLastLoginIp());
        v.put("login_attempts", u.getLoginAttempts());
        v.put("locked_until", u.getLockedUntil());
        v.put("roles", u.getRoles() != null ? u.getRoles() : java.util.Collections.emptyList());
        return v;
    }

    private static List<String[]> parseCsv(MultipartFile file, Charset charset) throws IOException {
        List<String[]> rows = new ArrayList<String[]>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(splitCsvLine(line));
            }
        }
        return rows;
    }

    /** 简单 CSV 分割（不处理带换行的引用字段，医院批量导入场景够用）。 */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static int indexOf(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (name.equalsIgnoreCase(header[i].trim())) return i;
        }
        return -1;
    }

    private static String get(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        String v = cols[idx].trim();
        return v.isEmpty() ? null : v;
    }
}
