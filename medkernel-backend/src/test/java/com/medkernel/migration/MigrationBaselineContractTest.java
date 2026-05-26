package com.medkernel.migration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 五方言迁移静态合同门禁。
 *
 * <p>达梦、金仓没有稳定公开容器可供普通 CI 执行，因此这里把版本序列、表族、索引、
 * 业务约束和关键字段作为全部五方言的最低合同；可启动的数据库仍由 Flyway smoke 执行解析验证。
 */
class MigrationBaselineContractTest {

    private static final List<String> DIALECTS = List.of("h2", "postgres", "oracle", "dm", "kingbase");
    private static final List<String> EXPECTED_MIGRATIONS = List.of(
        "V1__init.sql",
        "V2__org_audit_baseline.sql",
        "V3__knowledge_asset_baseline.sql",
        "V4__terminology_mapping_baseline.sql",
        "V5__audit_chain_baseline.sql",
        "V6__security_permission_baseline.sql",
        "V7__clinical_context_baseline.sql"
    );
    private static final Set<String> REQUIRED_TABLES = Set.of(
        "medkernel_meta", "org_unit", "audit_event", "source_document", "source_version",
        "source_fragment", "knowledge_identity", "knowledge_asset_version", "citation",
        "knowledge_supersession", "knowledge_export_job", "standard_term", "local_term",
        "term_mapping", "mapping_candidate", "mapping_conflict", "term_mapping_package",
        "term_mapping_package_item", "term_mapping_package_release", "audit_chain_head",
        "role_permission", "user_role_assignment",
        "context_snapshot", "canonical_resource", "clinical_event", "context_idempotency_key"
    );
    private static final Set<String> REQUIRED_INDEXES = Set.of(
        "idx_org_unit_parent", "idx_org_unit_tenant_lv", "idx_audit_event_resource",
        "idx_audit_event_actor", "idx_audit_event_tenant", "idx_audit_event_trace",
        "idx_source_document_tenant_type", "idx_source_document_tenant_auth",
        "idx_source_version_tenant_doc", "idx_source_fragment_tenant_ver",
        "idx_knowledge_identity_tenant_domain", "idx_knowledge_identity_specialty",
        "idx_knowledge_identity_updated", "idx_knowledge_av_identity_status",
        "idx_knowledge_av_tenant_status", "idx_knowledge_av_tenant_updated",
        "idx_knowledge_av_content_hash", "idx_citation_tenant_av", "idx_citation_fragment",
        "idx_supersession_tenant_identity", "idx_supersession_old", "idx_supersession_new",
        "idx_export_job_tenant_status", "idx_export_job_tenant_created",
        "idx_standard_term_tenant_category", "idx_standard_term_tenant_updated",
        "idx_local_term_tenant_source", "idx_local_term_department",
        "idx_term_mapping_tenant_status", "idx_term_mapping_local_standard",
        "idx_mapping_candidate_tenant_status", "idx_mapping_conflict_tenant_status",
        "idx_term_pkg_tenant_status", "idx_term_pkg_scope", "idx_term_pkg_item_package",
        "idx_term_pkg_release_package", "idx_role_permission_tenant_role",
        "idx_user_role_assignment_user",
        "idx_context_snapshot_tenant_patient", "idx_context_snapshot_tenant_enc",
        "idx_context_snapshot_status", "idx_canonical_resource_snapshot",
        "idx_canonical_resource_tenant_type", "idx_clinical_event_tenant_received",
        "idx_clinical_event_snapshot", "idx_context_idempotency_expires"
    );
    private static final Set<String> COMMON_CONSTRAINTS = Set.of(
        "uk_org_unit_tenant_code", "ck_org_unit_level", "ck_org_unit_status",
        "uk_audit_event_event_id", "ck_audit_event_status",
        "uk_source_document_tenant_code", "ck_source_document_type", "ck_source_document_authority",
        "uk_source_version_doc_no", "uk_source_fragment_version_anchor",
        "uk_knowledge_identity_tenant_code", "ck_knowledge_identity_domain", "ck_knowledge_identity_status",
        "uk_knowledge_asset_version", "ck_knowledge_asset_version_status", "ck_knowledge_asset_version_risk",
        "uk_citation_av_fragment", "ck_citation_relation", "ck_knowledge_supersession_type",
        "uk_knowledge_export_job_code", "ck_knowledge_export_job_type", "ck_knowledge_export_job_status",
        "uk_standard_term_code", "ck_standard_term_category", "ck_standard_term_status",
        "uk_local_term_code", "ck_local_term_category", "ck_local_term_status",
        "ck_term_mapping_status", "ck_term_mapping_risk",
        "ck_mapping_candidate_status", "ck_mapping_candidate_source", "ck_mapping_candidate_risk",
        "ck_mapping_conflict_type", "ck_mapping_conflict_status", "ck_mapping_conflict_risk",
        "uk_term_mapping_package", "ck_term_mapping_package_status",
        "ck_term_pkg_release_event", "ck_term_pkg_release_mode",
        "uk_role_permission", "ck_role_permission_effect",
        "uk_user_role_assignment", "ck_user_role_assignment_active",
        "uk_context_snapshot_id", "ck_context_snapshot_status", "ck_context_snapshot_quality",
        "uk_canonical_resource_id", "ck_canonical_resource_type", "ck_canonical_resource_quality",
        "uk_clinical_event_id", "ck_clinical_event_type", "ck_clinical_event_status",
        "uk_context_idempotency_tenant_key"
    );
    private static final Set<String> TENANT_TABLES = Set.of(
        "org_unit", "audit_event", "source_document", "source_version", "source_fragment",
        "knowledge_identity", "knowledge_asset_version", "citation", "knowledge_supersession",
        "knowledge_export_job", "standard_term", "local_term", "term_mapping", "mapping_candidate",
        "mapping_conflict", "term_mapping_package", "term_mapping_package_item",
        "term_mapping_package_release", "audit_chain_head", "role_permission", "user_role_assignment",
        "context_snapshot", "canonical_resource", "clinical_event", "context_idempotency_key"
    );
    private static final Set<String> MUTABLE_AUDITED_TABLES = Set.of(
        "org_unit", "source_document", "knowledge_identity", "knowledge_asset_version",
        "standard_term", "local_term", "term_mapping", "mapping_candidate", "mapping_conflict",
        "term_mapping_package", "role_permission", "user_role_assignment"
    );
    private static final Map<String, Set<String>> TECHNICAL_AUDIT_FIELDS = Map.of(
        "audit_event", Set.of("occurred_at", "actor_user_id", "created_at"),
        "knowledge_supersession", Set.of("transitioned_at", "transitioned_by"),
        "knowledge_export_job", Set.of("requested_by", "created_at", "started_at", "completed_at", "expires_at"),
        "term_mapping_package_release", Set.of("created_at", "created_by"),
        "audit_chain_head", Set.of("last_signature", "updated_at")
    );
    private static final Map<String, Set<String>> LIFECYCLE_FIELDS = Map.ofEntries(
        Map.entry("org_unit", Set.of("status")),
        Map.entry("audit_event", Set.of("status")),
        Map.entry("source_version", Set.of("version_no")),
        Map.entry("knowledge_identity", Set.of("status")),
        Map.entry("knowledge_asset_version", Set.of("version_no", "status")),
        Map.entry("knowledge_export_job", Set.of("status")),
        Map.entry("standard_term", Set.of("version_no", "status")),
        Map.entry("local_term", Set.of("status")),
        Map.entry("term_mapping", Set.of("status")),
        Map.entry("mapping_candidate", Set.of("status")),
        Map.entry("mapping_conflict", Set.of("status")),
        Map.entry("term_mapping_package", Set.of("package_version", "status")),
        Map.entry("context_snapshot", Set.of("status", "quality_status")),
        Map.entry("clinical_event", Set.of("processing_status"))
    );

