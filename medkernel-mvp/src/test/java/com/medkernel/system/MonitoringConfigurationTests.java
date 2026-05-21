package com.medkernel.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringConfigurationTests {
    @Test
    void managementEndpointConfigurationExposesPrometheusOnInternalPort() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("management.server.port"))
                .isEqualTo("${MEDKERNEL_MANAGEMENT_PORT:18081}");
        assertThat(properties.getProperty("management.server.address"))
                .isEqualTo("${MEDKERNEL_MANAGEMENT_ADDRESS:127.0.0.1}");
        assertThat(properties.getProperty("management.endpoints.web.base-path"))
                .isEqualTo("${MEDKERNEL_MANAGEMENT_BASE_PATH:/medkernel/actuator}");

        String exposed = properties.getProperty("management.endpoints.web.exposure.include");
        assertThat(Arrays.asList(exposed.split(",")))
                .containsExactly("health", "info", "metrics", "prometheus");
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled"))
                .isEqualTo("true");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("${MEDKERNEL_MANAGEMENT_HEALTH_DETAILS:when_authorized}");
        assertThat(properties.getProperty("management.health.neo4j.enabled"))
                .isEqualTo("${MEDKERNEL_MANAGEMENT_NEO4J_HEALTH_ENABLED:false}");
    }
}
