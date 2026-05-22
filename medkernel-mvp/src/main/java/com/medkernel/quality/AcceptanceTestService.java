package com.medkernel.quality;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AcceptanceTestService {

    private static final Logger log = LoggerFactory.getLogger(AcceptanceTestService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AcceptanceTestService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // =========================================================================
    // 用例管理
    // =========================================================================

    public AcceptanceTestCase createTestCase(AcceptanceTestCase testCase) {
        if (!properties.isEnabled()) {
            return testCase;
        }
        testCase.setId(Ids.next());
        if (testCase.getStatus() == null) {
            testCase.setStatus("DRAFT");
        }
        testCase.setCreatedTime(LocalDateTime.now());

        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO qa_acceptance_test_case "
                    + "(id, tenant_id, case_code, case_name, feature_code, feature_name, category, "
                    + "description, preconditions, steps, expected_result, priority, status, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO qa_acceptance_test_case "
                    + "(id, tenant_id, case_code, case_name, feature_code, feature_name, category, "
                    + "description, preconditions, steps, expected_result, priority, status, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, testCase.getId());
            ps.setObject(i++, testCase.getTenantId());
            ps.setString(i++, testCase.getCaseCode());
            ps.setString(i++, testCase.getCaseName());
            ps.setString(i++, testCase.getFeatureCode());
            ps.setString(i++, testCase.getFeatureName());
            ps.setString(i++, testCase.getCategory());
            ps.setString(i++, testCase.getDescription());
            ps.setString(i++, testCase.getPreconditions());
            ps.setString(i++, testCase.getSteps());
            ps.setString(i++, testCase.getExpectedResult());
            ps.setString(i++, testCase.getPriority());
            ps.setString(i++, testCase.getStatus());
            ps.setString(i++, testCase.getCreatedBy());
            ps.setString(i++, testCase.getUpdatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建验收测试用例失败: " + ex.getMessage(), ex);
        }
        return testCase;
    }

    public AcceptanceTestCase updateTestCase(AcceptanceTestCase testCase) {
        if (!properties.isEnabled()) {
            return testCase;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE qa_acceptance_test_case SET case_name=?, feature_code=?, feature_name=?, "
                    + "category=?, description=?, preconditions=?, steps=?, expected_result=?, "
                    + "priority=?, status=?, updated_by=?, updated_time=CURRENT_TIMESTAMP "
                    + "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE qa_acceptance_test_case SET case_name=?, feature_code=?, feature_name=?, "
                    + "category=?, description=?, preconditions=?, steps=?, expected_result=?, "
                    + "priority=?, status=?, updated_by=?, updated_time=SYSTIMESTAMP "
                    + "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, testCase.getCaseName());
            ps.setString(i++, testCase.getFeatureCode());
            ps.setString(i++, testCase.getFeatureName());
            ps.setString(i++, testCase.getCategory());
            ps.setString(i++, testCase.getDescription());
            ps.setString(i++, testCase.getPreconditions());
            ps.setString(i++, testCase.getSteps());
            ps.setString(i++, testCase.getExpectedResult());
            ps.setString(i++, testCase.getPriority());
            ps.setString(i++, testCase.getStatus());
            ps.setString(i++, testCase.getUpdatedBy());
            ps.setLong(i++, testCase.getId());
            ps.setObject(i++, testCase.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Acceptance test case not found: id=" + testCase.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("更新验收测试用例失败: " + ex.getMessage(), ex);
        }
        return testCase;
    }

    public List<AcceptanceTestCase> listTestCases(Long tenantId, String category, String featureCode, String status) {
        if (!properties.isEnabled()) {
            return new ArrayList<AcceptanceTestCase>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM qa_acceptance_test_case WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (featureCode != null && !featureCode.isEmpty()) {
            sql.append(" AND feature_code = ?");
            params.add(featureCode);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<AcceptanceTestCase> cases = new ArrayList<AcceptanceTestCase>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cases.add(mapTestCase(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询验收测试用例失败: " + ex.getMessage(), ex);
        }
        return cases;
    }

    // =========================================================================
    // 测试执行
    // =========================================================================

    public AcceptanceTestResult recordResult(AcceptanceTestResult result) {
        if (!properties.isEnabled()) {
            return result;
        }
        result.setId(Ids.next());
        if (result.getResultCode() == null) {
            result.setResultCode("AT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (result.getStatus() == null) {
            result.setStatus("EXECUTED");
        }
        result.setExecutedTime(LocalDateTime.now());
        result.setCreatedTime(LocalDateTime.now());

        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO qa_acceptance_test_result "
                    + "(id, tenant_id, result_code, test_case_id, case_code, case_name, feature_code, category, "
                    + "verdict, actual_result, deviation, evidence_refs, environment, "
                    + "executed_by, executed_time, reviewed_by, reviewed_time, review_note, status, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO qa_acceptance_test_result "
                    + "(id, tenant_id, result_code, test_case_id, case_code, case_name, feature_code, category, "
                    + "verdict, actual_result, deviation, evidence_refs, environment, "
                    + "executed_by, executed_time, reviewed_by, reviewed_time, review_note, status, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, result.getId());
            ps.setObject(i++, result.getTenantId());
            ps.setString(i++, result.getResultCode());
            ps.setObject(i++, result.getTestCaseId());
            ps.setString(i++, result.getCaseCode());
            ps.setString(i++, result.getCaseName());
            ps.setString(i++, result.getFeatureCode());
            ps.setString(i++, result.getCategory());
            ps.setString(i++, result.getVerdict());
            ps.setString(i++, result.getActualResult());
            ps.setString(i++, result.getDeviation());
            ps.setString(i++, result.getEvidenceRefs());
            ps.setString(i++, result.getEnvironment());
            ps.setString(i++, result.getExecutedBy());
            ps.setTimestamp(i++, result.getExecutedTime() != null ? Timestamp.valueOf(result.getExecutedTime()) : null);
            ps.setString(i++, result.getReviewedBy());
            ps.setTimestamp(i++, result.getReviewedTime() != null ? Timestamp.valueOf(result.getReviewedTime()) : null);
            ps.setString(i++, result.getReviewNote());
            ps.setString(i++, result.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("记录验收测试结果失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    public List<AcceptanceTestResult> listResults(Long tenantId, String caseCode, String verdict, String status) {
        if (!properties.isEnabled()) {
            return new ArrayList<AcceptanceTestResult>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM qa_acceptance_test_result WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (caseCode != null && !caseCode.isEmpty()) {
            sql.append(" AND case_code = ?");
            params.add(caseCode);
        }
        if (verdict != null && !verdict.isEmpty()) {
            sql.append(" AND verdict = ?");
            params.add(verdict);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<AcceptanceTestResult> results = new ArrayList<AcceptanceTestResult>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapTestResult(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询验收测试结果失败: " + ex.getMessage(), ex);
        }
        return results;
    }

    public AcceptanceTestResult reviewResult(Long resultId, String reviewedBy, String reviewNote, String status) {
        if (!properties.isEnabled()) {
            AcceptanceTestResult result = new AcceptanceTestResult();
            result.setId(resultId);
            result.setReviewedBy(reviewedBy);
            result.setReviewNote(reviewNote);
            result.setStatus(status);
            return result;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE qa_acceptance_test_result SET reviewed_by=?, review_note=?, status=?, "
                    + "reviewed_time=CURRENT_TIMESTAMP WHERE id=?";
        } else {
            sql = "UPDATE qa_acceptance_test_result SET reviewed_by=?, review_note=?, status=?, "
                    + "reviewed_time=SYSTIMESTAMP WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, reviewedBy);
            ps.setString(i++, reviewNote);
            ps.setString(i++, status);
            ps.setLong(i++, resultId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Acceptance test result not found: id=" + resultId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("审核验收测试结果失败: " + ex.getMessage(), ex);
        }
        return getResult(resultId);
    }

    // =========================================================================
    // 证据管理
    // =========================================================================

    public AcceptanceEvidence attachEvidence(AcceptanceEvidence evidence) {
        if (!properties.isEnabled()) {
            return evidence;
        }
        evidence.setId(Ids.next());
        if (evidence.getEvidenceCode() == null) {
            evidence.setEvidenceCode("EV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        evidence.setCreatedTime(LocalDateTime.now());

        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO qa_acceptance_evidence "
                    + "(id, tenant_id, evidence_code, result_code, case_code, evidence_type, "
                    + "description, file_path, file_hash, file_size, mime_type, "
                    + "captured_by, captured_time, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO qa_acceptance_evidence "
                    + "(id, tenant_id, evidence_code, result_code, case_code, evidence_type, "
                    + "description, file_path, file_hash, file_size, mime_type, "
                    + "captured_by, captured_time, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, evidence.getId());
            ps.setObject(i++, evidence.getTenantId());
            ps.setString(i++, evidence.getEvidenceCode());
            ps.setString(i++, evidence.getResultCode());
            ps.setString(i++, evidence.getCaseCode());
            ps.setString(i++, evidence.getEvidenceType());
            ps.setString(i++, evidence.getDescription());
            ps.setString(i++, evidence.getFilePath());
            ps.setString(i++, evidence.getFileHash());
            ps.setLong(i++, evidence.getFileSize());
            ps.setString(i++, evidence.getMimeType());
            ps.setString(i++, evidence.getCapturedBy());
            ps.setTimestamp(i++, evidence.getCapturedTime() != null ? Timestamp.valueOf(evidence.getCapturedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("附加验收证据失败: " + ex.getMessage(), ex);
        }
        return evidence;
    }

    public List<AcceptanceEvidence> listEvidence(Long tenantId, String resultCode) {
        if (!properties.isEnabled()) {
            return new ArrayList<AcceptanceEvidence>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM qa_acceptance_evidence WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (resultCode != null && !resultCode.isEmpty()) {
            sql.append(" AND result_code = ?");
            params.add(resultCode);
        }
        sql.append(" ORDER BY created_time DESC");

        List<AcceptanceEvidence> evidences = new ArrayList<AcceptanceEvidence>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evidences.add(mapEvidence(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询验收证据失败: " + ex.getMessage(), ex);
        }
        return evidences;
    }

    // =========================================================================
    // 报告
    // =========================================================================

    public Map<String, Object> getAcceptanceSummary(Long tenantId, String featureCode) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);

        if (!properties.isEnabled()) {
            summary.put("totalCases", 0);
            summary.put("totalResults", 0);
            summary.put("verdictDistribution", new LinkedHashMap<String, Integer>());
            summary.put("statusDistribution", new LinkedHashMap<String, Integer>());
            return summary;
        }

        // 用例统计
        String caseSql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END) AS active_count "
                + "FROM qa_acceptance_test_case WHERE tenant_id=?";
        int totalCases = 0;
        int activeCases = 0;
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(caseSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalCases = rs.getInt("total");
                    activeCases = rs.getInt("active_count");
                }
            }
        } catch (SQLException ex) {
            log.error("获取验收用例统计失败", ex);
        }
        summary.put("totalCases", totalCases);
        summary.put("activeCases", activeCases);

        // 结果统计
        StringBuilder resultSqlBuilder = new StringBuilder(
                "SELECT verdict, status, COUNT(*) AS cnt FROM qa_acceptance_test_result WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (featureCode != null && !featureCode.isEmpty()) {
            resultSqlBuilder.append(" AND feature_code = ?");
            params.add(featureCode);
        }
        resultSqlBuilder.append(" GROUP BY verdict, status");

        Map<String, Integer> verdictDist = new LinkedHashMap<String, Integer>();
        verdictDist.put("PASS", 0);
        verdictDist.put("FAIL", 0);
        verdictDist.put("BLOCKED", 0);
        verdictDist.put("SKIP", 0);
        Map<String, Integer> statusDist = new LinkedHashMap<String, Integer>();
        statusDist.put("EXECUTED", 0);
        statusDist.put("REVIEWED", 0);
        statusDist.put("ACCEPTED", 0);
        statusDist.put("REJECTED", 0);
        int totalResults = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(resultSqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String verdict = rs.getString("verdict");
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    totalResults += cnt;
                    if (verdict != null && verdictDist.containsKey(verdict)) {
                        verdictDist.put(verdict, verdictDist.get(verdict) + cnt);
                    }
                    if (status != null && statusDist.containsKey(status)) {
                        statusDist.put(status, statusDist.get(status) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("获取验收结果统计失败", ex);
        }

        summary.put("totalResults", totalResults);
        summary.put("verdictDistribution", verdictDist);
        summary.put("statusDistribution", statusDist);
        if (featureCode != null) {
            summary.put("featureCode", featureCode);
        }
        return summary;
    }

    public Map<String, Object> generateAcceptanceReport(Long tenantId, String featureCode) {
        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("tenantId", tenantId);
        report.put("featureCode", featureCode);
        report.put("generatedTime", LocalDateTime.now().toString());

        if (!properties.isEnabled()) {
            report.put("summary", getAcceptanceSummary(tenantId, featureCode));
            report.put("cases", new ArrayList<AcceptanceTestCase>());
            report.put("results", new ArrayList<AcceptanceTestResult>());
            report.put("evidences", new ArrayList<AcceptanceEvidence>());
            return report;
        }

        // 摘要
        report.put("summary", getAcceptanceSummary(tenantId, featureCode));

        // 用例列表
        List<AcceptanceTestCase> cases;
        if (featureCode != null && !featureCode.isEmpty()) {
            cases = listTestCases(tenantId, null, featureCode, null);
        } else {
            cases = listTestCases(tenantId, null, null, null);
        }
        report.put("totalCases", cases.size());

        // 结果列表
        List<AcceptanceTestResult> results;
        if (featureCode != null && !featureCode.isEmpty()) {
            results = listResults(tenantId, null, null, null);
            // 过滤 featureCode
            List<AcceptanceTestResult> filtered = new ArrayList<AcceptanceTestResult>();
            for (AcceptanceTestResult r : results) {
                if (featureCode.equals(r.getFeatureCode())) {
                    filtered.add(r);
                }
            }
            results = filtered;
        } else {
            results = listResults(tenantId, null, null, null);
        }
        report.put("totalResults", results.size());

        // 证据列表
        List<AcceptanceEvidence> evidences = new ArrayList<AcceptanceEvidence>();
        for (AcceptanceTestResult r : results) {
            List<AcceptanceEvidence> evList = listEvidence(tenantId, r.getResultCode());
            evidences.addAll(evList);
        }
        report.put("totalEvidences", evidences.size());

        // 通过率
        long passCount = 0;
        long failCount = 0;
        for (AcceptanceTestResult r : results) {
            if ("PASS".equals(r.getVerdict())) {
                passCount++;
            } else if ("FAIL".equals(r.getVerdict())) {
                failCount++;
            }
        }
        double passRate = results.isEmpty() ? 0 : (double) passCount / results.size() * 100;
        report.put("passCount", passCount);
        report.put("failCount", failCount);
        report.put("passRate", Math.round(passRate * 100.0) / 100.0);

        return report;
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private AcceptanceTestResult getResult(Long resultId) {
        String sql = "SELECT * FROM qa_acceptance_test_result WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, resultId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTestResult(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询验收测试结果失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private AcceptanceTestCase mapTestCase(ResultSet rs) throws SQLException {
        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        testCase.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        testCase.setCaseCode(rs.getString("case_code"));
        testCase.setCaseName(rs.getString("case_name"));
        testCase.setFeatureCode(rs.getString("feature_code"));
        testCase.setFeatureName(rs.getString("feature_name"));
        testCase.setCategory(rs.getString("category"));
        testCase.setDescription(rs.getString("description"));
        testCase.setPreconditions(rs.getString("preconditions"));
        testCase.setSteps(rs.getString("steps"));
        testCase.setExpectedResult(rs.getString("expected_result"));
        testCase.setPriority(rs.getString("priority"));
        testCase.setStatus(rs.getString("status"));
        testCase.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        testCase.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        testCase.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        testCase.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return testCase;
    }

    private AcceptanceTestResult mapTestResult(ResultSet rs) throws SQLException {
        AcceptanceTestResult result = new AcceptanceTestResult();
        result.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        result.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        result.setResultCode(rs.getString("result_code"));
        Object testCaseIdObj = rs.getObject("test_case_id");
        result.setTestCaseId(testCaseIdObj instanceof Long ? (Long) testCaseIdObj : null);
        result.setCaseCode(rs.getString("case_code"));
        result.setCaseName(rs.getString("case_name"));
        result.setFeatureCode(rs.getString("feature_code"));
        result.setCategory(rs.getString("category"));
        result.setVerdict(rs.getString("verdict"));
        result.setActualResult(rs.getString("actual_result"));
        result.setDeviation(rs.getString("deviation"));
        result.setEvidenceRefs(rs.getString("evidence_refs"));
        result.setEnvironment(rs.getString("environment"));
        result.setExecutedBy(rs.getString("executed_by"));
        Timestamp executedTime = rs.getTimestamp("executed_time");
        result.setExecutedTime(executedTime != null ? executedTime.toLocalDateTime() : null);
        result.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        result.setReviewedTime(reviewedTime != null ? reviewedTime.toLocalDateTime() : null);
        result.setReviewNote(rs.getString("review_note"));
        result.setStatus(rs.getString("status"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        result.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return result;
    }

    private AcceptanceEvidence mapEvidence(ResultSet rs) throws SQLException {
        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        evidence.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        evidence.setEvidenceCode(rs.getString("evidence_code"));
        evidence.setResultCode(rs.getString("result_code"));
        evidence.setCaseCode(rs.getString("case_code"));
        evidence.setEvidenceType(rs.getString("evidence_type"));
        evidence.setDescription(rs.getString("description"));
        evidence.setFilePath(rs.getString("file_path"));
        evidence.setFileHash(rs.getString("file_hash"));
        evidence.setFileSize(rs.getLong("file_size"));
        evidence.setMimeType(rs.getString("mime_type"));
        evidence.setCapturedBy(rs.getString("captured_by"));
        Timestamp capturedTime = rs.getTimestamp("captured_time");
        evidence.setCapturedTime(capturedTime != null ? capturedTime.toLocalDateTime() : null);
        Timestamp createdTime = rs.getTimestamp("created_time");
        evidence.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return evidence;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
