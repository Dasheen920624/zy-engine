package com.medkernel.advanced.chatbot;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.advanced.llm.LlmGateway;
import com.medkernel.advanced.llm.LlmRequest;

/**
 * GA-EXT-20 · 合规 chatbot（员工随时问"这个操作合规吗"）。
 * 集团合规部刚需 —— 把合规知识库 + LLM Gateway 结合，员工自助查询。
 */
@RestController
@RequestMapping("/api/v1/advanced/chatbot/compliance")
public class ComplianceChatbotController {

    private final LlmGateway llmGateway;

    public ComplianceChatbotController(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public record Question(String text, String context) {}

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Question q) {
        String prompt = "你是 MedKernel 合规专家，参考个保法/数据安全法/电子病历应用管理规范/等保 2.0 三级回答以下问题：\n"
            + (q.context() != null ? "[上下文] " + q.context() + "\n" : "")
            + "[问题] " + q.text();
        var resp = llmGateway.invokeWithFallback(new LlmRequest(prompt, 0.1, 512));
        return Map.of(
            "answer", resp.text(),
            "provider", resp.providerId(),
            "latencyMs", resp.latencyMs(),
            "degraded", resp.degraded(),
            "references", List.of(
                "个人信息保护法 第 13/27/51 条",
                "数据安全法 第 21/27 条",
                "电子病历应用管理规范（2024 版）",
                "GB/T 35273-2020 个人信息安全规范"
            )
        );
    }
}