    private static final Pattern TABLE_PATTERN =
        Pattern.compile("(?i)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+([a-z0-9_]+)");
    private static final Pattern TABLE_BLOCK_PATTERN = Pattern.compile(
        "(?is)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+([a-z0-9_]+)\\s*\\((.*?)\\);");
    private static final Pattern INDEX_PATTERN = Pattern.compile(
        "(?i)CREATE\\s+(?:UNIQUE\\s+)?INDEX(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+([a-z0-9_]+)");
    private static final Pattern CONSTRAINT_PATTERN =
        Pattern.compile("(?i)CONSTRAINT\\s+([a-z0-9_]+)");

    @Test
    void everyDialectPublishesTheSameAuthoritativeMigrationSequence() throws IOException {
        for (String dialect : DIALECTS) {
            assertThat(migrationFiles(dialect))
                .as("%s 权威迁移序列", dialect)
                .containsExactlyElementsOf(EXPECTED_MIGRATIONS);
        }
    }

    @Test
    void everyDialectPreservesRequiredTablesIndexesAndBusinessConstraints() throws IOException {
        for (String dialect : DIALECTS) {
            String ddl = combinedDdl(dialect);
            assertThat(names(TABLE_PATTERN, ddl)).as("%s 表族", dialect)
                .containsExactlyInAnyOrderElementsOf(REQUIRED_TABLES);
            assertThat(names(INDEX_PATTERN, ddl)).as("%s 索引", dialect)
                .containsExactlyInAnyOrderElementsOf(REQUIRED_INDEXES);

            Set<String> expectedConstraints = COMMON_CONSTRAINTS;
            if (dialect.equals("oracle") || dialect.equals("dm")) {
                expectedConstraints = new HashSet<>(COMMON_CONSTRAINTS);
                expectedConstraints.add("ck_mapping_candidate_conflict");
            }
            assertThat(names(CONSTRAINT_PATTERN, ddl)).as("%s 业务约束", dialect)
                .containsExactlyInAnyOrderElementsOf(expectedConstraints);
        }
    }

