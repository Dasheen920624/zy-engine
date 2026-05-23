package com.medkernel.clinical.mpi;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-06 · 多机构主索引联邦（集团多院区患者唯一身份）。
 * 集团医疗中枢的本质能力 —— 把"总院 / 东区 / 南区"3 个院区的同一患者识别合并。
 */
@RestController
@RequestMapping("/api/v1/clinical/mpi/federation")
public class FederationController {

    public record FederatedRecord(
        String federalMpi,
        String localMpi,
        String hospitalId,
        String hospitalName,
        String visitCount
    ) {}

    @GetMapping("/lookup")
    public List<FederatedRecord> lookup(@RequestParam String federalMpi) {
        // mock：返回 3 院区的本地记录
        return List.of(
            new FederatedRecord(federalMpi, "MPI-A-00892", "main", "总院", "23"),
            new FederatedRecord(federalMpi, "MPI-B-00125", "east", "东区分院", "4"),
            new FederatedRecord(federalMpi, "MPI-C-00038", "south", "南区分院", "1")
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
            "totalFederatedPatients", 894322,
            "hospitalsConnected", 3,
            "duplicateRatio", 0.063,
            "lastSyncAt", "30 秒前"
        );
    }
}
