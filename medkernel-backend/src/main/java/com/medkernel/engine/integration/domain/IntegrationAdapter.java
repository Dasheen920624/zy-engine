package com.medkernel.engine.integration.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 院内异构系统接入适配器实体 (IntegrationAdapter) - Spring Data JDBC Record 风格。
 */
@Table("integration_adapter")
public record IntegrationAdapter(
    @Id Long id,
    @Column("adapter_id") String adapterId,
    @Column("tenant_id") String tenantId,
    @Column("name") String name,
    @Column("protocol_type") String protocolType,
    @Column("status") String status, // ACTIVE, SUSPENDED
    @Column("config_json") String configJson,
    @Column("health_status") String healthStatus, // HEALTHY, UNHEALTHY
    @Column("rtt_ms") Long rttMs,
    @Column("last_heartbeat_at") Instant lastHeartbeatAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {
    public IntegrationAdapter withPing(Long newRtt, String newConfigReport, Instant timestamp) {
        return new IntegrationAdapter(id, adapterId, tenantId, name, protocolType, status, newConfigReport, healthStatus, newRtt, timestamp, createdAt, createdBy, Instant.now(), updatedBy);
    }

    public IntegrationAdapter withUpdate(String newName, String newProtocol, String newConfig, String newStatus) {
        return new IntegrationAdapter(id, adapterId, tenantId, newName, newProtocol, newStatus, newConfig, healthStatus, rttMs, lastHeartbeatAt, createdAt, createdBy, Instant.now(), updatedBy);
    }
}