    @Test
    void tenantIsolationAuditAndLifecycleColumnsRemainPresent() throws IOException {
        for (String dialect : DIALECTS) {
            Map<String, String> tables = tableBlocks(combinedDdl(dialect));
            TENANT_TABLES.forEach(table ->
                assertThat(tables.get(table)).as("%s.%s 租户字段", dialect, table).contains("tenant_id"));
            MUTABLE_AUDITED_TABLES.forEach(table ->
                assertThat(tables.get(table)).as("%s.%s 审计字段", dialect, table)
                    .contains("created_at", "created_by", "updated_at", "updated_by"));
            TECHNICAL_AUDIT_FIELDS.forEach((table, fields) ->
                assertThat(tables.get(table)).as("%s.%s 专属审计字段", dialect, table)
                    .contains(fields.toArray(String[]::new)));
            LIFECYCLE_FIELDS.forEach((table, fields) ->
                assertThat(tables.get(table)).as("%s.%s 状态或版本字段", dialect, table)
                    .contains(fields.toArray(String[]::new)));
        }
    }

    private List<String> migrationFiles(String dialect) throws IOException {
        try (var files = Files.list(migrationPath(dialect))) {
            return files.map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".sql"))
                .sorted()
                .toList();
        }
    }

    private String combinedDdl(String dialect) throws IOException {
        StringBuilder ddl = new StringBuilder();
        for (String migration : EXPECTED_MIGRATIONS) {
            ddl.append(Files.readString(migrationPath(dialect).resolve(migration))).append('\n');
        }
        return ddl.toString().toLowerCase(Locale.ROOT);
    }

    private Path migrationPath(String dialect) {
        var resource = getClass().getClassLoader().getResource("db/migration/" + dialect);
        assertThat(resource).as("%s 迁移资源目录", dialect).isNotNull();
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("无法读取迁移资源目录: " + dialect, exception);
        }
    }

    private Set<String> names(Pattern pattern, String ddl) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        var matcher = pattern.matcher(ddl);
        while (matcher.find()) {
            names.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private Map<String, String> tableBlocks(String ddl) {
        Map<String, String> blocks = new LinkedHashMap<>();
        var matcher = TABLE_BLOCK_PATTERN.matcher(ddl);
        while (matcher.find()) {
            blocks.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
        }
        return blocks;
    }
}
