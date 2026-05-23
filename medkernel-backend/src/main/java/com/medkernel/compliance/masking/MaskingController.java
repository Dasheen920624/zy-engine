package com.medkernel.compliance.masking;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance/masking")
public class MaskingController {

    private final MaskingService masking;

    public MaskingController(MaskingService masking) {
        this.masking = masking;
    }

    @GetMapping("/profiles")
    public Map<String, String> profiles() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("DEV", "开发：身份证/手机全脱，姓名半脱");
        m.put("TEST", "测试：身份证/手机/姓名全脱");
        m.put("TRAINING", "培训：身份证/手机全脱，姓名→虚拟测试患者");
        m.put("EXPORT", "数据出境：全字段假名化 SM3 hash（个保法第 51 条）");
        return m;
    }

    @PostMapping("/apply")
    public Map<String, String> apply(@RequestBody Map<String, String> raw,
                                      @RequestParam(defaultValue = "DEV") String profile) {
        MaskingProfile p = MaskingProfile.valueOf(profile.toUpperCase());
        return masking.maskRecord(raw, p);
    }
}
