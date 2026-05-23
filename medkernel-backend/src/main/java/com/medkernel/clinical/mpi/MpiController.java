package com.medkernel.clinical.mpi;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-CLINICAL-01 · 患者主索引 API。
 */
@RestController
@RequestMapping("/api/v1/clinical/mpi")
public class MpiController {

    private static final List<MpiPatient> SEED = List.of(
        new MpiPatient("MPI-000123456", "张**", "男", 58, "1234", 2, "稳定"),
        new MpiPatient("MPI-000123457", "李**", "女", 42, "5678", 0, "稳定"),
        new MpiPatient("MPI-000123458", "王**", "男", 65, "9012", 1, "冲突待处理"),
        new MpiPatient("MPI-000123459", "刘**", "女", 33, "3456", 0, "稳定"),
        new MpiPatient("MPI-000123460", "陈**", "男", 71, "7890", 3, "稳定")
    );

    @GetMapping("/patients")
    public List<MpiPatient> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return SEED;
        }
        String lc = q.toLowerCase();
        return SEED.stream()
            .filter(p -> p.mpiId().toLowerCase().contains(lc) || p.idLast4().contains(lc))
            .toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("total", 1_248_322, "todayNew", 283, "conflicts", 12, "crossSiteMerged", 47);
    }
}
