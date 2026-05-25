package com.medkernel.engine.terminology;

record StandardTermFilter(String standardSystem, TermCategory category, StandardTermStatus status, String keyword) {
    static StandardTermFilter empty() {
        return new StandardTermFilter(null, null, null, null);
    }
}

record LocalTermFilter(String sourceSystem, TermCategory category, LocalTermStatus status, String keyword) {
    static LocalTermFilter empty() {
        return new LocalTermFilter(null, null, null, null);
    }
}

record MappingFilter(String sourceSystem, TermCategory category, TermMappingStatus status, String keyword) {
    static MappingFilter empty() {
        return new MappingFilter(null, null, null, null);
    }
}

record CandidateFilter(MappingCandidateStatus status, TermRiskLevel riskLevel, Boolean conflictFlag) {
    static CandidateFilter empty() {
        return new CandidateFilter(null, null, null);
    }
}

record ConflictFilter(MappingConflictStatus status, TermRiskLevel riskLevel, MappingConflictType conflictType) {
    static ConflictFilter empty() {
        return new ConflictFilter(null, null, null);
    }
}

record PackageFilter(String packageCode, TermMappingPackageStatus status, String scopeLevel, String scopeCode) {
    static PackageFilter empty() {
        return new PackageFilter(null, null, null, null);
    }
}
