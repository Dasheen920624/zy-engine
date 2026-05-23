package com.medkernel.clinical.udi;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * GA-EXT-02 · 医疗器械 UDI + 药品本位码自动校验。
 *
 * <p>UDI = Unique Device Identification（国家药监局 NMPA 强制要求）
 * 药品本位码 = 药智号（YBZ-*）
 *
 * <p>合规依据：
 * - 医疗器械唯一标识系统规则（NMPA 2019 年第 66 号公告）
 * - 国家药监局《药品追溯码标识规范》
 */
@RestController
@RequestMapping("/api/v1/clinical/udi")
public class UdiCheckController {

    public record CheckRequest(@NotBlank String code, String type) {}

    public record CheckResponse(
        String code, String type, Boolean valid,
        String name, String registrant, String expiry, String note
    ) {}

    @PostMapping("/check")
    public CheckResponse check(@Valid @RequestBody CheckRequest req) {
        // mock：根据前缀判定
        String prefix = req.code().substring(0, Math.min(req.code().length(), 4));
        if (prefix.startsWith("UDI")) {
            return new CheckResponse(req.code(), "UDI",
                true, "心脏支架 · ABC-5000", "微创医疗", "2028-06-30", "已在 NMPA 备案");
        }
        if (prefix.startsWith("YBZ")) {
            return new CheckResponse(req.code(), "YBZ",
                true, "阿司匹林 100mg", "拜耳", "2027-12-31", "本位码合法");
        }
        return new CheckResponse(req.code(), "unknown",
            false, null, null, null, "未识别的 UDI/YBZ 前缀");
    }

    @GetMapping("/recent")
    public List<Map<String, Object>> recent() {
        return List.of(
            Map.of("code", "UDI-CN-MC5000-001", "name", "心脏支架 ABC-5000", "checkedAt", "10:23", "valid", true),
            Map.of("code", "YBZ-86902101100012", "name", "阿司匹林 100mg", "checkedAt", "10:21", "valid", true),
            Map.of("code", "UDI-XYZ-未识别", "name", "—", "checkedAt", "10:15", "valid", false)
        );
    }
}
