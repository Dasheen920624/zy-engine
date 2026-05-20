package com.medkernel.config;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/config-packages/import")
public class ConfigPackageImportController {
    private final ConfigPackageImportService importService;
    private final OrganizationContextService organizationContextService;

    public ConfigPackageImportController(ConfigPackageImportService importService,
                                         OrganizationContextService organizationContextService) {
        this.importService = importService;
        this.organizationContextService = organizationContextService;
    }

    // Step 1: 上传配置包文件
    @PostMapping("/upload")
    public ApiResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                  HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, Object> result = importService.upload(file, orgContext.getTenantId());
        return ApiResult.success(result);
    }

    // Step 2: 校验包完整性
    @PostMapping("/validate")
    public ApiResult<Map<String, Object>> validate(@RequestBody Map<String, Object> request,
                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        Map<String, Object> result = importService.validate(request, orgContext.getTenantId());
        return ApiResult.success(result);
    }

    // Step 3: 来源审核检查
    @PostMapping("/source-check")
    public ApiResult<Map<String, Object>> sourceCheck(@RequestBody Map<String, Object> request,
                                                       HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        Map<String, Object> result = importService.sourceCheck(request, orgContext.getTenantId());
        // MISSING_SOURCE 阻断：当有缺失来源时返回错误
        if (Boolean.FALSE.equals(result.get("allow_publish"))) {
            return ApiResult.failure(ErrorCode.DATA_MISSING, "来源审核未通过，存在缺失来源");
        }
        return ApiResult.success(result);
    }

    // Step 4: 影响评估
    @PostMapping("/impact")
    public ApiResult<Map<String, Object>> impact(@RequestBody Map<String, Object> request,
                                                  HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        Map<String, Object> result = importService.impact(request, orgContext.getTenantId());
        return ApiResult.success(result);
    }

    // Step 5: 确认发布
    @PostMapping("/confirm")
    public ApiResult<Map<String, Object>> confirm(@RequestBody Map<String, Object> request,
                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        // 必须有 approved_by 和 reason
        String approvedBy = string(request.get("approved_by"));
        String reason = string(request.get("reason"));
        if (approvedBy == null || approvedBy.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "approved_by is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "reason is required");
        }
        Map<String, Object> result = importService.confirm(request, orgContext.getTenantId());
        return ApiResult.success(result);
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
