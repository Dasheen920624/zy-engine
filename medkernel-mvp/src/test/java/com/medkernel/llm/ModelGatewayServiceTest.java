package com.medkernel.llm;

import com.medkernel.knowledge.AiKnowledgeJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("模型网关服务单元测试")
class ModelGatewayServiceTest {

    @Mock
    private LlmProviderFactory llmProviderFactory;

    @Mock
    private AiKnowledgeJobService jobService;

    @Mock
    private ModelGatewayProperties properties;

    @Mock
    private ModelProvider qianwenProvider;

    @Mock
    private ModelProvider deepseekProvider;

    @Mock
    private ModelProvider localProvider;

    @Mock
    private ModelProvider ollamaProvider;

    private ModelGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        // 配置 Provider mock 基础属性
        when(qianwenProvider.getProviderType()).thenReturn("QIANWEN");
        when(qianwenProvider.getProviderName()).thenReturn("qwen-max");
        when(qianwenProvider.isReady()).thenReturn(true);

        when(deepseekProvider.getProviderType()).thenReturn("DEEPSEEK");
        when(deepseekProvider.getProviderName()).thenReturn("deepseek-chat");
        when(deepseekProvider.isReady()).thenReturn(true);

        when(localProvider.getProviderType()).thenReturn("LOCAL");
        when(localProvider.getProviderName()).thenReturn("local-rules");
        when(localProvider.isReady()).thenReturn(true);

        when(ollamaProvider.getProviderType()).thenReturn("OLLAMA_LOCAL");
        when(ollamaProvider.getProviderName()).thenReturn("ollama-llama3");
        when(ollamaProvider.isReady()).thenReturn(true);

