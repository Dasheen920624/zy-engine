package com.medkernel.advanced.llm;

import org.springframework.stereotype.Component;

/** 内网首选：Ollama 本地大模型 mock 实现。 */
@Component
public class OllamaMockProvider implements LlmProvider {
    public String id() { return "ollama-qwen2.5-7b"; }
    public String name() { return "Ollama · 通义 Q2.5-7b（内网首选）"; }
    public boolean isLocal() { return true; }
    public boolean isHealthy() { return true; }

    @Override
    public LlmResponse chat(LlmRequest request) {
        long start = System.currentTimeMillis();
        String text = "（Ollama mock 回复）prompt 长度 " + request.prompt().length()
            + " · 温度 " + request.temperature()
            + "\n本响应由内网本地大模型生成，不出网，符合个保法 + 数据出境合规。";
        long latency = System.currentTimeMillis() - start;
        return new LlmResponse(text, id(), request.prompt().length() / 4, latency, false);
    }
}
