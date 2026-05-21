package com.medkernel.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议 LLM Provider —— 单一类适配 80% 国产大模型 + 本地 Ollama。
 *
 * <p>支持的厂商（仅需配 base-url + api-key + model）：
 * <ul>
 *   <li>阿里通义千问（DashScope 兼容模式）：base-url=https://dashscope.aliyuncs.com/compatible-mode/v1</li>
 *   <li>DeepSeek：base-url=https://api.deepseek.com/v1</li>
 *   <li>月之暗面 Kimi：base-url=https://api.moonshot.cn/v1</li>
 *   <li>智谱 GLM：base-url=https://open.bigmodel.cn/api/paas/v4</li>
 *   <li>字节豆包（火山引擎 ARK）：base-url=https://ark.cn-beijing.volces.com/api/v3</li>
 *   <li>MiniMax / 百川 / 零一万物 / 阶跃 / 商汤等 OpenAI 兼容</li>
 *   <li>本地 Ollama（0.1.30+）：base-url=http://localhost:11434/v1</li>
 * </ul>
 *
 * <p>不实例化为 Spring Bean（{@link LlmProviderFactory} 启动时根据 application.yml 配置
 * 为每个 providers.* 项实例化一个 instance，注入到 {@link ModelGatewayService}）。
 *
 * <p>ADR-0013：去 Dify 化策略。
 */
public class OpenAICompatibleProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleProvider.class);

    private final String providerType;
    private final LlmProviderConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAICompatibleProvider(String providerType, LlmProviderConfig config, ObjectMapper objectMapper) {
        this.providerType = providerType;
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = buildRestTemplate(config.getTimeoutMs());
    }

    private static RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(timeoutMs, 5000));
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    @Override
    public String getProviderType() {
        return providerType;
    }

    @Override
    public boolean isReady() {
        return config.isReady();
    }

    @Override
    public String getProviderName() {
        if (config.getModel() == null) {
            return providerType;
        }
        return providerType + "/" + config.getModel();
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> request) {
        Map<String, Object> safeRequest = request != null ? request : new LinkedHashMap<String, Object>();
        long start = System.currentTimeMillis();

        try {
            String url = resolveChatCompletionsUrl(config.getBaseUrl());
            String prompt = buildPrompt(safeRequest);

            Map<String, Object> body = buildRequestBody(prompt);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return parseResponse(response.getBody(), start);
        } catch (RestClientException ex) {
            return buildErrorResult("PROVIDER_HTTP_ERROR",
                    "HTTP call to " + providerType + " failed: " + ex.getMessage(), start);
        } catch (Exception ex) {
            log.error("Provider {} invocation failed", providerType, ex);
            return buildErrorResult("PROVIDER_ERROR",
                    "Provider invocation failed: " + ex.getMessage(), start);
        }
    }

    /**
     * 规范化 base-url 到 OpenAI Chat Completions endpoint。
     */
    static String resolveChatCompletionsUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("base-url is required");
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "chat/completions";
        }
        return trimmed + "/chat/completions";
    }

    /**
     * 从 medkernel 业务请求构造 prompt。
     * <ul>
     *   <li>若 request 有 "prompt" / "query" / "question" 字段 → 直接用</li>
     *   <li>否则把整个 request JSON 当作 user 输入（保留所有业务上下文）</li>
     * </ul>
     */
    private String buildPrompt(Map<String, Object> request) {
        Object prompt = request.get("prompt");
        if (prompt == null) prompt = request.get("query");
        if (prompt == null) prompt = request.get("question");
        if (prompt instanceof String && !((String) prompt).trim().isEmpty()) {
            return (String) prompt;
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            return String.valueOf(request);
        }
    }

    /**
     * 构造 OpenAI Chat Completions 请求体。
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);

        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());
        body.put("stream", false);
        return body;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + config.getApiKey().trim());
        }
        return headers;
    }

    /**
     * 解析 OpenAI 兼容响应：
     * <pre>
     * { "choices": [ { "message": { "role": "assistant", "content": "..." } } ],
     *   "usage": { "prompt_tokens": N, "completion_tokens": N, "total_tokens": N } }
     * </pre>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> parseResponse(Map responseBody, long startMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("provider", providerType);
        result.put("model", config.getModel());
        result.put("trace_id", TraceContext.getTraceId());
        result.put("elapsed_ms", System.currentTimeMillis() - startMs);

        if (responseBody == null) {
            result.put("status", "EMPTY_RESPONSE");
            result.put("message", "LLM returned empty response body");
            return result;
        }

        String content = extractContent(responseBody);
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("content", content);
        outputs.put("answer", content);
        outputs.put("explanation", content);
        result.put("outputs", outputs);

        Object usage = responseBody.get("usage");
        if (usage instanceof Map) {
            Map usageMap = (Map) usage;
            result.put("prompt_tokens", usageMap.get("prompt_tokens"));
            result.put("completion_tokens", usageMap.get("completion_tokens"));
            result.put("total_tokens", usageMap.get("total_tokens"));
        }

        return result;
    }

    @SuppressWarnings({"rawtypes"})
    private String extractContent(Map responseBody) {
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List)) {
            return "";
        }
        List choices = (List) choicesObj;
        if (choices.isEmpty()) {
            return "";
        }
        Object first = choices.get(0);
        if (!(first instanceof Map)) {
            return "";
        }
        Object messageObj = ((Map) first).get("message");
        if (!(messageObj instanceof Map)) {
            return "";
        }
        Object content = ((Map) messageObj).get("content");
        return content == null ? "" : String.valueOf(content);
    }

    private Map<String, Object> buildErrorResult(String errorCode, String message, long startMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ERROR");
        result.put("provider", providerType);
        result.put("error_code", errorCode);
        result.put("message", message);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("elapsed_ms", System.currentTimeMillis() - startMs);
        return result;
    }
}
