package com.medkernel.dify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM Provider 工厂 —— 启动时根据 {@link ModelGatewayProperties#getProviders()} 配置
 * 为每个配置项实例化一个 {@link OpenAICompatibleProvider}（或 Ollama / 其它）。
 *
 * <p>不直接注册为 Spring Bean（避免动态注册的复杂度），而是被 {@link ModelGatewayService}
 * 注入后调用 {@link #getDynamicProviders()} 取所有动态 Provider，与 @Component 标注的静态
 * Provider（DifyModelProvider / LocalModelProvider）合并为完整的 Provider 集合。
 *
 * <p>ADR-0013：去 Dify 化策略。
 */
@Component
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);

    private final ModelGatewayProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, ModelProvider> dynamicProviders = new LinkedHashMap<String, ModelProvider>();

    public LlmProviderFactory(ModelGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        Map<String, LlmProviderConfig> configs = properties.getProviders();
        if (configs == null || configs.isEmpty()) {
            log.info("No dynamic LLM providers configured in medkernel.model-gateway.providers — only static providers (DIFY/LOCAL) available");
            return;
        }
        for (Map.Entry<String, LlmProviderConfig> entry : configs.entrySet()) {
            String providerType = entry.getKey() == null ? "" : entry.getKey().trim().toUpperCase();
            LlmProviderConfig config = entry.getValue();
            if (providerType.isEmpty() || config == null) {
                log.warn("Skip empty LLM provider entry");
                continue;
            }
            try {
                ModelProvider provider = createProvider(providerType, config);
                if (provider != null) {
                    dynamicProviders.put(providerType, provider);
                    log.info("LLM provider '{}' registered (type={}, model={}, ready={})",
                            providerType, config.getType(), config.getModel(), provider.isReady());
                }
            } catch (Exception ex) {
                log.error("Failed to create LLM provider '{}': {}", providerType, ex.getMessage());
            }
        }
        log.info("LlmProviderFactory initialized with {} dynamic providers: {}",
                dynamicProviders.size(), dynamicProviders.keySet());
    }

    private ModelProvider createProvider(String providerType, LlmProviderConfig config) {
        String type = config.getType() == null ? "openai-compatible" : config.getType().trim().toLowerCase();
        switch (type) {
            case "openai-compatible":
            case "ollama":      // Ollama 0.1.30+ 兼容 OpenAI /v1/chat/completions
            case "qianwen":     // 通义 DashScope 兼容模式
            case "deepseek":
            case "kimi":
            case "zhipu":
            case "minimax":
            case "doubao":
            case "yi":
            case "baichuan":
            case "step":
            case "sense":
                return new OpenAICompatibleProvider(providerType, config, objectMapper);
            default:
                log.warn("Unknown LLM provider type '{}' for '{}' — skip (only openai-compatible supported in v0.3, wenxin/vllm see roadmap)",
                        type, providerType);
                return null;
        }
    }

    /**
     * 获取所有动态注册的 ModelProvider（按配置顺序）。
     */
    public List<ModelProvider> getDynamicProviders() {
        return new ArrayList<ModelProvider>(dynamicProviders.values());
    }

    /**
     * 按 providerType 取单个 Provider，未找到返回 null。
     */
    public ModelProvider getProvider(String providerType) {
        if (providerType == null) return null;
        return dynamicProviders.get(providerType.toUpperCase());
    }

    /**
     * 获取所有动态 Provider 名单（按注册顺序）。
     */
    public List<String> listProviderTypes() {
        return Collections.unmodifiableList(new ArrayList<String>(dynamicProviders.keySet()));
    }
}
