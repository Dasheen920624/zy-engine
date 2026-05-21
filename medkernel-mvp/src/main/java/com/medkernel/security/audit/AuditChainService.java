package com.medkernel.security.audit;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计链服务：实现审计记录的 hash 计算和链式校验。
 *
 * <p>每条审计记录包含两个 hash 字段：
 * <ul>
 *   <li>{@code record_hash} - 本条记录内容的 SHA-256 hash</li>
 *   <li>{@code chain_hash} - 前一条 chain_hash + 本条 record_hash 的 SHA-256 hash</li>
 * </ul>
 * <p>首条记录的 chain_hash = record_hash。
 * <p>校验时从头重新计算，任何篡改会导致后续所有 hash 不匹配。
 */
@Service
public class AuditChainService {

    private static final Logger log = LoggerFactory.getLogger(AuditChainService.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String INITIAL_CHAIN_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AuditChainService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 计算审计记录的 hash 并更新数据库。
     *
     * @param tableName 审计表名
     * @param recordId  记录 ID
     * @param fields    用于计算 hash 的字段值（按顺序）
     */
    public void computeAndUpdateHash(String tableName, Long recordId, String... fields) {
        try {
            String recordHash = computeRecordHash(fields);
            String previousChainHash = getPreviousChainHash(tableName);
            String chainHash = computeChainHash(previousChainHash, recordHash);

            updateHash(tableName, recordId, recordHash, chainHash);
        } catch (Exception e) {
            log.error("Failed to compute hash for {}.{}", tableName, recordId, e);
        }
    }

    /**
     * 校验审计链完整性。
     *
     * @param tableName 审计表名
     * @return 校验结果
     */
    public AuditChainCheckpoint verifyChain(String tableName) {
        AuditChainCheckpoint checkpoint = new AuditChainCheckpoint();
        checkpoint.setId(Ids.next());
        checkpoint.setCheckpointTime(LocalDateTime.now());
        checkpoint.setChainStatus("IN_PROGRESS");
        checkpoint.setCreatedBy("system");
        checkpoint.setCreatedTime(LocalDateTime.now());

        try {
            List<AuditRecord> records = getAllRecords(tableName);
            checkpoint.setTotalRecords(records.size());

            String expectedChainHash = INITIAL_CHAIN_HASH;
            long validCount = 0;
            Long firstBrokenId = null;

            for (AuditRecord record : records) {
                // 计算本条记录应有的 hash
                String expectedRecordHash = computeRecordHash(record.fields);
                String expectedChain = computeChainHash(expectedChainHash, expectedRecordHash);

                // 比较实际 hash
                boolean recordValid = expectedRecordHash.equals(record.recordHash);
                boolean chainValid = expectedChain.equals(record.chainHash);

                if (recordValid && chainValid) {
                    validCount++;
                    expectedChainHash = record.chainHash;
                } else {
                    if (firstBrokenId == null) {
                        firstBrokenId = record.id;
                        checkpoint.setFirstBrokenId(firstBrokenId);
                        log.warn("Audit chain broken at record {} in table {}: recordValid={}, chainValid={}",
                                record.id, tableName, recordValid, chainValid);
                    }
                }
            }

            checkpoint.setValidRecords(validCount);
            checkpoint.setBrokenRecords(records.size() - validCount);
            checkpoint.setChainStatus(firstBrokenId == null ? "VALID" : "BROKEN");
            checkpoint.setLastCheckedId(records.isEmpty() ? 0L : records.get(records.size() - 1).id);

            // 保存校验点
            saveCheckpoint(checkpoint);

        } catch (Exception e) {
            log.error("Failed to verify audit chain for table {}", tableName, e);
            checkpoint.setChainStatus("ERROR");
            checkpoint.setDetails("{\"error\": \"" + e.getMessage() + "\"}");
        }

        return checkpoint;
    }

    /**
     * 计算记录 hash（SHA-256）。
     */
    public String computeRecordHash(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            sb.append(field != null ? field : "").append("|");
        }
        return sha256(sb.toString());
    }

    /**
     * 计算链式 hash。
     */
    public String computeChainHash(String previousChainHash, String recordHash) {
        return sha256(previousChainHash + recordHash);
    }

    /**
     * 获取初始链 hash。
     */
    public String getInitialChainHash() {
        return INITIAL_CHAIN_HASH;
    }

