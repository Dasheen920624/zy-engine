package com.zyengine.provenance;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provenance")
public class ProvenanceController {
    private final ProvenanceService provenanceService;

    public ProvenanceController(ProvenanceService provenanceService) {
        this.provenanceService = provenanceService;
    }

    @PostMapping("/source-documents")
    public ApiResult<Map<String, Object>> importSourceDocuments(@RequestBody Object request) {
        return ApiResult.success(provenanceService.importDocuments(request));
    }

    @GetMapping("/source-documents")
    public ApiResult<List<Map<String, Object>>> listSourceDocuments(@RequestParam(required = false) String tenantId,
                                                                    @RequestParam(required = false) String tenant_id,
                                                                    @RequestParam(required = false) String sourceType,
                                                                    @RequestParam(required = false) String source_type,
                                                                    @RequestParam(required = false) String reviewStatus,
                                                                    @RequestParam(required = false) String review_status,
                                                                    @RequestParam(required = false) String publisher,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("sourceType", sourceType == null ? source_type : sourceType);
        filters.put("reviewStatus", reviewStatus == null ? review_status : reviewStatus);
        filters.put("publisher", publisher);
        filters.put("keyword", keyword);
        filters.put("limit", limit);
        return ApiResult.success(provenanceService.listDocuments(filters));
    }

    @GetMapping("/source-documents/{documentCode}")
    public ApiResult<Map<String, Object>> getSourceDocument(@PathVariable String documentCode,
                                                            @RequestParam(required = false) String tenantId,
                                                            @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(provenanceService.getDocument(documentCode, tenantId == null ? tenant_id : tenantId));
    }
}
