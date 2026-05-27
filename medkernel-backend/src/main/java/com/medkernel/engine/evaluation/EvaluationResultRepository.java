package com.medkernel.engine.evaluation;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 评估结果持久化仓库（GA-ENG-API-08 运行结果事实）。
 *
 * <p>支持按运行回放结果、按租户浏览最新结果，以及按指标编码、判定等级和责任科室分页查询。
 */
@Repository
public interface EvaluationResultRepository extends ListCrudRepository<EvaluationResult, Long> {

    /**
     * 按运行 ID 和租户 ID 查询结果，按创建时间升序用于诊断装配。
     */
    List<EvaluationResult> findByRunIdAndTenantIdOrderByCreatedAtAsc(String runId, String tenantId);

    /**
     * 按租户列出评估结果，按创建时间倒序用于管理端浏览。
     */
    List<EvaluationResult> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /**
     * 按指标编码、结果等级和责任科室可选过滤统计结果总数。
     */
    @Query("""
        SELECT COUNT(*)
        FROM evaluation_result
        WHERE tenant_id = :tenantId
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
          AND (:resultLevel IS NULL OR result_level = :resultLevel)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        """)
    long countByFilter(String tenantId, String indicatorCode, String resultLevel, String departmentId);

    /**
     * 与 {@link #countByFilter} 同口径分页查询评估结果，按创建时间倒序返回。
     */
    @Query("""
        SELECT *
        FROM evaluation_result
        WHERE tenant_id = :tenantId
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
          AND (:resultLevel IS NULL OR result_level = :resultLevel)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    List<EvaluationResult> pageByFilter(
        String tenantId, String indicatorCode, String resultLevel, String departmentId, int offset, int limit);
}
