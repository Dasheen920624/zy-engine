package com.medkernel.engine.mpi;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.StateTransitionRecorder;

/**
 * 患者主索引（MPI）业务服务逻辑层。
 *
 * <p>提供多租户强隔离下的患者检索、指标汇总以及跨院区患者身份物理合并。
 * 合并操作在同一事务中进行状态变迁，并触发底座状态机历史记录器。
 */
@Service
public class MpiService {

    private final MpiPatientRepository repository;
    private final StateTransitionRecorder stateTransitionRecorder;

    public MpiService(MpiPatientRepository repository, StateTransitionRecorder stateTransitionRecorder) {
        this.repository = repository;
        this.stateTransitionRecorder = stateTransitionRecorder;
    }

    /**
     * 分页查询当前租户下的患者主索引列表。
     *
     * @param keyword 姓名或主索引ID检索关键字（模糊查询）
     * @param status  主索引状态（ACTIVE / MERGED_INTO）
     * @param pageReq 分页请求参数
     * @return 分页包装的患者列表
     */
    public PageResponse<MpiPatient> getPatients(String keyword, String status, PageRequest pageReq) {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("当前请求上下文中租户 ID 缺失，拒绝查询");
        }

        PageRequest req = pageReq != null ? pageReq : PageRequest.defaults();
        long total = repository.countPatients(tenantId, keyword, status);
        if (total == 0) {
            return PageResponse.empty(req);
        }

        List<MpiPatient> items = repository.findPatients(tenantId, keyword, status, req.safeSize(), req.offset());
        return PageResponse.of(items, req, total);
    }

    /**
     * 获取当前租户下的患者主索引驾驶舱指标统计。
     *
     * @return MPI 统计指标详情
     */
    public MpiStatsResponse getStats() {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("当前请求上下文中租户 ID 缺失，拒绝查询");
        }

        long activeCount = repository.countActive(tenantId);
        long mergedCount = repository.countMerged(tenantId);
        Double avgAgeVal = repository.averageAge(tenantId);
        double averageAge = avgAgeVal != null ? avgAgeVal : 0.0;

        List<MpiPatientRepository.GenderCount> genderCounts = repository.countGender(tenantId);
        Map<String, Long> genderMap = new HashMap<>();
        // 初始填充，保证前端能拿到完整性别分类
        genderMap.put("M", 0L);
        genderMap.put("F", 0L);
        genderMap.put("UNKNOWN", 0L);

        if (genderCounts != null) {
            for (MpiPatientRepository.GenderCount gc : genderCounts) {
                String gender = gc.getGender();
                if (gender == null || gender.isBlank()) {
                    genderMap.put("UNKNOWN", genderMap.getOrDefault("UNKNOWN", 0L) + gc.getCnt());
                } else {
                    genderMap.put(gender.toUpperCase(), gc.getCnt());
                }
            }
        }

        return new MpiStatsResponse(activeCount, mergedCount, averageAge, genderMap);
    }

    /**
     * 物理合并重复患者主索引。
     *
     * <p>在同一个物理事务中，将源 MPI 患者的状态变迁为 MERGED_INTO，设置 mergedIntoMpiId，
     * 并累加目标患者的 mergedCount。同时触发 StateTransitionRecorder 记录审计日志。
     *
     * @param sourceMpiId 被合并的源主索引 ID
     * @param targetMpiId 合并入的目标主索引 ID
     */
    @Transactional
    public void mergePatients(String sourceMpiId, String targetMpiId) {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("当前请求上下文中租户 ID 缺失，拒绝操作");
        }

        if (sourceMpiId == null || sourceMpiId.isBlank() || targetMpiId == null || targetMpiId.isBlank()) {
            throw new IllegalArgumentException("源患者主索引 ID 或目标患者主索引 ID 不能为空");
        }

        if (sourceMpiId.equals(targetMpiId)) {
            throw new IllegalArgumentException("源患者与目标患者不能是同一个患者，无法合并");
        }

        MpiPatient sourcePatient = repository.findByTenantIdAndMpiId(tenantId, sourceMpiId)
            .orElseThrow(() -> new IllegalArgumentException("未找到源患者主索引记录，ID: " + sourceMpiId));

        MpiPatient targetPatient = repository.findByTenantIdAndMpiId(tenantId, targetMpiId)
            .orElseThrow(() -> new IllegalArgumentException("未找到目标患者主索引记录，ID: " + targetMpiId));

        if (!"ACTIVE".equals(sourcePatient.status())) {
            throw new IllegalStateException("源患者状态不是活跃状态（ACTIVE），不能进行合并操作");
        }

        if (!"ACTIVE".equals(targetPatient.status())) {
            throw new IllegalStateException("目标患者状态不是活跃状态（ACTIVE），合并目标必须是活跃患者");
        }

        String actor = RequestContext.currentUserId().orElse("system");
        Instant now = Instant.now();

        // 1. 更新源患者状态为 MERGED_INTO，并指向目标患者
        MpiPatient updatedSource = new MpiPatient(
            sourcePatient.id(),
            sourcePatient.mpiId(),
            sourcePatient.tenantId(),
            sourcePatient.maskedName(),
            sourcePatient.gender(),
            sourcePatient.age(),
            sourcePatient.idLast4(),
            sourcePatient.mergedCount(),
            "MERGED_INTO",
            targetMpiId,
            sourcePatient.createdAt(),
            sourcePatient.createdBy(),
            now,
            actor
        );

        // 2. 更新目标患者的被合并数（累加源患者的被合并数以及源患者本身）
        int newMergedCount = targetPatient.mergedCount() + sourcePatient.mergedCount() + 1;
        MpiPatient updatedTarget = new MpiPatient(
            targetPatient.id(),
            targetPatient.mpiId(),
            targetPatient.tenantId(),
            targetPatient.maskedName(),
            targetPatient.gender(),
            targetPatient.age(),
            targetPatient.idLast4(),
            newMergedCount,
            targetPatient.status(),
            targetPatient.mergedIntoMpiId(),
            targetPatient.createdAt(),
            targetPatient.createdBy(),
            now,
            actor
        );

        repository.save(updatedSource);
        repository.save(updatedTarget);

        // 3. 同事务物理触发可观测性审计
        stateTransitionRecorder.record(
            "mpi_patient",
            sourceMpiId,
            "ACTIVE",
            "MERGED_INTO",
            "物理合并至目标患者主索引：" + targetMpiId,
            null
        );
    }
}
