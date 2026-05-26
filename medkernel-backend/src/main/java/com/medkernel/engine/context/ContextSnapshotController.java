package com.medkernel.engine.context;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;

/**
 * GA-ENG-API-01 标准上下文 API。
 *
 * <p>三接口：POST 创建 / GET by ID / GET 列表。
 * <ul>
 *   <li>类级 {@link DataScope}({@code requireTenant=true})：所有方法都需要租户上下文</li>
 *   <li>方法级 {@code @perm.has('context.write'/'context.read')}：分别控制写读权限</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/engine/context/snapshots")
@DataScope(requireTenant = true)
public class ContextSnapshotController {

    private final ContextSnapshotService service;

    public ContextSnapshotController(ContextSnapshotService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('context.write')")
    public ResponseEntity<ApiResult<ContextSnapshotResponse>> create(
            @RequestBody @Valid ContextSnapshotRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ContextSnapshotResponse resp = service.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(resp));
    }

    @GetMapping("/{snapshotId}")
    @PreAuthorize("@perm.has('context.read')")
    public ApiResult<ContextSnapshotResponse> findById(@PathVariable String snapshotId) {
        return ApiResult.ok(service.findById(snapshotId));
    }

    @GetMapping
    @PreAuthorize("@perm.has('context.read')")
    public ApiResult<PageResponse<ContextSnapshotSummary>> list(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String encounterId,
            @RequestParam(required = false) ContextSnapshotStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        ContextSnapshotFilter filter = new ContextSnapshotFilter(patientId, encounterId, status, null, null);
        PageRequest req = new PageRequest(page, size, sort);
        return ApiResult.ok(service.list(filter, req));
    }
}
