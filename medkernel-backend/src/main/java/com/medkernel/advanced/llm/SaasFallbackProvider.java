package com.medkernel.advanced.llm;

import org.springframework.stereotype.Component;

/** 外网备选：OpenAI Compatible / 文心 4.0 mock 实现（降级目标）。 */
@Component
public class SaasFallbackProvider implements LlmProvider {
    public String id() { return "saas-wenxin-4.0"; }
    public String name() { return "OpenAI Compatible · 文心 4.0（外网降级）"; }
    public boolean isLocal() { return false; }
    public boolean isHealthy() { return true; }

    @Override
    public LlmResponse chat(LlmRequest request) {
        long start = System.currentTimeMillis();
        String text = "（外网 SaaS mock 回复）prompt 长度 " + request.prompt().length()
            + "\n注意：调用前必须经数据脱敏中心 + 数据出境评估包。";
        long latency = System.currentTimeMillis() - start;
        return new LlmResponse(text, id(), request.prompt().length() / 4, latency, true);
    }
}
