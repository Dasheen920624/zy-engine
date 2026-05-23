package com.medkernel.tenant.pathway;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/pathways")
public class PathwayController {

    private static final List<PathwayTemplate> SEED = List.of(
        new PathwayTemplate("p1", "胸痛 AMI 急诊路径", "AMI / 急性心肌梗死", "心内 + 急诊", 12, "active"),
        new PathwayTemplate("p2", "急性脑卒中绿色通道", "卒中", "神经 + 急诊", 15, "published"),
        new PathwayTemplate("p3", "原发性高血压管理", "高血压", "全科 / 心内", 8, "pending_review"),
        new PathwayTemplate("p4", "2 型糖尿病规范化管理", "T2DM", "内分泌", 10, "draft")
    );

    @GetMapping
    public List<PathwayTemplate> list() {
        return SEED;
    }

    @PostMapping("/{id}/publish")
    public Map<String, Object> publish(@PathVariable String id) {
        return Map.of("id", id, "result", "ok", "stage", "canary", "rollout", "10%");
    }
}
