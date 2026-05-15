package com.zyengine.terminology;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/terminology")
public class TerminologyController {
    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @PostMapping("/normalize")
    public ApiResult<Map<String, Object>> normalize(@RequestBody Map<String, Object> request) {
        return ApiResult.success(terminologyService.normalize(request));
    }
}
