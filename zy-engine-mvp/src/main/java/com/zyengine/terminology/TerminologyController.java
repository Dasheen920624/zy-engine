package com.zyengine.terminology;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    @PostMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> importMappings(@RequestBody Object request) {
        return ApiResult.success(terminologyService.importMappings(request));
    }

    @GetMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> listMappings() {
        return ApiResult.success(terminologyService.listMappings());
    }

    @GetMapping("/mappings/{sourceSystem}/{sourceCode}")
    public ApiResult<Map<String, Object>> getMapping(@PathVariable String sourceSystem,
                                                     @PathVariable String sourceCode,
                                                     @RequestParam String conceptType) {
        return ApiResult.success(terminologyService.getMapping(sourceSystem, sourceCode, conceptType));
    }
}
