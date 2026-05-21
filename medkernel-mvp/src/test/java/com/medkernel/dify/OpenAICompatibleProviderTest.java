package com.medkernel.dify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAICompatibleProvider 单元测试 — ADR-0013。
 *
 * <p>不调用真实 LLM API（避免依赖外部网络 / API key）。仅测试：
 * <ul>
 *   <li>配置解析 / 就绪判定</li>
 *   <li>URL 规范化</li>
 *   <li>错误处理（无 base-url / NOT_READY 配置）</li>
 *   <li>降级到 LOCAL 时返回结构正确</li>
 * </ul>
 *
 * <p>真实 LLM 调用测试见 ai-dev-input/07_tests/llm-integration（带 mock server）。
 */
public class OpenAICompatibleProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void notReadyWhenApiKeyMissing() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setType("openai-compatible");
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setModel("deepseek-chat");
        config.setEnabled(true);
        // apiKey 未设
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider("DEEPSEEK", config, objectMapper);
        assertFalse(provider.isReady(), "no api-key should make provider NOT_READY");
    }

    @Test
    public void notReadyWhenDisabled() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setType("openai-compatible");
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setModel("deepseek-chat");
        config.setApiKey("sk-test");
        config.setEnabled(false);
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider("DEEPSEEK", config, objectMapper);
        assertFalse(provider.isReady(), "disabled should make provider NOT_READY");
    }

    @Test
    public void readyWhenFullyConfigured() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setType("openai-compatible");
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setModel("deepseek-chat");
        config.setApiKey("sk-test");
        config.setEnabled(true);
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider("DEEPSEEK", config, objectMapper);
        assertTrue(provider.isReady(), "fully configured provider should be READY");
        assertEquals("DEEPSEEK", provider.getProviderType());
        assertEquals("DEEPSEEK/deepseek-chat", provider.getProviderName());
    }

    @Test
    public void ollamaReadyWithoutApiKey() {
        // Ollama 本地无鉴权，apiKey 可以为空
        LlmProviderConfig config = new LlmProviderConfig();
        config.setType("ollama");
        config.setBaseUrl("http://localhost:11434/v1");
        config.setModel("qwen2:7b");
        config.setApiKey("");
        config.setEnabled(true);
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider("OLLAMA_LOCAL", config, objectMapper);
        assertTrue(provider.isReady(), "Ollama without api-key should be READY when enabled+baseUrl+model present");
    }

    @Test
    public void urlNormalizationHandlesAllSuffixes() {
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                OpenAICompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com/v1"));
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                OpenAICompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com/v1/"));
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                OpenAICompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com/v1/chat/completions"));
        assertEquals("http://localhost:11434/v1/chat/completions",
                OpenAICompatibleProvider.resolveChatCompletionsUrl("http://localhost:11434/v1"));
    }

    @Test
    public void invokeReturnsErrorWhenBaseUrlInvalid() {
        // base-url 指向不可达地址，invoke 应捕获并返回错误，不抛异常
        LlmProviderConfig config = new LlmProviderConfig();
        config.setType("openai-compatible");
        config.setBaseUrl("http://127.0.0.1:1");  // 故意不可达端口
        config.setModel("test");
        config.setApiKey("sk-test");
        config.setEnabled(true);
        config.setTimeoutMs(500);   // 短超时让测试快
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider("TEST", config, objectMapper);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("prompt", "test prompt");
        Map<String, Object> result = provider.invoke(request);

        assertNotNull(result);
        assertEquals("ERROR", result.get("status"));
        assertEquals("TEST", result.get("provider"));
        assertNotNull(result.get("message"));
        assertNotNull(result.get("trace_id"));
        assertNotNull(result.get("elapsed_ms"));
    }
}