    // 私有方法

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private String getPreviousChainHash(String tableName) {
        String sql = "SELECT chain_hash FROM " + tableName + " ORDER BY id DESC";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String hash = rs.getString("chain_hash");
                return hash != null ? hash : INITIAL_CHAIN_HASH;
            }
        } catch (SQLException e) {
            log.warn("Failed to get previous chain hash from {}", tableName, e);
        }
        return INITIAL_CHAIN_HASH;
    }

    private void updateHash(String tableName, Long recordId, String recordHash, String chainHash) {
        String sql = "UPDATE " + tableName + " SET record_hash = ?, chain_hash = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, recordHash);
            ps.setString(2, chainHash);
            ps.setLong(3, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update hash for {}.{}", tableName, recordId, e);
        }
    }

    private List<AuditRecord> getAllRecords(String tableName) {
        // 根据表名选择不同的字段
        String sql;
        switch (tableName) {
            case "engine_audit_log":
                sql = "SELECT id, trace_id, engine_type, action_type, target_type, target_code, " +
                      "patient_id, encounter_id, operator_id, tenant_id, group_code, hospital_code, " +
                      "campus_code, site_code, department_code, scope_level, scope_code, org_source, " +
                      "detail_json, created_time, record_hash, chain_hash FROM engine_audit_log ORDER BY id";
                break;
            case "sec_auth_audit_log":
                sql = "SELECT id, tenant_id, user_id, username, event_type, event_result, " +
                      "ip_address, user_agent, failure_reason, trace_id, created_time, " +
                      "record_hash, chain_hash FROM sec_auth_audit_log ORDER BY id";
                break;
            case "sec_sso_audit_log":
                sql = "SELECT id, tenant_id, user_id, config_id, event_type, event_result, " +
                      "external_subject, error_code, error_message, ip_address, user_agent, " +
                      "trace_id, created_time, record_hash, chain_hash FROM sec_sso_audit_log ORDER BY id";
                break;
            default:
                throw new IllegalArgumentException("Unsupported audit table: " + tableName);
        }

        List<AuditRecord> records = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuditRecord record = new AuditRecord();
                record.id = rs.getLong("id");
                record.recordHash = rs.getString("record_hash");
                record.chainHash = rs.getString("chain_hash");

                // 提取用于计算 hash 的字段
                List<String> fields = new ArrayList<>();
                fields.add(String.valueOf(record.id));
                fields.add(rs.getString("tenant_id"));
                fields.add(rs.getString("created_time"));

                // 根据表类型添加特定字段
                if ("engine_audit_log".equals(tableName)) {
                    fields.add(rs.getString("trace_id"));
                    fields.add(rs.getString("engine_type"));
                    fields.add(rs.getString("action_type"));
                    fields.add(rs.getString("target_type"));
                    fields.add(rs.getString("target_code"));
                    fields.add(rs.getString("operator_id"));
                } else if ("sec_auth_audit_log".equals(tableName)) {
                    fields.add(rs.getString("user_id"));
                    fields.add(rs.getString("username"));
                    fields.add(rs.getString("event_type"));
                    fields.add(rs.getString("event_result"));
                    fields.add(rs.getString("ip_address"));
                } else if ("sec_sso_audit_log".equals(tableName)) {
                    fields.add(rs.getString("user_id"));
                    fields.add(rs.getString("event_type"));
                    fields.add(rs.getString("event_result"));
                    fields.add(rs.getString("external_subject"));
                    fields.add(rs.getString("ip_address"));
                }

                record.fields = fields.toArray(new String[0]);
                records.add(record);
            }
        } catch (SQLException e) {
            log.error("Failed to get records from {}", tableName, e);
        }
        return records;
    }

    private void saveCheckpoint(AuditChainCheckpoint checkpoint) {
        String sql = "INSERT INTO sec_audit_chain_checkpoint (id, checkpoint_time, last_checked_id, " +
                     "chain_status, total_records, valid_records, broken_records, first_broken_id, " +
                     "details, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, checkpoint.getId());
            ps.setTimestamp(2, Timestamp.valueOf(checkpoint.getCheckpointTime()));
            ps.setLong(3, checkpoint.getLastCheckedId());
            ps.setString(4, checkpoint.getChainStatus());
            ps.setLong(5, checkpoint.getTotalRecords());
            ps.setLong(6, checkpoint.getValidRecords());
            ps.setLong(7, checkpoint.getBrokenRecords());
            if (checkpoint.getFirstBrokenId() != null) {
                ps.setLong(8, checkpoint.getFirstBrokenId());
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }
            ps.setString(9, checkpoint.getDetails());
            ps.setString(10, checkpoint.getCreatedBy());
            ps.setTimestamp(11, Timestamp.valueOf(checkpoint.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save checkpoint", e);
        }
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    }

    /**
     * 内部类：审计记录。
     */
    private static class AuditRecord {
        Long id;
        String recordHash;
        String chainHash;
        String[] fields;
    }
}
