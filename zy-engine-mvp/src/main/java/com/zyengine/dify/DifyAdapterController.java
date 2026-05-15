package com.zyengine.dify;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dify")
public class DifyAdapterController {
    private final DifyService difyService;

    public DifyAdapterController(DifyService difyService) {
        this.difyService = difyService;
    }

    @PostMapping("/workflows/run")
    public ApiResult<Map<String, Object>> run(@RequestBody Map<String, Object> request) {
        return ApiResult.success(difyService.runWorkflow(request));
    }
}
