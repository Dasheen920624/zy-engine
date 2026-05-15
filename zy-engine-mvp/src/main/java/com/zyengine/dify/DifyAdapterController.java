package com.zyengine.dify;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dify")
public class DifyAdapterController {
    @PostMapping("/workflows/run")
    public ApiResult<Map<String, Object>> run(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workflow_code", request.get("workflow_code"));
        result.put("status", "DEGRADED");
        result.put("message", "MVP模式未启用真实Dify调用，返回降级结果。");
        return ApiResult.success(result);
    }
}

