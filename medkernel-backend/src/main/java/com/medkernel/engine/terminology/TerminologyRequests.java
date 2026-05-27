// 术语映射模块各写操作 API 的 Request 入参聚合（与 Controller / Service 写方法对应）。
package com.medkernel.engine.terminology;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 确认候选映射请求体。
 *
 * <p>reviewNote 审核备注；evidenceOverride 可覆盖候选自带的证据文本，非空则取此值作为正式映射证据。
 */
record ConfirmMappingRequest(
    @Size(max = 500) String reviewNote,
    @Size(max = 1024) String evidenceOverride
) {
}

/**
 * 冲突处置请求体；resolutionNote 必填，作为冲突处置原因留痕。
 */
record ResolveConflictRequest(@NotBlank @Size(max = 500) String resolutionNote) {
}

/**
 * 构建术语映射包请求体。
 *
 * <p>packageCode 业务编码 + packageVersion 版本号 + scopeLevel/scopeCode 作用域，
 * 与 (tenant_id) 一起构成包业务唯一键；displayName 用于前台展示。
 */
record BuildTerminologyPackageRequest(
    @NotBlank @Size(max = 128) String packageCode,
    @NotBlank @Size(max = 64) String packageVersion,
    @NotBlank @Size(max = 32) String scopeLevel,
    @NotBlank @Size(max = 64) String scopeCode,
    @NotBlank @Size(max = 256) String displayName
) {
}

/**
 * 发布术语映射包请求体。
 *
 * <p>releaseMode GRAY/FULL；reason 必填留痕；grayScopeJson 灰度发布时声明灰度作用域 JSON。
 */
record PublishTerminologyPackageRequest(
    @NotNull PackageReleaseMode releaseMode,
    @NotBlank @Size(max = 500) String reason,
    @Size(max = 2048) String grayScopeJson
) {
}

/**
 * 回滚术语映射包请求体；targetPackageId 必须是同 packageCode + scope 下的可回滚版本。
 */
record RollbackTerminologyPackageRequest(
    @NotNull Long targetPackageId,
    @NotBlank @Size(max = 500) String reason
) {
}
