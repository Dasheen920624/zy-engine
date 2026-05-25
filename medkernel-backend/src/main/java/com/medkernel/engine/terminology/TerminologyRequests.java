package com.medkernel.engine.terminology;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ConfirmMappingRequest(
    @Size(max = 500) String reviewNote,
    @Size(max = 1024) String evidenceOverride
) {
}

record ResolveConflictRequest(@NotBlank @Size(max = 500) String resolutionNote) {
}

record BuildTerminologyPackageRequest(
    @NotBlank @Size(max = 128) String packageCode,
    @NotBlank @Size(max = 64) String packageVersion,
    @NotBlank @Size(max = 32) String scopeLevel,
    @NotBlank @Size(max = 64) String scopeCode,
    @NotBlank @Size(max = 256) String displayName
) {
}

record PublishTerminologyPackageRequest(
    @NotNull PackageReleaseMode releaseMode,
    @NotBlank @Size(max = 500) String reason,
    @Size(max = 2048) String grayScopeJson
) {
}

record RollbackTerminologyPackageRequest(
    @NotNull Long targetPackageId,
    @NotBlank @Size(max = 500) String reason
) {
}
