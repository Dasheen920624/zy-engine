package com.medkernel.shared.runtime;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeBackupReadiness;
import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeDependencyStatus;
import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeDomesticProfile;
import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeFeatureFlag;
import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeJvmMetadata;
import com.medkernel.shared.runtime.RuntimeOperationsSnapshot.RuntimeOsMetadata;

@Service
public class RuntimeOperationsService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final Environment environment;
    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final RuntimeProperties properties;

    public RuntimeOperationsService(Environment environment,
                                    HealthEndpoint healthEndpoint,
                                    MeterRegistry meterRegistry,
                                    RuntimeProperties properties) {
        this.environment = environment;
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    public RuntimeOperationsSnapshot snapshot() {
        String healthStatus = healthEndpoint.health().getStatus().getCode();
        return new RuntimeOperationsSnapshot(
            environment.getProperty("spring.application.name", "medkernel"),
            properties.getEnvironment(),
            properties.getDeploymentMode(),
            properties.getDatabaseDialect(),
            properties.getMigrationLocation(),
            activeProfiles(),
            healthStatus,
            jvmMetadata(),
            osMetadata(),
            featureFlags(),
            dependencies(healthStatus),
            backupReadiness(),
            domesticProfile(),
            Instant.now()
        );
    }

    private List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.stream(profiles).sorted().toList();
    }

    private RuntimeJvmMetadata jvmMetadata() {
        return new RuntimeJvmMetadata(
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("java.vm.name"),
            environment.getProperty("spring.threads.virtual.enabled", Boolean.class, false),
            Runtime.getRuntime().availableProcessors()
        );
    }

    private RuntimeOsMetadata osMetadata() {
        return new RuntimeOsMetadata(
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")
        );
    }

    private List<RuntimeFeatureFlag> featureFlags() {
        return properties.getFeatureFlags().entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey()))
            .map(entry -> {
                RuntimeProperties.FeatureFlag flag = entry.getValue();
                return new RuntimeFeatureFlag(
                    entry.getKey(),
                    flag.getDisplayName(),
                    flag.isEnabled(),
                    flag.getRisk(),
                    flag.getOwner(),
                    flag.getDescription()
                );
            })
            .toList();
    }

    private List<RuntimeDependencyStatus> dependencies(String healthStatus) {
        boolean prometheusReady = meterRegistry.find("medkernel_tenant_onboarding_total").counter() != null;
        boolean graphEnabled = flagEnabled("graph-projection");
        boolean difyEnabled = flagEnabled("dify-workflow");
        return List.of(
            new RuntimeDependencyStatus(
                "database",
                "关系数据库",
                STATUS_UP.equals(healthStatus) ? STATUS_UP : STATUS_DEGRADED,
                properties.getDatabaseDialect() + " · " + properties.getMigrationLocation()
            ),
            new RuntimeDependencyStatus(
                "prometheus",
                "Prometheus 指标",
                prometheusReady ? STATUS_UP : STATUS_DEGRADED,
                prometheusReady ? "业务指标已注册" : "业务指标未注册"
            ),
            new RuntimeDependencyStatus(
                "backup-restore",
                "备份恢复",
                properties.getBackup().isEnabled() ? STATUS_UP : STATUS_DISABLED,
                properties.getBackup().getChecksumPolicy()
            ),
            new RuntimeDependencyStatus(
                "graph-projection",
                "知识图谱投影",
                graphEnabled ? STATUS_UP : STATUS_DISABLED,
                graphEnabled ? "图谱投影已启用" : "Feature Flag 关闭"
            ),
            new RuntimeDependencyStatus(
                "dify-workflow",
                "Dify 工作流",
                difyEnabled ? STATUS_UP : STATUS_DISABLED,
                difyEnabled ? "Dify 工作流已启用" : "Feature Flag 关闭"
            )
        );
    }

    private boolean flagEnabled(String key) {
        RuntimeProperties.FeatureFlag flag = properties.getFeatureFlags().get(key);
        return flag != null && flag.isEnabled();
    }

    private RuntimeBackupReadiness backupReadiness() {
        RuntimeProperties.Backup backup = properties.getBackup();
        return new RuntimeBackupReadiness(
            backup.isEnabled(),
            backup.getRpo(),
            backup.getRto(),
            backup.getBackupScript(),
            backup.getRestoreScript(),
            backup.getChecksumPolicy()
        );
    }

    private RuntimeDomesticProfile domesticProfile() {
        RuntimeProperties.DomesticProfile profile = properties.getDomesticProfile();
        return new RuntimeDomesticProfile(
            profile.getTargetOs(),
            profile.getTargetJdk(),
            profile.getDatabaseVendors(),
            profile.getCryptoAlgorithms(),
            profile.getEvidence()
        );
    }
}
