package com.medkernel.engine.mpi;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;

/**
 * 患者主索引（MPI）控制器。
 *
 * <p>提供多租户强隔离下的 MPI 患者列表检索、多维度统计以及原子合并服务。
 * 全线经过 @DataScope(requireTenant = true) 隔离校验，防止垂直越权。
 */
@RestController
@RequestMapping("/api/v1/clinical/mpi")
@DataScope(requireTenant = true)
public class MpiController {

    private final MpiService service;

    public MpiController(MpiService service) {
        this.service = service;
    }

    /**
     * 分页查询当前租户下的患者主索引列表。
     *
     * @param keyword 姓名或 ID 模糊检索关键词（可选）
     * @param status  状态筛选（ACTIVE / MERGED_INTO，可选）
     * @param page    当前页码，从 1 起（默认 1）
     * @param size    每页条数，最大 200（默认 20）
     * @param sort    排序规则（可选）
     * @return 统一格式的分页数据包装
     */
    @GetMapping("/patients")
    @PreAuthorize("@perm.has('mpi.read')")
    public ApiResult<PageResponse<MpiPatient>> getPatients(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        PageRequest pageReq = new PageRequest(page, size, sort);
        return ApiResult.ok(service.getPatients(keyword, status, pageReq));
    }

    /**
     * 获取当前租户下的患者主索引驾驶舱核心指标统计。
     *
     * @return 统一格式的驾驶舱指标统计数据
     */
    @GetMapping("/stats")
    @PreAuthorize("@perm.has('mpi.read')")
    public ApiResult<MpiStatsResponse> getStats() {
        return ApiResult.ok(service.getStats());
    }

    /**
     * 物理合并源与目标患者主索引。
     *
     * @param request 患者合并请求负载，包含源 MPI ID 与目标 MPI ID
     * @return 空成功响应
     */
    @PostMapping("/patients/merge")
    @PreAuthorize("@perm.has('mpi.write')")
    public ApiResult<Void> mergePatients(@Valid @RequestBody MpiMergeRequest request) {
        service.mergePatients(request.sourceMpiId(), request.targetMpiId());
        return ApiResult.empty();
    }
}
