# MedKernel Monitoring Pack

PR-FINAL-24 provides the on-prem monitoring baseline for v0.3-final:

- Spring Boot actuator endpoint: `http://127.0.0.1:18081/medkernel/actuator/prometheus`
- Prometheus scrape template: `prometheus/prometheus-medkernel.yml`
- Prometheus alert rules: `prometheus/medkernel-alert-rules.yml`
- Grafana provisioning: `grafana/provisioning`
- Grafana dashboards: system, JVM, DB pool, API performance, business operations

## Runtime Boundary

The default management bind address is `127.0.0.1`, so Prometheus should run on the
same host or behind a local sidecar. If the hospital monitoring platform scrapes
from a dedicated monitoring VLAN, set:

```bash
MEDKERNEL_MANAGEMENT_ADDRESS=0.0.0.0
MEDKERNEL_MANAGEMENT_PORT=18081
MEDKERNEL_MANAGEMENT_BASE_PATH=/medkernel/actuator
```

Only the monitoring network should be allowed to reach `18081` at the firewall or
reverse-proxy layer. Do not expose this port to office, patient, or internet
segments.

Optional Neo4j actuator health is disabled by default because graph integration is
an optional provider in v0.3-final. Enable `MEDKERNEL_MANAGEMENT_NEO4J_HEALTH_ENABLED=true`
only when Neo4j is a required runtime dependency for that deployment.

## Local Validation

```bash
curl -fsS http://127.0.0.1:18081/medkernel/actuator/health
curl -fsS http://127.0.0.1:18081/medkernel/actuator/prometheus | grep medkernel_provider_ready
promtool check config deploy/monitoring/prometheus/prometheus-medkernel.yml
promtool check rules deploy/monitoring/prometheus/medkernel-alert-rules.yml
```

The DB pool dashboard is wired to standard HikariCP metric names. It will start
showing data after the Hikari PR lands in the backend.
