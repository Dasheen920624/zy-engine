package com.zyengine.system;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("service", "zy-engine-mvp");
        data.put("status", "UP");
        data.put("jdk", "1.8-compatible");
        return ApiResult.success(data);
    }
}

