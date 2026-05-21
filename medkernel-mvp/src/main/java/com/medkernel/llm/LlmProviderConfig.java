package com.medkernel.llm;

/**
 * 单个 LLM Provider 的配置数据类。
 *
 * <p>通过 {@code medkernel.model-gateway.providers.<KEY>} 在 application.yml 中配置。
 * 启动时由 {@link LlmProviderFactory} 根据每个配置项构造一个 {@link OpenAICompatibleProvider} 实例。
 *
 * <p>ADR-0013：去 Dify 化策略 — 直连国产大模型 + Ollama 本地化。
 */
public class LlmProviderConfig {

    /** Provider 类型：openai-compatible（默认，覆盖 80% 国产大模型 + Ollama）/ ollama / wenxin（v0.4 后） */
    private String type = "openai-compatible";

    /** Provider 接入 base URL（不含 /chat/completions 后缀）。例：https://api.deepseek.com/v1 */
    private String baseUrl;

    /** API Key — 必须通过环境变量注入，禁止明文写到 application.yml（不变量 A7） */
    private String apiKey;

    /** 模型名。例：deepseek-chat / qwen-plus / glm-4-plus / qwen2:7b（Ollama） */
    private String model;

    /** 请求超时（毫秒）— 云端 LLM 默认 8s，本地 Ollama 推荐 30s+ */
    private int timeoutMs = 8000;

    /** 最大输出 token 数 */
    private int maxTokens = 2000;

    /** 采样温度 — 医学场景推荐 0.3（更保守） */
    private double temperature = 0.3;

    /** 是否启用（默认 false — 必须显式配 apiKey 才启用） */
    private boolean enabled = false;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Ollama 本地推理需要更宽松的就绪判定 — apiKey 可以为空（本地无鉴权）。
     */
    public boolean isOllama() {
        return "ollama".equalsIgnoreCase(type);
    }

    /**
     * 配置就绪判定：enabled + baseUrl 非空 + (云端必须有 apiKey；Ollama 本地可以无 apiKey)
     */
    public boolean isReady() {
        if (!enabled) {
            return false;
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return false;
        }
        if (model == null || model.trim().isEmpty()) {
            return false;
        }
        if (!isOllama() && (apiKey == null || apiKey.trim().isEmpty())) {
            return false;
        }
        return true;
    }
}
