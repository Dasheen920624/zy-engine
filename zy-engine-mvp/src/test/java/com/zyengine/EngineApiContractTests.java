package com.zyengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class EngineApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        Map<String, Object> result = invokeGet("/api/health");
        assertEquals(Boolean.TRUE, result.get("success"));
        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("UP", data.get("status"));
        assertEquals("zy-engine-mvp", data.get("service"));
    }

    @Test
    void ruleExecLogSummaryAggregates() throws Exception {
        // 导入并发布两条规则（一条总命中，一条总不命中），多次模拟后调用 summary 接口
        Map<String, Object> hitRule = ruleDefinition("R_SUM_HIT", "命中规则");
        Map<String, Object> missRule = ruleDefinition("R_SUM_MISS", "不命中规则");
        Map<String, Object> chiefComplaint = new LinkedHashMap<String, Object>();
        chiefComplaint.put("fact", "chief_complaints.code");
        chiefComplaint.put("operator", "in");
        chiefComplaint.put("value", Arrays.asList("NEVER_MATCH"));
        Map<String, Object> missCondition = new LinkedHashMap<String, Object>();
        missCondition.put("all", Arrays.asList(chiefComplaint));
        missRule.put("condition", missCondition);

        invokePost("/api/rules", Arrays.asList(hitRule, missRule));
        invokePost("/api/rules/R_SUM_HIT/publish", new LinkedHashMap<String, Object>());
        invokePost("/api/rules/R_SUM_MISS/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> simulateHit = new LinkedHashMap<String, Object>();
        simulateHit.put("rule_code", "R_SUM_HIT");
        simulateHit.put("patient_context", samplePatientContext());
        invokePost("/api/rules/simulate", simulateHit);
        invokePost("/api/rules/simulate", simulateHit);

        Map<String, Object> simulateMiss = new LinkedHashMap<String, Object>();
        simulateMiss.put("rule_code", "R_SUM_MISS");
        simulateMiss.put("patient_context", samplePatientContext());
        invokePost("/api/rules/simulate", simulateMiss);

        Map<String, Object> hitSummary = invokeGet("/api/rules/exec-logs/summary?ruleCode=R_SUM_HIT");
        Map<String, Object> hitData = asMap(hitSummary.get("data"));
        assertEquals(2, ((Number) hitData.get("total")).intValue());
        assertEquals(2, ((Number) hitData.get("total_hits")).intValue());
        assertEquals(100.0, ((Number) hitData.get("hit_rate")).doubleValue());
        List<Map<String, Object>> byRule = asListOfMap(hitData.get("by_rule"));
        assertEquals(1, byRule.size());
        assertEquals("R_SUM_HIT", byRule.get(0).get("rule_code"));
        assertEquals(2, ((Number) byRule.get(0).get("hits")).intValue());

        Map<String, Object> missSummary = invokeGet("/api/rules/exec-logs/summary?ruleCode=R_SUM_MISS");
        Map<String, Object> missData = asMap(missSummary.get("data"));
        assertEquals(1, ((Number) missData.get("total")).intValue());
        assertEquals(0, ((Number) missData.get("total_hits")).intValue());
        assertEquals(0.0, ((Number) missData.get("hit_rate")).doubleValue());
    }

    @Test
    void ruleImportPublishSimulateAndQueryLogs() throws Exception {
        Map<String, Object> rulePayload = ruleDefinition("R_TEST_STEMI", "AMI/STEMI候选入径测试规则");
        List<Map<String, Object>> rules = Arrays.asList(rulePayload);
        Map<String, Object> imported = invokePost("/api/rules", rules);
        assertEquals(Boolean.TRUE, imported.get("success"));
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("version_no", "1.0.0");
        publishBody.put("approved_by", "JUNIT");
        Map<String, Object> published = invokePost("/api/rules/R_TEST_STEMI/publish", publishBody);
        assertEquals("PUBLISHED", asMap(published.get("data")).get("status"));

        Map<String, Object> simulateBody = new LinkedHashMap<String, Object>();
        simulateBody.put("rule_code", "R_TEST_STEMI");
        simulateBody.put("version_no", "1.0.0");
        simulateBody.put("patient_context", samplePatientContext());
        Map<String, Object> simulate = invokePost("/api/rules/simulate", simulateBody);
        Map<String, Object> simulateData = asMap(simulate.get("data"));
        assertEquals("R_TEST_STEMI", simulateData.get("ruleCode"));
        assertEquals(Boolean.TRUE, simulateData.get("hit"));

        Map<String, Object> logs = invokeGet("/api/rules/exec-logs?ruleCode=R_TEST_STEMI&hit=true&limit=10");
        List<Map<String, Object>> entries = asListOfMap(logs.get("data"));
        assertFalse(entries.isEmpty(), "exec-logs should contain at least one entry");
        Map<String, Object> firstEntry = entries.get(0);
        assertEquals("R_TEST_STEMI", firstEntry.get("ruleCode"));
        assertEquals(Boolean.TRUE, firstEntry.get("hit"));
        assertNotNull(firstEntry.get("logId"));

        Map<String, Object> detail = invokeGet("/api/rules/exec-logs/" + firstEntry.get("logId"));
        assertEquals(firstEntry.get("logId"), asMap(detail.get("data")).get("logId"));
    }

    @Test
    void pathwayImportPublishAdmitAndComplete() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_TEST", "AMI测试路径");
        Map<String, Object> created = invokePost("/api/pathways", pathwayConfig);
        assertEquals("DRAFT", asMap(created.get("data")).get("status"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("version_no", "1.0.0");
        Map<String, Object> published = invokePost("/api/pathways/AMI_TEST/publish", publishBody);
        assertEquals("PUBLISHED", asMap(published.get("data")).get("status"));

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_JUNIT_001");
        admitBody.put("encounter_id", "E_JUNIT_001");
        admitBody.put("pathway_code", "AMI_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "JUNIT_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        Map<String, Object> admittedData = asMap(admitted.get("data"));
        assertEquals("ACTIVE", admittedData.get("status"));
        assertEquals("NODE_IDENTIFY", admittedData.get("currentNodeCode"));
        String instanceId = (String) admittedData.get("instanceId");
        assertNotNull(instanceId);

        Map<String, Object> completeNodeBody = new LinkedHashMap<String, Object>();
        completeNodeBody.put("operator_id", "JUNIT_DOC");
        Map<String, Object> completed = invokePost("/api/patient-pathways/" + instanceId + "/nodes/NODE_IDENTIFY/complete",
                completeNodeBody);
        assertEquals("NODE_TREATMENT", asMap(completed.get("data")).get("currentNodeCode"));
    }

    @Test
    void pathwayImportRejectsInvalidConfig() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<String, Object>();
        bad.put("pathway_code", "AMI_BAD");
        // pathway_name 和 stages 全部缺失，必须返回 VALIDATION_ERROR
        Map<String, Object> response = invokePostExpectingClientError("/api/pathways", bad);
        assertEquals(Boolean.FALSE, response.get("success"));
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void terminologyImportListAndNormalizeBuiltIn() throws Exception {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("source_system", "HIS");
        entry.put("source_code", "I50.0");
        entry.put("source_name", "心力衰竭");
        entry.put("concept_type", "DIAGNOSIS");
        entry.put("standard_code", "HEART_FAILURE");
        entry.put("standard_name", "心力衰竭");
        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("mappings", Arrays.asList(entry));
        Map<String, Object> imported = invokePost("/api/terminology/mappings", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> getResp = invokeGet("/api/terminology/mappings/HIS/I50.0?conceptType=DIAGNOSIS");
        assertEquals("HEART_FAILURE", asMap(getResp.get("data")).get("standard_code"));

        Map<String, Object> normalizeBody = new LinkedHashMap<String, Object>();
        normalizeBody.put("source_system", "HIS");
        normalizeBody.put("source_code", "I21.3");
        normalizeBody.put("source_name", "急性ST段抬高型心肌梗死");
        normalizeBody.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalized = invokePost("/api/terminology/normalize", normalizeBody);
        Map<String, Object> normalizedData = asMap(normalized.get("data"));
        assertEquals(Boolean.TRUE, normalizedData.get("matched"));
        assertEquals("AMI_STEMI", normalizedData.get("standard_code"));
    }

    @Test
    void terminologyImportRejectsInvalidMapping() throws Exception {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("source_system", "HIS");
        entry.put("concept_type", "DIAGNOSIS");
        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("mappings", Arrays.asList(entry));
        Map<String, Object> response = invokePostExpectingClientError("/api/terminology/mappings", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void adapterImportAndQuery() throws Exception {
        Map<String, Object> sampleRow = new LinkedHashMap<String, Object>();
        sampleRow.put("exam_code", "CT_CHEST");
        sampleRow.put("report_text", "未见明显异常。");
        sampleRow.put("report_time", "2026-05-16T08:00:00+08:00");

        Map<String, Object> definition = new LinkedHashMap<String, Object>();
        definition.put("adapter_code", "RIS_TEST_ADAPTER");
        definition.put("adapter_name", "RIS测试适配器");
        definition.put("adapter_type", "REST");
        definition.put("source_system", "RIS");
        definition.put("query_code", "QUERY_CHEST_CT");
        definition.put("query_name", "查询胸部CT报告");
        definition.put("description", "JUnit 测试用胸部CT Mock。");
        definition.put("schema", Arrays.asList("patient_id", "encounter_id", "exam_code", "report_text"));
        definition.put("sample_rows", Arrays.asList(sampleRow));

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("definitions", Arrays.asList(definition));
        Map<String, Object> imported = invokePost("/api/adapters/definitions", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> queryBody = new LinkedHashMap<String, Object>();
        queryBody.put("adapter_code", "RIS_TEST_ADAPTER");
        queryBody.put("query_code", "QUERY_CHEST_CT");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("patient_id", "P_JUNIT_001");
        params.put("encounter_id", "E_JUNIT_001");
        queryBody.put("params", params);
        Map<String, Object> queryResult = invokePost("/api/adapters/query", queryBody);
        Map<String, Object> data = asMap(queryResult.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals(1, ((Number) data.get("row_count")).intValue());
        List<Map<String, Object>> rows = asListOfMap(data.get("rows"));
        assertEquals("CT_CHEST", rows.get(0).get("exam_code"));
        assertEquals("P_JUNIT_001", rows.get(0).get("patient_id"));
    }

    @Test
    void pathwayVersionDiffBetweenDraftAndPublished() throws Exception {
        // 先发布 1.0.0，再修改草稿（增加一个节点 + 改主节点名 + 增减任务），通过 /diff 比较 1.0.0 vs draft
        Map<String, Object> v1 = samplePathwayConfig("AMI_DIFF_TEST", "AMI差异对比初版");
        invokePost("/api/pathways", v1);
        invokePost("/api/pathways/AMI_DIFF_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> v2 = samplePathwayConfig("AMI_DIFF_TEST", "AMI差异对比改版");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) v2.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");
        // 修改 NODE_IDENTIFY 名称 + 删除 TASK_TRIAGE + 增加 TASK_ECG
        Map<String, Object> identify = nodes.get(0);
        identify.put("node_name", "识别（升级版）");
        Map<String, Object> ecgTask = new LinkedHashMap<String, Object>();
        ecgTask.put("task_code", "TASK_ECG");
        ecgTask.put("task_name", "心电图采集");
        ecgTask.put("task_type", "EXAM");
        ecgTask.put("required", true);
        identify.put("tasks", Arrays.asList(ecgTask));
        // 增加一个全新节点 NODE_FOLLOWUP
        Map<String, Object> followup = new LinkedHashMap<String, Object>();
        followup.put("node_code", "NODE_FOLLOWUP");
        followup.put("node_name", "随访");
        List<Map<String, Object>> mutableNodes = new java.util.ArrayList<Map<String, Object>>(nodes);
        mutableNodes.add(followup);
        stages.get(0).put("nodes", mutableNodes);
        invokePost("/api/pathways", v2);

        Map<String, Object> diffResp = invokeGet("/api/pathways/AMI_DIFF_TEST/diff?from=1.0.0&to=draft");
        Map<String, Object> diff = asMap(diffResp.get("data"));
        assertEquals("AMI_DIFF_TEST", diff.get("pathway_code"));

        Map<String, Object> summary = asMap(diff.get("summary"));
        assertEquals(1, ((Number) summary.get("metadata_changed")).intValue());
        assertEquals(1, ((Number) summary.get("nodes_added")).intValue());
        assertEquals(0, ((Number) summary.get("nodes_removed")).intValue());
        assertEquals(1, ((Number) summary.get("nodes_modified")).intValue());

        List<Map<String, Object>> nodesAdded = asListOfMap(diff.get("nodes_added"));
        // nodes_added 是 String 列表，asListOfMap 不适用；直接取 List<Object>
        List<Object> addedList = asList(diff.get("nodes_added"));
        assertEquals(1, addedList.size());
        assertEquals("NODE_FOLLOWUP", addedList.get(0));

        List<Map<String, Object>> nodesModified = asListOfMap(diff.get("nodes_modified"));
        assertEquals(1, nodesModified.size());
        Map<String, Object> identifyDiff = nodesModified.get(0);
        assertEquals("NODE_IDENTIFY", identifyDiff.get("node_code"));
        List<Map<String, Object>> fieldChanges = asListOfMap(identifyDiff.get("fields"));
        assertEquals(1, fieldChanges.size());
        assertEquals("node_name", fieldChanges.get(0).get("field"));
        List<Object> tasksAdded = asList(identifyDiff.get("tasks_added"));
        List<Object> tasksRemoved = asList(identifyDiff.get("tasks_removed"));
        assertTrue(tasksAdded.contains("TASK_ECG"));
        assertTrue(tasksRemoved.contains("TASK_TRIAGE"));
    }

    @Test
    void pathwayNodeCompletionMetrics() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_NODE_TEST", "AMI节点完成率测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_NODE_TEST/publish", new LinkedHashMap<String, Object>());

        // 入径 P_NODE_001 并完成首节点（含 TASK_TRIAGE）— 进入 NODE_IDENTIFY → COMPLETED → 自动进入 NODE_TREATMENT
        Map<String, Object> admit1 = new LinkedHashMap<String, Object>();
        admit1.put("patient_id", "P_NODE_001");
        admit1.put("encounter_id", "E_NODE_001");
        admit1.put("pathway_code", "AMI_NODE_TEST");
        admit1.put("version_no", "1.0.0");
        admit1.put("doctor_id", "NODE_DOC");
        Map<String, Object> admitted1 = invokePost("/api/patient-pathways/admit", admit1);
        String instance1 = (String) asMap(admitted1.get("data")).get("instanceId");

        Map<String, Object> completeTask = new LinkedHashMap<String, Object>();
        completeTask.put("operator_id", "NODE_DOC");
        invokePost("/api/patient-pathways/" + instance1 + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/complete", completeTask);
        Map<String, Object> completeNode = new LinkedHashMap<String, Object>();
        completeNode.put("operator_id", "NODE_DOC");
        invokePost("/api/patient-pathways/" + instance1 + "/nodes/NODE_IDENTIFY/complete", completeNode);

        // 入径 P_NODE_002 但只跳过 TASK_TRIAGE，不完成节点 — NODE_IDENTIFY 保持 RUNNING
        Map<String, Object> admit2 = new LinkedHashMap<String, Object>();
        admit2.put("patient_id", "P_NODE_002");
        admit2.put("encounter_id", "E_NODE_002");
        admit2.put("pathway_code", "AMI_NODE_TEST");
        admit2.put("version_no", "1.0.0");
        admit2.put("doctor_id", "NODE_DOC");
        Map<String, Object> admitted2 = invokePost("/api/patient-pathways/admit", admit2);
        String instance2 = (String) asMap(admitted2.get("data")).get("instanceId");
        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "NODE_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "节点完成率测试：跳过分诊。");
        invokePost("/api/patient-pathways/" + instance2 + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> resp = invokeGet("/api/pathway-instances/node-completion?pathwayCode=AMI_NODE_TEST");
        Map<String, Object> data = asMap(resp.get("data"));
        assertEquals(2, ((Number) data.get("total_instances")).intValue());
        assertEquals(2, ((Number) data.get("total_nodes")).intValue());

        List<Map<String, Object>> nodes = asListOfMap(data.get("nodes"));
        Map<String, Map<String, Object>> byNode = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> node : nodes) {
            byNode.put((String) node.get("node_code"), node);
        }

        Map<String, Object> identify = byNode.get("NODE_IDENTIFY");
        assertEquals(2, ((Number) identify.get("entered")).intValue());
        assertEquals(1, ((Number) identify.get("completed")).intValue());
        assertEquals(1, ((Number) identify.get("running")).intValue());
        assertEquals(50.0, ((Number) identify.get("completion_rate")).doubleValue());

        List<Map<String, Object>> tasks = asListOfMap(identify.get("tasks"));
        Map<String, Map<String, Object>> byTask = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> task : tasks) {
            byTask.put((String) task.get("task_code"), task);
        }
        Map<String, Object> triage = byTask.get("TASK_TRIAGE");
        assertEquals(2, ((Number) triage.get("total")).intValue());
        assertEquals(1, ((Number) triage.get("completed")).intValue());
        assertEquals(1, ((Number) triage.get("skipped")).intValue());
        assertEquals(50.0, ((Number) triage.get("completion_rate")).doubleValue());

        Map<String, Object> treatment = byNode.get("NODE_TREATMENT");
        assertEquals(1, ((Number) treatment.get("entered")).intValue());
        // 治疗节点没有任务，instance1 进入后保持 RUNNING（completeNode 自动进入下一节点但未完成）
        assertEquals(0, ((Number) treatment.get("completed")).intValue());
    }

    @Test
    void pathwayInstancesListAndSummary() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_INST_TEST", "AMI实例统计测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_INST_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_INST_001");
        admitBody.put("encounter_id", "E_INST_001");
        admitBody.put("pathway_code", "AMI_INST_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "INST_DOC");
        invokePost("/api/patient-pathways/admit", admitBody);

        Map<String, Object> admit2 = new LinkedHashMap<String, Object>();
        admit2.put("patient_id", "P_INST_002");
        admit2.put("encounter_id", "E_INST_002");
        admit2.put("pathway_code", "AMI_INST_TEST");
        admit2.put("version_no", "1.0.0");
        admit2.put("doctor_id", "INST_DOC");
        Map<String, Object> second = invokePost("/api/patient-pathways/admit", admit2);
        String secondInstanceId = (String) asMap(second.get("data")).get("instanceId");

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "INST_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "实例统计测试：跳过分诊任务。");
        invokePost("/api/patient-pathways/" + secondInstanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> listResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_INST_TEST&limit=10");
        List<Map<String, Object>> instances = asListOfMap(listResp.get("data"));
        assertEquals(2, instances.size());
        for (Map<String, Object> instance : instances) {
            assertEquals("AMI_INST_TEST", instance.get("pathwayCode"));
            assertEquals("ACTIVE", instance.get("status"));
        }

        Map<String, Object> filteredResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_INST_TEST&patientId=P_INST_001");
        assertEquals(1, asListOfMap(filteredResp.get("data")).size());

        Map<String, Object> summaryResp = invokeGet("/api/pathway-instances/summary?pathwayCode=AMI_INST_TEST");
        Map<String, Object> summary = asMap(summaryResp.get("data"));
        assertEquals(2, ((Number) summary.get("total")).intValue());
        List<Map<String, Object>> byStatus = asListOfMap(summary.get("by_status"));
        assertEquals(1, byStatus.size());
        assertEquals("ACTIVE", byStatus.get(0).get("status"));
        assertEquals(2, ((Number) byStatus.get(0).get("count")).intValue());
        assertEquals(1, ((Number) summary.get("variation_total")).intValue());
        List<Map<String, Object>> variationByType = asListOfMap(summary.get("variation_by_type"));
        assertEquals(1, variationByType.size());
        assertEquals("PATIENT_REASON", variationByType.get(0).get("variation_type"));
    }

    @Test
    void pathwayVariationsListAndSummary() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_VAR_TEST", "AMI变异聚合测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_VAR_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_VAR_001");
        admitBody.put("encounter_id", "E_VAR_001");
        admitBody.put("pathway_code", "AMI_VAR_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "VAR_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        String instanceId = (String) asMap(admitted.get("data")).get("instanceId");

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "VAR_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "患者拒绝完成分诊任务。");
        invokePost("/api/patient-pathways/" + instanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> manualVariation = new LinkedHashMap<String, Object>();
        manualVariation.put("node_code", "NODE_IDENTIFY");
        manualVariation.put("variation_type", "RESOURCE_LIMIT");
        manualVariation.put("reason", "导管室资源等待。");
        manualVariation.put("operator_id", "VAR_DOC");
        invokePost("/api/patient-pathways/" + instanceId + "/variations", manualVariation);

        Map<String, Object> listResp = invokeGet("/api/pathway-variations?pathwayCode=AMI_VAR_TEST&limit=20");
        List<Map<String, Object>> records = asListOfMap(listResp.get("data"));
        assertEquals(2, records.size(), "expected 2 variations for AMI_VAR_TEST");
        for (Map<String, Object> record : records) {
            assertEquals("AMI_VAR_TEST", record.get("pathwayCode"));
            assertEquals(instanceId, record.get("instanceId"));
        }

        Map<String, Object> filteredResp = invokeGet("/api/pathway-variations?pathwayCode=AMI_VAR_TEST&variationType=RESOURCE_LIMIT");
        List<Map<String, Object>> filtered = asListOfMap(filteredResp.get("data"));
        assertEquals(1, filtered.size());
        assertEquals("RESOURCE_LIMIT", filtered.get(0).get("variationType"));

        Map<String, Object> summaryResp = invokeGet("/api/pathway-variations/summary?pathwayCode=AMI_VAR_TEST");
        Map<String, Object> summary = asMap(summaryResp.get("data"));
        assertEquals(2, ((Number) summary.get("total")).intValue());
        List<Map<String, Object>> byType = asListOfMap(summary.get("by_variation_type"));
        assertEquals(2, byType.size());
        Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> bucket : byType) {
            typeCounts.put((String) bucket.get("variation_type"), ((Number) bucket.get("count")).intValue());
        }
        assertEquals(Integer.valueOf(1), typeCounts.get("PATIENT_REASON"));
        assertEquals(Integer.valueOf(1), typeCounts.get("RESOURCE_LIMIT"));
        List<Map<String, Object>> byPathway = asListOfMap(summary.get("by_pathway_code"));
        assertEquals(1, byPathway.size());
        assertEquals("AMI_VAR_TEST", byPathway.get(0).get("pathway_code"));
    }

    @Test
    void difyWorkflowTemplateImportAndDegradedRun() throws Exception {
        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("workflow_code", "WF_JUNIT_EXPLAIN");
        template.put("workflow_name", "JUnit 解释工作流");
        template.put("workflow_version", "1.0.0");
        template.put("description", "JUnit 用例用的 Dify 工作流模板。");
        template.put("required_inputs", Arrays.asList("patient_id", "target_code"));
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("scenario", "PATHWAY_ENTRY");
        defaults.put("language", "zh-CN");
        template.put("input_defaults", defaults);
        Map<String, Object> degraded = new LinkedHashMap<String, Object>();
        degraded.put("explanation", "JUnit 模板降级输出。");
        degraded.put("recommended_action", "由医生确认。");
        template.put("degraded_outputs", degraded);

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("templates", Arrays.asList(template));
        Map<String, Object> imported = invokePost("/api/dify/workflows", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> listResp = invokeGet("/api/dify/workflows");
        List<Map<String, Object>> templates = asListOfMap(listResp.get("data"));
        assertFalse(templates.isEmpty());

        Map<String, Object> getResp = invokeGet("/api/dify/workflows/WF_JUNIT_EXPLAIN");
        assertEquals("WF_JUNIT_EXPLAIN", asMap(getResp.get("data")).get("workflowCode"));

        // 模板要求 patient_id 与 target_code，缺失任一应返回 VALIDATION_ERROR
        Map<String, Object> missingRun = new LinkedHashMap<String, Object>();
        missingRun.put("workflow_code", "WF_JUNIT_EXPLAIN");
        missingRun.put("inputs", new LinkedHashMap<String, Object>());
        Map<String, Object> validationError = invokePostExpectingClientError("/api/dify/workflows/run", missingRun);
        assertEquals("VALIDATION_ERROR", validationError.get("code"));

        // 正常调用：未启用 Dify 应回退到本地降级，且降级输出来自模板
        Map<String, Object> runBody = new LinkedHashMap<String, Object>();
        runBody.put("workflow_code", "WF_JUNIT_EXPLAIN");
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("patient_id", "P_JUNIT_001");
        inputs.put("target_code", "AMI_STEMI");
        runBody.put("inputs", inputs);
        Map<String, Object> runResp = invokePost("/api/dify/workflows/run", runBody);
        Map<String, Object> data = asMap(runResp.get("data"));
        assertEquals("DEGRADED", data.get("status"));
        assertEquals(Boolean.TRUE, data.get("template_applied"));
        Map<String, Object> outputs = asMap(data.get("outputs"));
        assertEquals("JUnit 模板降级输出。", outputs.get("explanation"));
        assertEquals("AMI_STEMI", outputs.get("target_code"));
    }

    @Test
    void graphNodesEdgesImportAndCandidatesRecall() throws Exception {
        String version = "JUNIT_GRAPH_NODES";

        Map<String, Object> diseaseNode = new LinkedHashMap<String, Object>();
        diseaseNode.put("code", "HEART_FAILURE");
        diseaseNode.put("name", "心力衰竭");
        diseaseNode.put("type", "DISEASE");
        diseaseNode.put("graph_version", version);
        Map<String, Object> symptomNode = new LinkedHashMap<String, Object>();
        symptomNode.put("code", "DYSPNEA");
        symptomNode.put("name", "呼吸困难");
        symptomNode.put("type", "SYMPTOM");
        symptomNode.put("graph_version", version);
        Map<String, Object> findingNode = new LinkedHashMap<String, Object>();
        findingNode.put("code", "BNP_HIGH");
        findingNode.put("name", "BNP 升高");
        findingNode.put("type", "FINDING");
        findingNode.put("graph_version", version);

        Map<String, Object> nodesBody = new LinkedHashMap<String, Object>();
        nodesBody.put("nodes", Arrays.asList(diseaseNode, symptomNode, findingNode));
        Map<String, Object> importedNodes = invokePost("/api/graph/nodes", nodesBody);
        assertEquals(3, asList(importedNodes.get("data")).size());

        Map<String, Object> edge1 = new LinkedHashMap<String, Object>();
        edge1.put("from_code", "DYSPNEA");
        edge1.put("to_code", "HEART_FAILURE");
        edge1.put("relation_type", "HAS_CORE_SYMPTOM");
        edge1.put("graph_version", version);
        edge1.put("weight", 0.8);
        Map<String, Object> edge2 = new LinkedHashMap<String, Object>();
        edge2.put("from_code", "BNP_HIGH");
        edge2.put("to_code", "HEART_FAILURE");
        edge2.put("relation_type", "HAS_EXAM_FINDING");
        edge2.put("graph_version", version);
        edge2.put("weight", 0.9);

        Map<String, Object> edgesBody = new LinkedHashMap<String, Object>();
        edgesBody.put("edges", Arrays.asList(edge1, edge2));
        Map<String, Object> importedEdges = invokePost("/api/graph/edges", edgesBody);
        assertEquals(2, asList(importedEdges.get("data")).size());

        Map<String, Object> nodesListResp = invokeGet("/api/graph/nodes?graphVersion=" + version + "&type=DISEASE");
        List<Map<String, Object>> nodesList = asListOfMap(nodesListResp.get("data"));
        assertEquals(1, nodesList.size());
        assertEquals("HEART_FAILURE", nodesList.get(0).get("code"));

        Map<String, Object> edgesListResp = invokeGet("/api/graph/edges?graphVersion=" + version + "&toCode=HEART_FAILURE");
        assertEquals(2, asList(edgesListResp.get("data")).size());

        // disease-candidates 降级：传入 DYSPNEA + BNP_HIGH 应召回 HEART_FAILURE，且不再返回 AMI_STEMI
        Map<String, Object> candidateRequest = new LinkedHashMap<String, Object>();
        candidateRequest.put("symptom_codes", Arrays.asList("DYSPNEA"));
        candidateRequest.put("finding_codes", Arrays.asList("BNP_HIGH"));
        candidateRequest.put("graph_version", version);
        Map<String, Object> candidateResp = invokePost("/api/graph/disease-candidates", candidateRequest);
        List<Map<String, Object>> candidates = asListOfMap(candidateResp.get("data"));
        assertEquals(1, candidates.size());
        Map<String, Object> first = candidates.get(0);
        assertEquals("HEART_FAILURE", first.get("diseaseCode"));
        assertEquals("REGISTERED_FALLBACK", first.get("graphSource"));
        // 评分 = 0.8 + 0.9 = 1.7，乘 100 = 170
        assertEquals(170.0, ((Number) first.get("rawGraphScore")).doubleValue());
        List<Map<String, Object>> relations = asListOfMap(first.get("matchedRelations"));
        assertEquals(2, relations.size());
    }

    @Test
    void graphVersionsAndEvidencesRegistration() throws Exception {
        Map<String, Object> versionEntry = new LinkedHashMap<String, Object>();
        versionEntry.put("graph_version", "JUNIT_GRAPH_2026_01");
        versionEntry.put("name", "JUnit 测试图谱");
        versionEntry.put("status", "DRAFT");
        versionEntry.put("description", "JUnit 用例的图谱版本");

        Map<String, Object> versionsBody = new LinkedHashMap<String, Object>();
        versionsBody.put("versions", Arrays.asList(versionEntry));
        Map<String, Object> importedVersions = invokePost("/api/graph/versions", versionsBody);
        assertEquals(1, asList(importedVersions.get("data")).size());

        Map<String, Object> activated = invokePost("/api/graph/versions/JUNIT_GRAPH_2026_01/activate",
                new LinkedHashMap<String, Object>());
        assertEquals("ACTIVE", asMap(activated.get("data")).get("status"));

        Map<String, Object> evidenceEntry = new LinkedHashMap<String, Object>();
        evidenceEntry.put("evidence_id", "EV_JUNIT_001");
        evidenceEntry.put("graph_version", "JUNIT_GRAPH_2026_01");
        evidenceEntry.put("target_code", "JUNIT_DISEASE");
        evidenceEntry.put("target_type", "DISEASE");
        evidenceEntry.put("evidence_type", "GUIDELINE");
        evidenceEntry.put("title", "JUnit 测试证据");
        evidenceEntry.put("summary", "用于验证 fallback 优先返回已注册证据。");
        evidenceEntry.put("confidence", 0.91);

        Map<String, Object> evidencesBody = new LinkedHashMap<String, Object>();
        evidencesBody.put("evidences", Arrays.asList(evidenceEntry));
        Map<String, Object> importedEvidences = invokePost("/api/graph/evidences", evidencesBody);
        assertEquals(1, asList(importedEvidences.get("data")).size());

        Map<String, Object> listResp = invokeGet("/api/graph/evidences?targetCode=JUNIT_DISEASE");
        List<Map<String, Object>> evidences = asListOfMap(listResp.get("data"));
        assertEquals(1, evidences.size());
        assertEquals("EV_JUNIT_001", evidences.get(0).get("evidence_id"));

        Map<String, Object> getResp = invokeGet("/api/graph/evidences/EV_JUNIT_001");
        assertEquals("JUNIT_DISEASE", asMap(getResp.get("data")).get("target_code"));

        // Neo4j 未启用：evidence 接口应返回注册证据而不是默认的 EV_AMI_001
        Map<String, Object> evidenceRequest = new LinkedHashMap<String, Object>();
        evidenceRequest.put("target_code", "JUNIT_DISEASE");
        evidenceRequest.put("graph_version", "JUNIT_GRAPH_2026_01");
        Map<String, Object> evidenceResp = invokePost("/api/graph/evidence", evidenceRequest);
        List<Map<String, Object>> returned = asListOfMap(evidenceResp.get("data"));
        assertEquals(1, returned.size());
        assertEquals("EV_JUNIT_001", returned.get(0).get("evidence_id"));
        assertEquals("REGISTERED_FALLBACK", returned.get(0).get("graph_source"));
    }

    @Test
    void graphVersionImportRejectsInvalidEntry() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<String, Object>();
        bad.put("versions", Arrays.asList(new LinkedHashMap<String, Object>()));
        Map<String, Object> response = invokePostExpectingClientError("/api/graph/versions", bad);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void graphDegradedWhenNeo4jDisabled() throws Exception {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("symptom_codes", Arrays.asList("CHEST_PAIN"));
        body.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        Map<String, Object> response = invokePost("/api/graph/disease-candidates", body);
        assertEquals(Boolean.TRUE, response.get("success"));
        // 关掉 Neo4j 时图谱接口应给出可解释降级，不应抛异常或返回失败。
        List<Map<String, Object>> candidates = asListOfMap(response.get("data"));
        assertFalse(candidates.isEmpty(), "fallback candidates should not be empty for AMI sample input");
        assertEquals("AMI_STEMI", candidates.get(0).get("diseaseCode"));
    }

    private Map<String, Object> ruleDefinition(String ruleCode, String ruleName) {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("rule_code", ruleCode);
        rule.put("rule_name", ruleName);
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("version_no", "1.0.0");
        rule.put("severity", "HIGH");
        rule.put("priority", 100);
        rule.put("enabled", true);
        Map<String, Object> chiefComplaint = new LinkedHashMap<String, Object>();
        chiefComplaint.put("fact", "chief_complaints.code");
        chiefComplaint.put("operator", "in");
        chiefComplaint.put("value", Arrays.asList("CHEST_PAIN"));
        Map<String, Object> exam = new LinkedHashMap<String, Object>();
        exam.put("fact", "exams.finding_codes");
        exam.put("operator", "contains");
        exam.put("value", "ST_ELEVATION_CONTIGUOUS_LEADS");
        Map<String, Object> condition = new LinkedHashMap<String, Object>();
        condition.put("all", Arrays.asList(chiefComplaint, exam));
        rule.put("condition", condition);
        rule.put("actions", Arrays.asList(actionMap("CREATE_RECOMMENDATION")));
        rule.put("message_template", "命中STEMI候选入径规则。");
        return rule;
    }

    private Map<String, Object> actionMap(String type) {
        Map<String, Object> action = new LinkedHashMap<String, Object>();
        action.put("type", type);
        return action;
    }

    private Map<String, Object> samplePatientContext() {
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_JUNIT_001");
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_JUNIT_001");
        encounter.put("visit_type", "EMERGENCY");
        Map<String, Object> chief = new LinkedHashMap<String, Object>();
        chief.put("code", "CHEST_PAIN");
        chief.put("text", "胸痛2小时");
        Map<String, Object> exam = new LinkedHashMap<String, Object>();
        exam.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("chief_complaints", Arrays.asList(chief));
        facts.put("exams", Arrays.asList(exam));
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("patient", patient);
        context.put("encounter", encounter);
        context.put("facts", facts);
        return context;
    }

    private Map<String, Object> samplePathwayConfig(String pathwayCode, String pathwayName) {
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("task_code", "TASK_TRIAGE");
        task.put("task_name", "急诊分诊");
        task.put("task_type", "TASK");
        task.put("required", true);

        Map<String, Object> nodeIdentify = new LinkedHashMap<String, Object>();
        nodeIdentify.put("node_code", "NODE_IDENTIFY");
        nodeIdentify.put("node_name", "识别");
        nodeIdentify.put("tasks", Arrays.asList(task));
        Map<String, Object> transition = new LinkedHashMap<String, Object>();
        transition.put("to_node", "NODE_TREATMENT");
        transition.put("priority", 100);
        nodeIdentify.put("transitions", Arrays.asList(transition));

        Map<String, Object> nodeTreatment = new LinkedHashMap<String, Object>();
        nodeTreatment.put("node_code", "NODE_TREATMENT");
        nodeTreatment.put("node_name", "治疗");

        Map<String, Object> stage = new LinkedHashMap<String, Object>();
        stage.put("stage_code", "STAGE_MAIN");
        stage.put("stage_name", "主流程");
        stage.put("nodes", Arrays.asList(nodeIdentify, nodeTreatment));

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("pathway_code", pathwayCode);
        config.put("pathway_name", pathwayName);
        config.put("version_no", "1.0.0");
        config.put("specialty_code", "CARDIOLOGY");
        config.put("disease_code", pathwayCode);
        config.put("stages", Arrays.asList(stage));
        return config;
    }

    private Map<String, Object> invokeGet(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "GET " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePost(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), "POST " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePostExpectingClientError(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertTrue(result.getResponse().getStatus() >= 400,
                "POST " + path + " expected 4xx but got " + result.getResponse().getStatus());
        return parse(result);
    }

    private Map<String, Object> parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readValue(body, LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : java.util.Collections.<Object>emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : java.util.Collections.<Map<String, Object>>emptyList();
    }
}
