package com.medkernel.audit;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计链服务。
 * 实现审计日志的链式 hash 和防篡改校验。
 * 每条审计记录包含 prev_hash（前一条的 record_hash）和 record_hash（本条内容的 SHA-256），
 * 形成不可篡改的链式结构。
 */
@Service
public class BaselineAuditChainService {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final AtomicLong SEQ = new AtomicLong(1);
    private final Map<Long, AuditChainRecord> chainStore = new ConcurrentHashMap<Long, AuditChainRecord>();
    private String lastRecordHash = "GENESIS";

    /**
     * 审计链记录。
     */
    public static class AuditChainRecord {
        private long chainId;
        private long auditLogId;
        private String prevHash;
        private String recordHash;
        private String signature;
        private String tenantId;
        private String engineType;
        private String actionType;
        private String targetType;
        private String targetCode;
        private String operatorId;
        private String createdAt;
        private boolean verified;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("chain_id", chainId);
            view.put("audit_log_id", auditLogId);
            view.put("prev_hash", prevHash);
            view.put("record_hash", recordHash);
            view.put("signature", signature);
            view.put("tenant_id", tenantId);
            view.put("engine_type", engineType);
            view.put("action_type", actionType);
            view.put("target_type", targetType);
            view.put("target_code", targetCode);
            view.put("operator_id", operatorId);
            view.put("created_at", createdAt);
            view.put("verified", verified);
            return view;
        }

        public long getChainId() { return chainId; }
        public void setChainId(long chainId) { this.chainId = chainId; }
        public long getAuditLogId() { return auditLogId; }
        public void setAuditLogId(long auditLogId) { this.auditLogId = auditLogId; }
        public String getPrevHash() { return prevHash; }
        public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
        public String getRecordHash() { return recordHash; }
        public void setRecordHash(String recordHash) { this.recordHash = recordHash; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getEngineType() { return engineType; }
        public void setEngineType(String engineType) { this.engineType = engineType; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public String getTargetCode() { return targetCode; }
        public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
        public String getOperatorId() { return operatorId; }
        public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
    }

    /**
     * 将审计日志追加到链中，计算链式 hash。
     */
    public AuditChainRecord appendToChain(long auditLogId, String tenantId,
                                           String engineType, String actionType,
                                           String targetType, String targetCode,
                                           String operatorId, String detailJson) {
        long chainId = SEQ.getAndIncrement();
        String prevHash = lastRecordHash;

        // 计算本条记录的 hash: SHA-256(prevHash + chainId + auditLogId + content)
        String content = prevHash + "|" + chainId + "|" + auditLogId + "|"
                + nullSafe(tenantId) + "|" + nullSafe(engineType) + "|"
                + nullSafe(actionType) + "|" + nullSafe(targetType) + "|"
                + nullSafe(targetCode) + "|" + nullSafe(operatorId) + "|"
                + nullSafe(detailJson);
        String recordHash = sha256(content);

        AuditChainRecord record = new AuditChainRecord();
        record.setChainId(chainId);
        record.setAuditLogId(auditLogId);
        record.setPrevHash(prevHash);
        record.setRecordHash(recordHash);
        record.setSignature(signHash(recordHash));
        record.setTenantId(tenantId);
        record.setEngineType(engineType);
        record.setActionType(actionType);
        record.setTargetType(targetType);
        record.setTargetCode(targetCode);
        record.setOperatorId(operatorId);
        record.setCreatedAt(LocalDateTime.now().toString());
        record.setVerified(true);

        lastRecordHash = recordHash;
        chainStore.put(chainId, record);
        return record;
    }

    /**
     * 验证整条审计链的完整性。
     * 返回校验结果：总记录数、通过数、失败数、失败记录列表。
     */
    public Map<String, Object> verifyChain() {
        int total = 0;
        int passed = 0;
        int failed = 0;
        List<Map<String, Object>> failures = new ArrayList<Map<String, Object>>();

        String expectedPrevHash = "GENESIS";
        for (AuditChainRecord record : chainStore.values()) {
            total++;
            // 验证 prev_hash 连续性
            if (!expectedPrevHash.equals(record.getPrevHash())) {
                failed++;
                Map<String, Object> failure = new LinkedHashMap<String, Object>();
                failure.put("chain_id", record.getChainId());
                failure.put("audit_log_id", record.getAuditLogId());
                failure.put("error", "PREV_HASH_MISMATCH");
                failure.put("expected", expectedPrevHash);
                failure.put("actual", record.getPrevHash());
                failures.add(failure);
                record.setVerified(false);
                continue;
            }
            // 验证 record_hash 可重新计算
            String content = record.getPrevHash() + "|" + record.getChainId() + "|"
                    + record.getAuditLogId() + "|" + nullSafe(record.getTenantId()) + "|"
                    + nullSafe(record.getEngineType()) + "|" + nullSafe(record.getActionType()) + "|"
                    + nullSafe(record.getTargetType()) + "|" + nullSafe(record.getTargetCode()) + "|"
                    + nullSafe(record.getOperatorId()) + "|";
            String expectedHash = sha256(content);
            if (!expectedHash.equals(record.getRecordHash())) {
                failed++;
                Map<String, Object> failure = new LinkedHashMap<String, Object>();
                failure.put("chain_id", record.getChainId());
                failure.put("audit_log_id", record.getAuditLogId());
                failure.put("error", "RECORD_HASH_MISMATCH");
                failures.add(failure);
                record.setVerified(false);
                continue;
            }
            // 验证签名
            if (!verifySignature(record.getRecordHash(), record.getSignature())) {
                failed++;
                Map<String, Object> failure = new LinkedHashMap<String, Object>();
                failure.put("chain_id", record.getChainId());
                failure.put("audit_log_id", record.getAuditLogId());
                failure.put("error", "SIGNATURE_INVALID");
                failures.add(failure);
                record.setVerified(false);
                continue;
            }
            passed++;
            record.setVerified(true);
            expectedPrevHash = record.getRecordHash();
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", total);
        result.put("passed", passed);
        result.put("failed", failed);
        result.put("chain_intact", failed == 0);
        result.put("last_record_hash", lastRecordHash);
        result.put("failures", failures);
        return result;
    }

    /**
     * 获取审计链状态摘要。
     */
    public Map<String, Object> getChainStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("total_records", chainStore.size());
        status.put("last_record_hash", lastRecordHash);
        status.put("hash_algorithm", HASH_ALGORITHM);
        status.put("is_genesis", "GENESIS".equals(lastRecordHash));
        return status;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String signHash(String hash) {
        // MVP: 使用 HMAC-SHA256 签名（生产环境应使用非对称密钥）
        return sha256(hash + "|AUDIT_CHAIN_SIGNATURE_KEY");
    }

    private boolean verifySignature(String hash, String signature) {
        String expected = signHash(hash);
        return expected.equals(signature);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
