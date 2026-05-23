package com.medkernel.llm;

import com.medkernel.common.TraceContext;
import com.medkernel.knowledge.AiKnowledgeJobService;
import com.medkernel.knowledge.AiModelCallLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelGatewayService {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayService.class);

    /**
     * 默认降级链（ADR-0013 去 Dify 化策略）：
     * - 国产大模型优先（QIANWEN/DEEPSEEK 通过 OpenAI 兼容协议直连，不依赖 Dify）
     * - Ollama 本地兜底（医院内网部署可选）
     * - LOCAL 规则降级（永远可用）
     * - 仅 WORKFLOW 调用类型走 Dify（复杂多步流程场景）
     */
    private static final Map<String, String> DEFAULT_DEGRADATION_CHAINS = new LinkedHashMap<String, String>();

    static {
        DEFAULT_DEGRADATION_CHAINS.put("RESEARCH", "QIANWEN,DEEPSEEK,OLLAMA_LOCAL,LOCAL");
        DEFAULT_DEGRADATION_CHAINS.put("EXTRACT", "DEEPSEEK,QIANWEN,OLLAMA_LOCAL,LOCAL");
        DEFAULT_DEGRADATION_CHAINS.put("EMBEDDING", "QIANWEN,LOCAL");
        DEFAULT_DEGRADATION_CHAINS.put("RERANK", "LOCAL");
        DEFAULT_DEGRADATION_CHAINS.put("CRITIC", "DEEPSEEK,QIANWEN,LOCAL");
        DEFAULT_DEGRADATION_CHAINS.put("WORKFLOW", "DIFY,LOCAL");
    }

    private final List<ModelProvider> staticProviders;
    private final LlmProviderFactory llmProviderFactory;
    private final AiKnowledgeJobService jobService;
    private final ModelGatewayProperties properties;
    private final com.medkernel.system.BusinessOperationMetrics businessMetrics;

    private final Map<String, ModelProvider> providerMap = new LinkedHashMap<String, ModelProvider>();
    private final List<ModelProvider> providers = new ArrayList<ModelProvider>();

    public ModelGatewayService(List<ModelProvider> staticProviders,
                               LlmProviderFactory llmProviderFactory,
                               AiKnowledgeJobService jobService,
                               ModelGatewayProperties properties,
                               com.medkernel.system.BusinessOperationMetrics businessMetrics) {
        this.staticProviders = staticProviders != null ? staticProviders : Collections.<ModelProvider>emptyList();
        this.llmProviderFactory = llmProviderFactory;
        this.jobService = jobService;
        this.properties = properties;
        this.businessMetrics = businessMetrics;
    }

    @PostConstruct
    public void init() {
        // 1) 注册静态 Provider（DifyModelProvider / LocalModelProvider，由 @Component 自动注入）
        for (ModelProvider provider : staticProviders) {
            providerMap.put(provider.getProviderType(), provider);
            providers.add(provider);
        }
        // 2) 注册动态 Provider（LlmProviderFactory 根据 application.yml 配置创建的国产大模型）
        if (llmProviderFactory != null) {
            for (ModelProvider provider : llmProviderFactory.getDynamicProviders()) {
                // 动态 Provider 优先级高，覆盖同名静态 Provider（理论上不会重名）
                providerMap.put(provider.getProviderType(), provider);
                providers.add(provider);
            }
        }
        log.info("ModelGateway initialized with {} providers (static + dynamic): {}",
                providerMap.size(), providerMap.keySet());
    }

    public Map<String, Object> invoke(String callType, Map<String, Object> request) {
        if (!properties.isEnabled()) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("status", "DISABLED");
            result.put("call_type", callType);
            result.put("message", "Model gateway is disabled");
            result.put("trace_id", TraceContext.getTraceId());
            return result;
        }

        long start = System.currentTimeMillis();
        List<String> chain = getDegradationChainList(callType);

        Map<String, Object> lastResult = null;
        String primaryProviderType = chain.isEmpty() ? "UNKNOWN" : chain.get(0);
        boolean fallbackUsed = false;
        String fallbackProvider = null;
        String fallbackModel = null;

        for (int i = 0; i < chain.size(); i++) {
            String providerType = chain.get(i);
            ModelProvider provider = providerMap.get(providerType);

            if (provider == null) {
                log.warn("Provider type '{}' not found in registered providers, skipping", providerType);
                continue;
            }

            if (!provider.isReady()) {
                log.warn("Provider '{}' ({}) is not ready, trying next in chain", providerType, provider.getProviderName());
                if (i > 0 || chain.size() > 1) {
                    fallbackUsed = true;
                    fallbackProvider = providerType;
                    fallbackModel = provider.getProviderName();
                }
                continue;
            }

            try {
                Map<String, Object> result = provider.invoke(request);
                long elapsed = System.currentTimeMillis() - start;
                result.put("call_type", callType);
                result.put("trace_id", TraceContext.getTraceId());
                result.put("elapsed_ms", elapsed);

                if (i > 0) {
                    fallbackUsed = true;
                    fallbackProvider = providerType;
                    fallbackModel = provider.getProviderName();
                    result.put("fallback_used", "true");
                    result.put("fallback_provider", fallbackProvider);
                    result.put("fallback_model", fallbackModel);
                    result.put("primary_provider", primaryProviderType);
                }

                logModelCall(callType, provider, result, elapsed, fallbackUsed, fallbackProvider, fallbackModel, request);

                // OPS-003: 记录 LLM 调用指标
                businessMetrics.recordLlmCall(elapsed * 1_000_000L, false);

                return result;
            } catch (Exception ex) {
                long elapsed = System.currentTimeMillis() - start;
                log.error("Provider '{}' ({}) invocation failed: {}", providerType, provider.getProviderName(), ex.getMessage());
                lastResult = buildErrorResult(callType, providerType, provider.getProviderName(), elapsed, ex);
                if (i > 0 || chain.size() > 1) {
                    fallbackUsed = true;
                    fallbackProvider = providerType;
                    fallbackModel = provider.getProviderName();
                }
                logModelCall(callType, provider, lastResult, elapsed, fallbackUsed, fallbackProvider, fallbackModel, request);

                // OPS-003: 记录 LLM 调用错误指标
                businessMetrics.recordLlmCall(elapsed * 1_000_000L, true);
            }
        }

        if (lastResult != null) {
            return lastResult;
        }

        Map<String, Object> noProviderResult = new LinkedHashMap<String, Object>();
        noProviderResult.put("status", "NO_PROVIDER_AVAILABLE");
        noProviderResult.put("call_type", callType);
        noProviderResult.put("message", "No available provider for call type: " + callType);
        noProviderResult.put("trace_id", TraceContext.getTraceId());
        noProviderResult.put("elapsed_ms", System.currentTimeMillis() - start);
        return noProviderResult;
    }

    public List<Map<String, Object>> listProviders() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ModelProvider provider : providers) {
            Map<String, Object> info = new LinkedHashMap<String, Object>();
            info.put("provider_type", provider.getProviderType());
            info.put("provider_name", provider.getProviderName());
            info.put("ready", provider.isReady());
            info.put("status", provider.isReady() ? "READY" : "UNAVAILABLE");
            list.add(info);
        }
        return list;
    }

    public Map<String, Object> getDegradationChain(String callType) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("call_type", callType);
        String chainStr = resolveChain(callType);
        result.put("chain", chainStr);
        result.put("providers", getDegradationChainList(callType));
        return result;
    }

    public Map<String, Object> getProviderStatus(String providerType) {
        ModelProvider provider = providerMap.get(providerType);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("provider_type", providerType);
        if (provider == null) {
            result.put("registered", false);
            result.put("ready", false);
            result.put("status", "NOT_FOUND");
            return result;
        }
        result.put("registered", true);
        result.put("provider_name", provider.getProviderName());
        result.put("ready", provider.isReady());
        result.put("status", provider.isReady() ? "READY" : "UNAVAILABLE");
        return result;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private List<String> getDegradationChainList(String callType) {
        String chainStr = resolveChain(callType);
        if (chainStr == null || chainStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> chain = new ArrayList<String>();
        for (String part : chainStr.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                chain.add(trimmed);
            }
        }
        return chain;
    }

    private String resolveChain(String callType) {
        if (callType == null) {
            return "";
        }
        String configured = properties.getDegradationChains().get(callType);
        if (configured != null && !configured.trim().isEmpty()) {
            return configured;
        }
        String defaultChain = DEFAULT_DEGRADATION_CHAINS.get(callType);
        return defaultChain != null ? defaultChain : "";
    }

    private Map<String, Object> buildErrorResult(String callType, String providerType,
                                                  String providerName, long elapsedMs, Exception ex) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "ERROR");
        result.put("call_type", callType);
        result.put("provider", providerType);
        result.put("provider_name", providerName);
        result.put("message", "Provider invocation failed: " + ex.getMessage());
        result.put("error_code", "PROVIDER_ERROR");
        result.put("trace_id", TraceContext.getTraceId());
        result.put("elapsed_ms", elapsedMs);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void logModelCall(String callType, ModelProvider provider, Map<String, Object> result,
                              long elapsedMs, boolean fallbackUsed, String fallbackProvider,
                              String fallbackModel, Map<String, Object> request) {
        try {
            AiModelCallLog callLog = new AiModelCallLog();
            callLog.setCallType(callType);
            callLog.setModelProvider(provider.getProviderType());
            callLog.setModelName(provider.getProviderName());
            callLog.setCallStatus(stringOf(result.get("status"), "UNKNOWN"));
            callLog.setErrorCode(stringOf(result.get("error_code"), null));
            callLog.setErrorMessage(stringOf(result.get("message"), null));
            callLog.setElapsedMs((int) Math.min(elapsedMs, Integer.MAX_VALUE));
            callLog.setTraceId(TraceContext.getTraceId());
            callLog.setFallbackUsed(fallbackUsed ? "true" : "false");
            callLog.setFallbackProvider(fallbackProvider);
            callLog.setFallbackModel(fallbackModel);

            if (request != null) {
                callLog.setPatientId(stringOf(request.get("patient_id"), null));
                callLog.setEncounterId(stringOf(request.get("encounter_id"), null));
                Object tenantId = request.get("tenant_id");
                if (tenantId instanceof Number) {
                    callLog.setTenantId(((Number) tenantId).longValue());
                }
            }

            jobService.logModelCall(callLog);
        } catch (Exception ex) {
            log.error("Failed to log model call: {}", ex.getMessage());
        }
    }

    private String stringOf(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
