package com.medkernel.engine.terminology;

enum TermCategory {
    DIAGNOSIS,
    PROCEDURE,
    DRUG,
    DEVICE,
    LAB,
    EXAM,
    ORDER,
    INSURANCE,
    DEPARTMENT,
    DOCUMENT,
    FOLLOWUP,
    OTHER
}

enum TermRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum StandardTermStatus {
    ACTIVE,
    DISABLED
}

enum LocalTermStatus {
    UNMAPPED,
    MAPPED,
    DISABLED
}

enum TermMappingStatus {
    DRAFT,
    CONFIRMED,
    SUPERSEDED,
    ROLLED_BACK
}

enum MappingCandidateStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    EXPIRED
}

enum MappingCandidateSource {
    RULE,
    AI,
    MANUAL,
    IMPORT
}

enum MappingConflictType {
    ONE_TO_MANY,
    MANY_TO_ONE,
    DISABLED_CODE,
    CROSS_SYSTEM_INCONSISTENT,
    HOMONYM,
    SYNONYM_MISMATCH
}

enum MappingConflictStatus {
    OPEN,
    RESOLVED,
    IGNORED
}

enum TermMappingPackageStatus {
    DRAFT,
    GRAY,
    PUBLISHED,
    SUPERSEDED,
    ROLLED_BACK,
    ARCHIVED
}

enum PackageReleaseMode {
    GRAY,
    FULL
}

enum TermPackageReleaseEventType {
    PUBLISH,
    ROLLBACK
}
