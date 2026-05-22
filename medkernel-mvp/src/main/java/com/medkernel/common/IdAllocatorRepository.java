package com.medkernel.common;

import com.medkernel.persistence.Ids;
import org.springframework.stereotype.Repository;

@Repository
public class IdAllocatorRepository {
    public long nextId() {
        return Ids.next();
    }

    public long nextId(String tenantId) {
        // The current Snowflake generator is globally unique; tenantId keeps callers scoped at repository boundaries.
        return Ids.next();
    }
}
