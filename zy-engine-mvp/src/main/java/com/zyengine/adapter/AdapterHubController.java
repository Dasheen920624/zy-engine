package com.zyengine.adapter;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/adapters")
public class AdapterHubController {
    private final AdapterHubService adapterHubService;

    public AdapterHubController(AdapterHubService adapterHubService) {
        this.adapterHubService = adapterHubService;
    }

    @PostMapping("/query")
    public ApiResult<Map<String, Object>> query(@RequestBody Map<String, Object> request) {
        return ApiResult.success(adapterHubService.query(request));
    }
}
