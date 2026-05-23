package com.medkernel.tenant.rule;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tenant/rules")
public class RuleController {

    private static final List<Rule> SEED = List.of(
        new Rule("r1", "DRG MS-30 入组校验", "医保审核", "blocker", 12834L, "active"),
        new Rule("r2", "抗菌药 24h 限制使用", "医嘱安全", "warning", 4521L, "active"),
        new Rule("r3", "高血压三月内未随访", "质控规则", "info", 287L, "published"),
        new Rule("r4", "DRG 8 月新政（DIP 草案）", "医保审核", "blocker", 0L, "pending_review")
    );

    @GetMapping
    public List<Rule> list() {
        return SEED;
    }

    @PostMapping("/validate")
    public RuleValidateResult validate(@Valid @RequestBody RuleValidateRequest req) {
        List<RuleValidateResult.RuleHit> hits = List.of(
            new RuleValidateResult.RuleHit(
                "R-AB-024",
                "头孢曲松皮试缺失",
                "warning",
                "医嘱安全规则 R-AB-024（2023 抗菌药管理办法）",
                "先开皮试医嘱，皮试通过后再开 头孢曲松"
            ),
            new RuleValidateResult.RuleHit(
                "R-ASA-002",
                "阿司匹林与既往胃出血史",
                "info",
                "抗血小板用药指南 2024",
                "评估出血风险，必要时加 PPI"
            )
        );
        return new RuleValidateResult(req.patientMpi(), hits.size(), hits);
    }
}