        when(properties.isEnabled()).thenReturn(true);
        when(properties.getDegradationChains()).thenReturn(new LinkedHashMap<>());
    }

    private ModelGatewayService createGateway(List<ModelProvider> staticProviders) {
        when(llmProviderFactory.getDynamicProviders()).thenReturn(Collections.emptyList());
        ModelGatewayService service = new ModelGatewayService(
                staticProviders, llmProviderFactory, jobService, properties);
        service.init();
        return service;
    }

    private Map<String, Object> buildRequest() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("prompt", "测试提示词");
        req.put("patient_id", "P001");
        req.put("tenant_id", 100L);
        return req;
    }

    private Map<String, Object> buildSuccessResult(String providerType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("provider", providerType);
        result.put("content", "模型返回内容");
        return result;
    }

    // ──────────────────────── 模型调用测试 ────────────────────────

    @Nested
    @DisplayName("模型调用")
    class InvokeTests {

        @Test
        @DisplayName("调用成功 - 主Provider可用时直接返回结果")
        void invoke_primaryProviderAvailable_returnsResult() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            Map<String, Object> successResult = buildSuccessResult("QIANWEN");
            when(qianwenProvider.invoke(any())).thenReturn(successResult);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("SUCCESS", result.get("status"));
            assertEquals("RESEARCH", result.get("call_type"));
            assertNotNull(result.get("trace_id"));
            assertNotNull(result.get("elapsed_ms"));
        }

        @Test
        @DisplayName("降级链 - 主Provider不可用时自动降级到备用Provider")
        void invoke_primaryFails_fallsBackToSecondary() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            when(qianwenProvider.isReady()).thenReturn(false);
            Map<String, Object> deepseekResult = buildSuccessResult("DEEPSEEK");
            when(deepseekProvider.invoke(any())).thenReturn(deepseekResult);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("SUCCESS", result.get("status"));
            assertEquals("true", result.get("fallback_used"));
            assertEquals("DEEPSEEK", result.get("fallback_provider"));
            assertEquals("QIANWEN", result.get("primary_provider"));
        }

        @Test
        @DisplayName("降级链 - 主Provider调用异常时降级到备用Provider")
        void invoke_primaryThrows_fallsBackToSecondary() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            when(qianwenProvider.invoke(any())).thenThrow(new RuntimeException("连接超时"));
            Map<String, Object> deepseekResult = buildSuccessResult("DEEPSEEK");
            when(deepseekProvider.invoke(any())).thenReturn(deepseekResult);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("SUCCESS", result.get("status"));
            assertEquals("true", result.get("fallback_used"));
            assertEquals("DEEPSEEK", result.get("fallback_provider"));
        }

        @Test
        @DisplayName("降级链 - 所有Provider都不可用时返回错误")
        void invoke_allProvidersFail_returnsError() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            when(qianwenProvider.isReady()).thenReturn(false);
            when(deepseekProvider.isReady()).thenReturn(false);
            when(localProvider.isReady()).thenReturn(false);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("NO_PROVIDER_AVAILABLE", result.get("status"));
            assertEquals("RESEARCH", result.get("call_type"));
        }

        @Test
        @DisplayName("降级链 - 所有Provider调用异常时返回最后一个错误")
        void invoke_allProvidersThrow_returnsLastError() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, localProvider));

            when(qianwenProvider.invoke(any())).thenThrow(new RuntimeException("QIANWEN超时"));
            when(localProvider.invoke(any())).thenThrow(new RuntimeException("LOCAL异常"));

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("ERROR", result.get("status"));
            assertNotNull(result.get("message"));
        }

        @Test
        @DisplayName("网关禁用 - 返回DISABLED状态")
        void invoke_gatewayDisabled_returnsDisabled() {
            when(properties.isEnabled()).thenReturn(false);
            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("DISABLED", result.get("status"));
            assertEquals("RESEARCH", result.get("call_type"));
        }

        @Test
        @DisplayName("降级到LOCAL - 最终兜底Provider可用")
        void invoke_fallsBackToLocal_returnsResult() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            when(qianwenProvider.invoke(any())).thenThrow(new RuntimeException("超时"));
            when(deepseekProvider.invoke(any())).thenThrow(new RuntimeException("超时"));
            Map<String, Object> localResult = buildSuccessResult("LOCAL");
            when(localProvider.invoke(any())).thenReturn(localResult);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("SUCCESS", result.get("status"));
            assertEquals("true", result.get("fallback_used"));
            assertEquals("LOCAL", result.get("fallback_provider"));
        }
    }

    // ──────────────────────── Provider 列表测试 ────────────────────────

    @Nested
    @DisplayName("Provider 列表查询")
    class ListProvidersTests {

        @Test
        @DisplayName("列出所有已注册Provider")
        void listProviders_returnsAllRegistered() {
            gatewayService = createGateway(Arrays.asList(qianwenProvider, deepseekProvider, localProvider));

            List<Map<String, Object>> providers = gatewayService.listProviders();

            assertEquals(3, providers.size());

            Map<String, Object> first = providers.get(0);
            assertEquals("QIANWEN", first.get("provider_type"));
            assertEquals("qwen-max", first.get("provider_name"));
            assertTrue((Boolean) first.get("ready"));
            assertEquals("READY", first.get("status"));
        }

        @Test
        @DisplayName("无Provider时返回空列表")
        void listProviders_noProviders_returnsEmpty() {
            gatewayService = createGateway(Collections.emptyList());

            List<Map<String, Object>> providers = gatewayService.listProviders();

            assertTrue(providers.isEmpty());
        }

        @Test
        @DisplayName("不可用Provider状态为UNAVAILABLE")
        void listProviders_unavailableProvider_showsUnavailable() {
            when(qianwenProvider.isReady()).thenReturn(false);
            gatewayService = createGateway(Collections.singletonList(qianwenProvider));

            List<Map<String, Object>> providers = gatewayService.listProviders();

            assertEquals(1, providers.size());
            assertFalse((Boolean) providers.get(0).get("ready"));
            assertEquals("UNAVAILABLE", providers.get(0).get("status"));
        }
    }

    // ──────────────────────── 降级链查询测试 ────────────────────────

    @Nested
    @DisplayName("降级链查询")
    class DegradationChainTests {

        @Test
        @DisplayName("查询RESEARCH类型降级链")
        void getDegradationChain_researchType() {
            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> chain = gatewayService.getDegradationChain("RESEARCH");

            assertEquals("RESEARCH", chain.get("call_type"));
            assertNotNull(chain.get("chain"));
            List<?> providers = (List<?>) chain.get("providers");
            assertFalse(providers.isEmpty());
            assertEquals("QIANWEN", providers.get(0));
        }

        @Test
        @DisplayName("查询EXTRACT类型降级链 - DEEPSEEK优先")
        void getDegradationChain_extractType_deepseekFirst() {
            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> chain = gatewayService.getDegradationChain("EXTRACT");

            List<?> providers = (List<?>) chain.get("providers");
            assertEquals("DEEPSEEK", providers.get(0));
        }

        @Test
        @DisplayName("查询WORKFLOW类型降级链 - DIFY优先")
        void getDegradationChain_workflowType_difyFirst() {
            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> chain = gatewayService.getDegradationChain("WORKFLOW");

            List<?> providers = (List<?>) chain.get("providers");
            assertEquals("DIFY", providers.get(0));
        }

        @Test
        @DisplayName("查询未知类型降级链 - 返回空链")
        void getDegradationChain_unknownType_emptyChain() {
            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> chain = gatewayService.getDegradationChain("UNKNOWN_TYPE");

            List<?> providers = (List<?>) chain.get("providers");
            assertTrue(providers.isEmpty());
        }

        @Test
        @DisplayName("自定义降级链覆盖默认链")
        void getDegradationChain_customChainOverridesDefault() {
            Map<String, String> customChains = new LinkedHashMap<>();
            customChains.put("RESEARCH", "DEEPSEEK,LOCAL");
            when(properties.getDegradationChains()).thenReturn(customChains);

            gatewayService = createGateway(Collections.emptyList());

            Map<String, Object> chain = gatewayService.getDegradationChain("RESEARCH");

            List<?> providers = (List<?>) chain.get("providers");
            assertEquals(2, providers.size());
            assertEquals("DEEPSEEK", providers.get(0));
            assertEquals("LOCAL", providers.get(1));
        }
    }

    // ──────────────────────── Provider 状态查询测试 ────────────────────────

    @Nested
    @DisplayName("Provider 状态查询")
    class ProviderStatusTests {

        @Test
        @DisplayName("查询已注册Provider状态")
        void getProviderStatus_registeredProvider() {
            gatewayService = createGateway(Collections.singletonList(qianwenProvider));

            Map<String, Object> status = gatewayService.getProviderStatus("QIANWEN");

            assertTrue((Boolean) status.get("registered"));
            assertTrue((Boolean) status.get("ready"));
            assertEquals("READY", status.get("status"));
            assertEquals("qwen-max", status.get("provider_name"));
        }

        @Test
        @DisplayName("查询未注册Provider状态")
        void getProviderStatus_unregisteredProvider() {
            gatewayService = createGateway(Collections.singletonList(qianwenProvider));

            Map<String, Object> status = gatewayService.getProviderStatus("NONEXISTENT");

            assertFalse((Boolean) status.get("registered"));
            assertFalse((Boolean) status.get("ready"));
            assertEquals("NOT_FOUND", status.get("status"));
        }

        @Test
        @DisplayName("查询不可用Provider状态")
        void getProviderStatus_unavailableProvider() {
            when(qianwenProvider.isReady()).thenReturn(false);
            gatewayService = createGateway(Collections.singletonList(qianwenProvider));

            Map<String, Object> status = gatewayService.getProviderStatus("QIANWEN");

            assertTrue((Boolean) status.get("registered"));
            assertFalse((Boolean) status.get("ready"));
            assertEquals("UNAVAILABLE", status.get("status"));
        }
    }

    // ──────────────────────── 网关启用状态测试 ────────────────────────

    @Nested
    @DisplayName("网关启用状态")
    class EnabledTests {

        @Test
        @DisplayName("网关默认启用")
        void isEnabled_defaultTrue() {
            when(properties.isEnabled()).thenReturn(true);
            gatewayService = createGateway(Collections.emptyList());

            assertTrue(gatewayService.isEnabled());
        }

        @Test
        @DisplayName("网关可禁用")
        void isEnabled_canBeDisabled() {
            when(properties.isEnabled()).thenReturn(false);
            gatewayService = createGateway(Collections.emptyList());

            assertFalse(gatewayService.isEnabled());
        }
    }

    // ──────────────────────── 动态Provider注册测试 ────────────────────────

    @Nested
    @DisplayName("动态Provider注册")
    class DynamicProviderTests {

        @Test
        @DisplayName("动态Provider与静态Provider合并注册")
        void init_dynamicAndStaticProviders_merged() {
            when(llmProviderFactory.getDynamicProviders())
                    .thenReturn(Collections.singletonList(ollamaProvider));

            gatewayService = createGateway(Arrays.asList(qianwenProvider, localProvider));

            List<Map<String, Object>> providers = gatewayService.listProviders();

            assertEquals(3, providers.size());
            assertTrue(providers.stream().anyMatch(p -> "OLLAMA_LOCAL".equals(p.get("provider_type"))));
            assertTrue(providers.stream().anyMatch(p -> "QIANWEN".equals(p.get("provider_type"))));
            assertTrue(providers.stream().anyMatch(p -> "LOCAL".equals(p.get("provider_type"))));
        }

        @Test
        @DisplayName("动态Provider覆盖同名静态Provider")
        void init_dynamicOverridesStatic_sameType() {
            ModelProvider staticQianwen = new ModelProvider() {
                @Override public String getProviderType() { return "QIANWEN"; }
                @Override public boolean isReady() { return false; }
                @Override public Map<String, Object> invoke(Map<String, Object> request) { return Collections.emptyMap(); }
                @Override public String getProviderName() { return "static-qwen"; }
            };

            when(llmProviderFactory.getDynamicProviders())
                    .thenReturn(Collections.singletonList(qianwenProvider));

            gatewayService = createGateway(Collections.singletonList(staticQianwen));

            Map<String, Object> status = gatewayService.getProviderStatus("QIANWEN");
            // 动态 Provider 覆盖后应该使用动态的（ready=true, name=qwen-max）
            assertTrue((Boolean) status.get("ready"));
            assertEquals("qwen-max", status.get("provider_name"));
        }
    }

    // ──────────────────────── Provider未注册时的降级测试 ────────────────────────

    @Nested
    @DisplayName("Provider未注册降级")
    class UnregisteredProviderTests {

        @Test
        @DisplayName("降级链中Provider未注册时跳过继续降级")
        void invoke_unregisteredProviderInChain_skipsAndContinues() {
            // 只注册 LOCAL，降级链中 QIANWEN/DEEPSEEK 未注册
            gatewayService = createGateway(Collections.singletonList(localProvider));

            Map<String, Object> localResult = buildSuccessResult("LOCAL");
            when(localProvider.invoke(any())).thenReturn(localResult);

            Map<String, Object> result = gatewayService.invoke("RESEARCH", buildRequest());

            assertEquals("SUCCESS", result.get("status"));
            assertEquals("true", result.get("fallback_used"));
            assertEquals("LOCAL", result.get("fallback_provider"));
        }
    }
}
