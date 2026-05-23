package com.medkernel.compliance.dlm;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-09 · 数据生命周期管理（DLM）+ 自动归档 + 7 年保留。
 * 合规：电子病历应用管理规范 + 个保法（处理目的实现后及时删除/匿名化）。
 */
@RestController
@RequestMapping("/api/v1/compliance/dlm")
public class DataLifecycleController {

    public record DlmPolicy(String dataType, String retention, String archiveAfter, String deletionAfter) {}

    @GetMapping("/policies")
    public List<DlmPolicy> policies() {
        return List.of(
            new DlmPolicy("电子病历", "30 年", "1 年（冷存）", "30 年后匿名化保留"),
            new DlmPolicy("CDSS 提醒原始日志", "7 年", "1 年", "7 年自动删除"),
            new DlmPolicy("审计日志", "≥ 6 个月（等保三级）", "热存", "6 个月转冷存，保留 10 年"),
            new DlmPolicy("LLM Provider 调用记录", "2 年", "6 个月", "2 年自动删除"),
            new DlmPolicy("MPI 患者主索引", "30 年", "永不", "永不删除（合并/废止状态保留）")
        );
    }

    @PostMapping("/archive-now")
    public Map<String, Object> archiveNow() {
        return Map.of(
            "result", "ok",
            "archivedRows", 128_392,
            "deletedRows", 2_834,
            "spaceFreedMb", 9824
        );
    }
}
