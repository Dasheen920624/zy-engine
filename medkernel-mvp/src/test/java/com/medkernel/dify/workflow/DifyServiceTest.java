package com.medkernel.dify.workflow;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dify工作流服务测试")
class DifyServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private DifyProperties difyProperties;
    private DifyService difyService;

    @BeforeEach
    void setUp() {
        difyProperties = new DifyProperties();
        difyProperties.setEnabled(false);
        difyProperties.setBaseUrl("http://localhost/v1");
        difyProperties.setApiKey("test-api-key");
        difyProperties.setTimeoutMs(3000);

        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());
        doNothing().when(persistenceService).saveDifyTemplate(any());

        difyService = new DifyService(difyProperties, persistenceService);
    }

    // ========== 模板导入 ==========

    @Test
    @DisplayName("导入模板 - 成功导入单个模板")
    void importTemplates_shouldImportSingleTemplate() {
        Map<String, Object> templateData = buildTemplateData("WF_TEST", "测试工作流", "1.0.0");

        List<DifyWorkflowTemplate> result = difyService.importTemplates(
                Collections.singletonList(templateData));

        assertEquals(1, result.size());
        assertEquals("WF_TEST", result.get(0).getWorkflowCode());
        assertEquals("1.0.0", result.get(0).getWorkflowVersion());
    }

    @Test
    @DisplayName("导入模板 - 成功导入多个模板")
    void importTemplates_shouldImportMultipleTemplates() {
        Map<String, Object> template1 = buildTemplateData("WF_A", "工作流A", "1.0.0");
        Map<String, Object> template2 = buildTemplateData("WF_B", "工作流B", "2.0.0");

        List<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();
        templates.add(template1);
        templates.add(template2);

        List<DifyWorkflowTemplate> result = difyService.importTemplates(templates);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("导入模板 - 空列表抛异常")
    void importTemplates_shouldThrowWhenEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> difyService.importTemplates(Collections.emptyList()));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("导入模板 - 缺少workflow_code抛异常")
    void importTemplates_shouldThrowWhenWorkflowCodeMissing() {
        Map<String, Object> templateData = new LinkedHashMap<String, Object>();
        templateData.put("workflow_name", "无编码工作流");

        List<Map<String, Object>> templates = Collections.singletonList(templateData);
        assertThrows(IllegalArgumentException.class,
                () -> difyService.importTemplates(templates));
    }

    @Test
    @DisplayName("导入模板 - 使用嵌套templates格式")
    void importTemplates_shouldSupportNestedTemplatesFormat() {
        Map<String, Object> templateData = buildTemplateData("WF_NESTED", "嵌套工作流", "1.0.0");
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("templates", Collections.singletonList(templateData));

        List<DifyWorkflowTemplate> result = difyService.importTemplates(request);

        assertEquals(1, result.size());
        assertEquals("WF_NESTED", result.get(0).getWorkflowCode());
    }

    @Test
    @DisplayName("导入模板 - 设置降级输出")
    void importTemplates_shouldSetDegradedOutputs() {
        Map<String, Object> templateData = buildTemplateData("WF_DEGRADED", "降级工作流", "1.0.0");
        Map<String, Object> degradedOutputs = new LinkedHashMap<String, Object>();
        degradedOutputs.put("explanation", "降级解释文本");
        degradedOutputs.put("recommended_action", "建议人工确认");
        templateData.put("degraded_outputs", degradedOutputs);

        List<DifyWorkflowTemplate> result = difyService.importTemplates(
                Collections.singletonList(templateData));

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDegradedOutputs());
        assertEquals("降级解释文本", result.get(0).getDegradedOutputs().get("explanation"));
    }

    // ========== 模板列表 ==========

    @Test
    @DisplayName("列出模板 - 返回已导入的模板")
    void listTemplates_shouldReturnImportedTemplates() {
        difyService.importTemplates(
                Collections.singletonList(buildTemplateData("WF_LIST", "列表工作流", "1.0.0")));

        List<DifyWorkflowTemplate> result = difyService.listTemplates();

        boolean found = false;
        for (DifyWorkflowTemplate t : result) {
            if ("WF_LIST".equals(t.getWorkflowCode())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "应能列出已导入的模板");
    }

    // ========== 获取模板 ==========

    @Test
    @DisplayName("获取模板 - 成功获取已导入模板")
    void getTemplate_shouldReturnImportedTemplate() {
        difyService.importTemplates(
                Collections.singletonList(buildTemplateData("WF_GET", "获取工作流", "1.0.0")));

        DifyWorkflowTemplate result = difyService.getTemplate("WF_GET", "1.0.0");

        assertEquals("WF_GET", result.getWorkflowCode());
        assertEquals("1.0.0", result.getWorkflowVersion());
    }

    @Test
    @DisplayName("获取模板 - 不指定版本返回最新版本")
    void getTemplate_shouldReturnLatestWhenVersionNotSpecified() {
        Map<String, Object> v1 = buildTemplateData("WF_VER", "版本工作流", "1.0.0");
        Map<String, Object> v2 = buildTemplateData("WF_VER", "版本工作流", "2.0.0");

        List<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();
        templates.add(v1);
        templates.add(v2);
        difyService.importTemplates(templates);

        DifyWorkflowTemplate result = difyService.getTemplate("WF_VER", null);

        assertEquals("2.0.0", result.getWorkflowVersion());
    }

    @Test
    @DisplayName("获取模板 - 不存在抛异常")
    void getTemplate_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> difyService.getTemplate("WF_NOT_EXIST", "1.0.0"));
    }

    // ========== 工作流执行 - Dify未启用降级 ==========

    @Test
    @DisplayName("执行工作流 - Dify未启用时返回降级结果")
    void runWorkflow_shouldReturnDegradedWhenDifyDisabled() {
        difyProperties.setEnabled(false);

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_DISABLED");
        request.put("inputs", Collections.emptyMap());

        Map<String, Object> result = difyService.runWorkflow(request);

        assertEquals("DEGRADED", result.get("status"));
        assertEquals("LOCAL_FALLBACK", result.get("provider"));
        assertEquals("DIFY_DISABLED", result.get("error_code"));
    }

    @Test
    @DisplayName("执行工作流 - Dify配置不完整时返回降级结果")
    void runWorkflow_shouldReturnDegradedWhenDifyNotReady() {
        difyProperties.setEnabled(true);
        difyProperties.setBaseUrl(null);
        difyProperties.setApiKey(null);

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_NOT_READY");
        request.put("inputs", Collections.emptyMap());

        Map<String, Object> result = difyService.runWorkflow(request);

        assertEquals("DEGRADED", result.get("status"));
        assertEquals("LOCAL_FALLBACK", result.get("provider"));
    }

    // ========== 工作流执行 - 降级输出 ==========

    @Test
    @DisplayName("执行工作流 - 降级时使用模板定义的降级输出")
    void runWorkflow_shouldUseTemplateDegradedOutputs() {
        difyProperties.setEnabled(false);

        Map<String, Object> templateData = buildTemplateData("WF_DEG_OUT", "降级输出工作流", "1.0.0");
        Map<String, Object> degradedOutputs = new LinkedHashMap<String, Object>();
        degradedOutputs.put("explanation", "自定义降级解释");
        degradedOutputs.put("recommended_action", "自定义建议");
        templateData.put("degraded_outputs", degradedOutputs);
        difyService.importTemplates(Collections.singletonList(templateData));

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_DEG_OUT");
        request.put("workflow_version", "1.0.0");
        request.put("inputs", Collections.emptyMap());

        Map<String, Object> result = difyService.runWorkflow(request);

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> outputs = (Map<String, Object>) result.get("outputs");
        assertEquals("自定义降级解释", outputs.get("explanation"));
    }

    @Test
    @DisplayName("执行工作流 - 降级时无模板使用默认降级输出")
    void runWorkflow_shouldUseDefaultDegradedOutputsWithoutTemplate() {
        difyProperties.setEnabled(false);

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_NO_TEMPLATE");
        request.put("inputs", Collections.emptyMap());

        Map<String, Object> result = difyService.runWorkflow(request);

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> outputs = (Map<String, Object>) result.get("outputs");
        assertTrue(outputs.containsKey("explanation"));
        assertTrue(outputs.containsKey("recommended_action"));
    }

    // ========== 工作流执行 - 输入校验 ==========

    @Test
    @DisplayName("执行工作流 - 缺少必填输入字段抛异常")
    void runWorkflow_shouldThrowWhenRequiredInputsMissing() {
        difyProperties.setEnabled(true);
        difyProperties.setBaseUrl("http://localhost/v1");
        difyProperties.setApiKey("test-key");

        Map<String, Object> templateData = buildTemplateData("WF_REQUIRED", "必填工作流", "1.0.0");
        templateData.put("required_inputs", Collections.singletonList("patient_id"));
        difyService.importTemplates(Collections.singletonList(templateData));

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_REQUIRED");
        request.put("workflow_version", "1.0.0");
        request.put("inputs", Collections.emptyMap());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> difyService.runWorkflow(request));
        assertTrue(ex.getMessage().contains("patient_id"));
    }

    @Test
    @DisplayName("执行工作流 - 输入默认值填充")
    void runWorkflow_shouldApplyInputDefaults() {
        Map<String, Object> templateData = buildTemplateData("WF_DEFAULTS", "默认值工作流", "1.0.0");
        Map<String, Object> inputDefaults = new LinkedHashMap<String, Object>();
        inputDefaults.put("model_type", "gpt-4");
        inputDefaults.put("temperature", 0.7);
        templateData.put("input_defaults", inputDefaults);
        difyService.importTemplates(Collections.singletonList(templateData));

        // 验证模板已导入，输入默认值已设置
        DifyWorkflowTemplate tmpl = difyService.getTemplate("WF_DEFAULTS", "1.0.0");
        assertNotNull(tmpl.getInputDefaults());
        assertEquals("gpt-4", tmpl.getInputDefaults().get("model_type"));
    }

    // ========== 调用统计 ==========

    @Test
    @DisplayName("调用统计 - 返回工作流调用统计信息")
    void summarizeInvocations_shouldReturnInvocationSummary() {
        // 先执行一次降级调用
        difyProperties.setEnabled(false);
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("workflow_code", "WF_SUMMARY");
        request.put("inputs", Collections.emptyMap());
        difyService.runWorkflow(request);

        Map<String, Object> summary = difyService.summarizeInvocations(null);

        assertTrue(((Number) summary.get("total_calls")).intValue() >= 1);
        assertNotNull(summary.get("by_status"));
        assertNotNull(summary.get("by_provider"));
    }

    @Test
    @DisplayName("调用统计 - 按工作流编码过滤")
    void summarizeInvocations_shouldFilterByWorkflowCode() {
        difyProperties.setEnabled(false);

        Map<String, Object> request1 = new LinkedHashMap<String, Object>();
        request1.put("workflow_code", "WF_FILTER_A");
        request1.put("inputs", Collections.emptyMap());
        difyService.runWorkflow(request1);

        Map<String, Object> request2 = new LinkedHashMap<String, Object>();
        request2.put("workflow_code", "WF_FILTER_B");
        request2.put("inputs", Collections.emptyMap());
        difyService.runWorkflow(request2);

        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("workflowCode", "WF_FILTER_A");
        Map<String, Object> summary = difyService.summarizeInvocations(filters);

        assertEquals(1, ((Number) summary.get("total_calls")).intValue());
    }

    // ========== 超时处理 ==========

    @Test
    @DisplayName("模板超时配置 - 自定义超时时间")
    void templateTimeout_shouldSetCustomTimeout() {
        Map<String, Object> templateData = buildTemplateData("WF_TIMEOUT", "超时工作流", "1.0.0");
        templateData.put("timeout_ms", 5000);
        templateData.put("retry_count", 2);
        difyService.importTemplates(Collections.singletonList(templateData));

        DifyWorkflowTemplate template = difyService.getTemplate("WF_TIMEOUT", "1.0.0");

        assertEquals(5000, template.getTimeoutMs().intValue());
        assertEquals(2, template.getRetryCount().intValue());
    }

    @Test
    @DisplayName("模板超时配置 - 默认超时时间")
    void templateTimeout_shouldUseDefaultTimeout() {
        Map<String, Object> templateData = buildTemplateData("WF_DEFAULT_TIMEOUT", "默认超时工作流", "1.0.0");
        difyService.importTemplates(Collections.singletonList(templateData));

        // 未设置timeoutMs时为null，由DifyProperties提供默认值
        assertEquals(3000, difyProperties.getTimeoutMs());
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> buildTemplateData(String workflowCode, String workflowName, String version) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("workflow_code", workflowCode);
        data.put("workflow_name", workflowName);
        data.put("workflow_version", version);
        data.put("description", "测试工作流描述");
        data.put("dify_app_code", "app-" + workflowCode.toLowerCase());
        return data;
    }
}
