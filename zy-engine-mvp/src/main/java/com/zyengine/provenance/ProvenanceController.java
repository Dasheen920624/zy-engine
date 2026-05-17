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
    private final SourceCitationService sourceCitationService;
    private final SourceAssetBindingService sourceAssetBindingService;

    public ProvenanceController(ProvenanceService provenanceService,
                                SourceCitationService sourceCitationService,
                                SourceAssetBindingService sourceAssetBindingService) {
        this.provenanceService = provenanceService;
        this.sourceCitationService = sourceCitationService;
        this.sourceAssetBindingService = sourceAssetBindingService;
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

    @PostMapping("/citations")
    public ApiResult<Map<String, Object>> importCitations(@RequestBody Object request) {
        return ApiResult.success(sourceCitationService.importCitations(request));
    }

    @GetMapping("/citations")
    public ApiResult<List<Map<String, Object>>> listCitations(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String tenant_id,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String document_code,
            @RequestParam(required = false) String citationType,
            @RequestParam(required = false) String citation_type,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("documentCode", documentCode == null ? document_code : documentCode);
        filters.put("citationType", citationType == null ? citation_type : citationType);
        filters.put("section", section);
        filters.put("limit", limit);
        return ApiResult.success(sourceCitationService.listCitations(filters));
    }

    @GetMapping("/citations/{citationId}")
    public ApiResult<Map<String, Object>> getCitation(@PathVariable String citationId,
                                                      @RequestParam(required = false) String tenantId,
                                                      @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(sourceCitationService.getCitation(citationId, tenantId == null ? tenant_id : tenantId));
    }

    @GetMapping("/source-documents/{documentCode}/citations")
    public ApiResult<List<Map<String, Object>>> getCitationsByDocument(
            @PathVariable String documentCode,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(sourceCitationService.getCitationsByDocument(
                documentCode, tenantId == null ? tenant_id : tenantId));
    }

    @PostMapping("/bindings")
    public ApiResult<Map<String, Object>> importBindings(@RequestBody Object request) {
        return ApiResult.success(sourceAssetBindingService.importBindings(request));
    }

    @GetMapping("/bindings")
    public ApiResult<List<Map<String, Object>>> listBindings(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String tenant_id,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String asset_type,
            @RequestParam(required = false) String assetCode,
            @RequestParam(required = false) String asset_code,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String document_code,
            @RequestParam(required = false) String bindingType,
            @RequestParam(required = false) String binding_type,
            @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("assetType", assetType == null ? asset_type : assetType);
        filters.put("assetCode", assetCode == null ? asset_code : assetCode);
        filters.put("documentCode", documentCode == null ? document_code : documentCode);
        filters.put("bindingType", bindingType == null ? binding_type : bindingType);
        filters.put("limit", limit);
        return ApiResult.success(sourceAssetBindingService.listBindings(filters));
    }

    @GetMapping("/bindings/{bindingId}")
    public ApiResult<Map<String, Object>> getBinding(@PathVariable String bindingId,
                                                     @RequestParam(required = false) String tenantId,
                                                     @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(sourceAssetBindingService.getBinding(bindingId, tenantId == null ? tenant_id : tenantId));
    }

    @GetMapping("/assets/{assetType}/{assetCode}/bindings")
    public ApiResult<List<Map<String, Object>>> getBindingsByAsset(
            @PathVariable String assetType,
            @PathVariable String assetCode,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(sourceAssetBindingService.getBindingsByAsset(
                assetType, assetCode, tenantId == null ? tenant_id : tenantId));
    }

    @GetMapping("/source-documents/{documentCode}/bindings")
    public ApiResult<List<Map<String, Object>>> getBindingsByDocument(
            @PathVariable String documentCode,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(sourceAssetBindingService.getBindingsByDocument(
                documentCode, tenantId == null ? tenant_id : tenantId));
    }
}
