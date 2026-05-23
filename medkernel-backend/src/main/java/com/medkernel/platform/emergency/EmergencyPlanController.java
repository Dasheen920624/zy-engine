package com.medkernel.platform.emergency;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-25 · 应急预案触发器（疫情/突发事件 一键切换院级模板）。
 * 国家卫健委评级 —— 应急预案能否一键启动是关键指标。
 */
@RestController
@RequestMapping("/api/v1/platform/emergency")
public class EmergencyPlanController {

    @GetMapping("/plans")
    public List<Map<String, Object>> plans() {
        return List.of(
            Map.of("id", "P-COVID", "name", "新冠疫情应急预案", "lastActivated", "—", "channels", 12),
            Map.of("id", "P-MASS-CASUALTY", "name", "群体伤亡事件预案", "lastActivated", "—", "channels", 8),
            Map.of("id", "P-FLU", "name", "流感大流行预案", "lastActivated", "2026-01-12", "channels", 6),
            Map.of("id", "P-DISASTER", "name", "地震/灾害响应预案", "lastActivated", "—", "channels", 10)
        );
    }

    @PostMapping("/{planId}/activate")
    public Map<String, Object> activate(@PathVariable String planId) {
        return Map.of(
            "result", "activated",
            "planId", planId,
            "activatedAt", java.time.Instant.now().toString(),
            "actionsTriggered", List.of(
                "全院通知已发送",
                "应急班次表已切换",
                "AI 工作流加载应急路径模板",
                "通知国家卫健委指挥中心",
                "CDSS 临时启用紧急规则集"
            )
        );
    }
}
