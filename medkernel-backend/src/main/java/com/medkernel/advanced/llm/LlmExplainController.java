package com.medkernel.advanced.llm;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advanced/llm/explain")
public class LlmExplainController {

    @GetMapping("/{decisionId}")
    public LlmExplain explain(@PathVariable String decisionId) {
        return new LlmExplain(
            decisionId,
            "AMI 患者 90 分钟内 PCI 建议",
            0.96,
            "高",
            List.of(
                new LlmExplain.EvidenceSource("guideline", "中国急性心肌梗死诊疗指南 2023", "§4.2.1", "2023-06"),
                new LlmExplain.EvidenceSource("paper", "NEJM 2023, PCI vs Fibrinolysis in STEMI", "Table 3", "2023-04"),
                new LlmExplain.EvidenceSource("kb", "MedKernel 路径库 · 胸痛 AMI v2.3", "node:emergency-pci", "2026-05-20")
            ),
            "训练数据：2018-01 ~ 2025-12 国内三甲 STEMI 病例 + 国际公开指南；不含 2026 年数据",
            "Ollama · 通义 Q2.5-7b（内网，符合个保法）",
            "本结论由 AI 生成，必须经医师确认后才能进入医嘱 / 病历（生成式 AI 管理办法 §7）"
        );
    }
}
