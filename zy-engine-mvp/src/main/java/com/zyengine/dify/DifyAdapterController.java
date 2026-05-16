package com.zyengine.dify;

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

    @PostMapping("/workflows")
    public ApiResult<List<DifyWorkflowTemplate>> importTemplates(@RequestBody Object request) {
        return ApiResult.success(difyService.importTemplates(request));
    }

    @GetMapping("/workflows")
    public ApiResult<List<DifyWorkflowTemplate>> listTemplates() {
        return ApiResult.success(difyService.listTemplates());
    }

    @GetMapping("/workflows/{workflowCode}")
    public ApiResult<DifyWorkflowTemplate> getTemplate(@PathVariable String workflowCode,
                                                       @RequestParam(required = false) String workflowVersion) {
        return ApiResult.success(difyService.getTemplate(workflowCode, workflowVersion));
    }
}
