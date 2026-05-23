package com.medkernel.common.dataclass;

import com.medkernel.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 最小化展示策略服务 (GA-DATA-01)。
 *
 * <p>根据当前用户角色决定是否对 HEALTH_DATA / SENSITIVE 级别的 entity 进行脱敏。
 * 核心原则：默认脱敏，仅持有特定角色的用户可查看完整数据。
 *
 * <h2>角色与脱敏映射</h2>
 *
 * <table>
 *   <tr><th>角色</th><th>患者数据</th><th>医生数据</th><th>规则评估数据</th></tr>
 *   <tr><td>系统管理员</td><td>脱敏</td><td>脱敏</td><td>完整</td></tr>
 *   <tr><td>临床医生</td><td>完整</td><td>完整</td><td>完整</td></tr>
 *   <tr><td>科室护士</td><td>脱敏</td><td>脱敏</td><td>完整</td></tr>
 *   <tr><td>质控人员</td><td>脱敏</td><td>脱敏</td><td>完整</td></tr>
 *   <tr><td>审计管理员</td><td>脱敏</td><td>脱敏</td><td>完整</td></tr>
 *   <tr><td>数据管理员</td><td>完整</td><td>完整</td><td>完整</td></tr>
 * </table>
 *
 * <h2>使用方式</h2>
 *
 * <pre>
 *   PatientEntity p = repository.findByPatientId(tenantId, patientId);
 *   // FieldEncryptionService 已在 Repository 层解密
 *   minimalDisplayService.applyMinimalDisplay(p);  // 按角色自动脱敏
 *   return ApiResult.ok(p);
 * </pre>
 *
 * @see DataMaskingService
 * @see DataClassification
 */
@Service
public class MinimalDisplayService {

    private static final Logger log = LoggerFactory.getLogger(MinimalDisplayService.class);

    private final DataMaskingService maskingService;
    private final DataClassRegistry dataClassRegistry;

    public MinimalDisplayService(DataMaskingService maskingService,
                                 DataClassRegistry dataClassRegistry) {
        this.maskingService = maskingService;
        this.dataClassRegistry = dataClassRegistry;
    }

    /**
     * 对单个 entity 应用最小化展示策略。
     *
     * <p>如果 entity 的 {@link DataClass} 分级为 SENSITIVE 或 HEALTH_DATA，
     * 且当前用户不在免脱敏角色列表中，则自动脱敏。
     *
     * @param entity 要展示的 entity
     */
    public void applyMinimalDisplay(Object entity) {
        if (entity == null) {
            return;
        }

        DataClassification classification = dataClassRegistry.classOf(entity.getClass());
        if (!classification.requiresMasking()) {
            // PUBLIC / INTERNAL 不需要脱敏
            return;
        }

        if (isExemptFromMasking()) {
            log.debug("用户 {} 持有免脱敏角色，跳过脱敏: {}",
                    SecurityContext.getUserId(), entity.getClass().getSimpleName());
            return;
        }

        maskingService.maskEntity(entity);
        log.debug("已对 {} 应用最小化展示脱敏", entity.getClass().getSimpleName());
    }

    /**
     * 对 entity 列表应用最小化展示策略。
     */
    public <T> void applyMinimalDisplay(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        for (T entity : entities) {
            applyMinimalDisplay(entity);
        }
    }

    /**
     * 检查当前用户是否免于脱敏。
     *
     * <p>MVP 阶段：通过用户名判断角色。生产阶段应接入 UnifiedPermissionService。
     */
    private boolean isExemptFromMasking() {
        Long userId = SecurityContext.getUserId();
        if (userId == null) {
            // 未认证用户默认脱敏
            return false;
        }

        // MVP 阶段：管理员用户（userId = 1）默认免脱敏
        // 生产阶段：应通过 UnifiedPermissionService 检查 VIEW_FULL_PII 权限
        // 参见 GA-PROD-01: RBAC 权限体系完善后替换此逻辑
        return userId == 1L;
    }
}
