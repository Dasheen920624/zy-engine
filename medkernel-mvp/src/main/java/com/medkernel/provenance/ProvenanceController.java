package com.medkernel.provenance;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provenance")
public class ProvenanceController {
    private final ProvenanceService provenanceService;
    private final SourceCitationService sourceCitationService;
    private final SourceAssetBindingService sourceAssetBindingService;
    private final OrganizationContextService organizationContextService;

    public ProvenanceController(ProvenanceService provenanceService,
                                SourceCitationService sourceCitationService,
                                SourceAssetBindingService sourceAssetBindingService,
                                OrganizationContextService organizationContextService) {
        this.provenanceService = provenanceService;
        this.sourceCitationService = sourceCitationService;
        this.sourceAssetBindingService = sourceAssetBindingService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping("/source-documents")
    public ApiResult<Map<String, Object>> importSourceDocuments(@RequestBody Object request,
                                                                 HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(provenanceService.importDocuments(request));
    }

    @GetMapping("/source-documents")
    public ApiResult<List<Map<String, Object>>> listSourceDocuments(@RequestParam(required = false) String sourceType,
                                                                    @RequestParam(required = false) String source_type,
                                                                    @RequestParam(required = false) String reviewStatus,
                                                                    @RequestParam(required = false) String review_status,
                                                                    @RequestParam(required = false) String publisher,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String limit,
                                                                    HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("sourceType", sourceType == null ? source_type : sourceType);
        filters.put("reviewStatus", reviewStatus == null ? review_status : reviewStatus);
        filters.put("publisher", publisher);
        filters.put("keyword", keyword);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(provenanceService.listDocuments(filters));
    }

    @GetMapping("/source-documents/{documentCode}")
    public ApiResult<Map<String, Object>> getSourceDocument(@PathVariable String documentCode,
                                                            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(provenanceService.getDocument(documentCode, filters.get("tenantId")));
    }

    @PostMapping("/citations")
    public ApiResult<Map<String, Object>> importCitations(@RequestBody Object request,
                                                           HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(sourceCitationService.importCitations(request));
    }

    @GetMapping("/citations")
    public ApiResult<List<Map<String, Object>>> listCitations(
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String document_code,
            @RequestParam(required = false) String citationType,
            @RequestParam(required = false) String citation_type,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String limit,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("documentCode", documentCode == null ? document_code : documentCode);
        filters.put("citationType", citationType == null ? citation_type : citationType);
        filters.put("section", section);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceCitationService.listCitations(filters));
    }

    @GetMapping("/citations/{citationId}")
    public ApiResult<Map<String, Object>> getCitation(@PathVariable String citationId,
                                                      HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceCitationService.getCitation(citationId, filters.get("tenantId")));
    }

    @GetMapping("/source-documents/{documentCode}/citations")
    public ApiResult<List<Map<String, Object>>> getCitationsByDocument(
            @PathVariable String documentCode,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceCitationService.getCitationsByDocument(
                documentCode, filters.get("tenantId")));
    }

    @PostMapping("/bindings")
    public ApiResult<Map<String, Object>> importBindings(@RequestBody Object request,
                                                          HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(sourceAssetBindingService.importBindings(request));
    }

    @GetMapping("/bindings")
    public ApiResult<List<Map<String, Object>>> listBindings(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String asset_type,
            @RequestParam(required = false) String assetCode,
            @RequestParam(required = false) String asset_code,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String document_code,
            @RequestParam(required = false) String bindingType,
            @RequestParam(required = false) String binding_type,
            @RequestParam(required = false) String limit,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("assetType", assetType == null ? asset_type : assetType);
        filters.put("assetCode", assetCode == null ? asset_code : assetCode);
        filters.put("documentCode", documentCode == null ? document_code : documentCode);
        filters.put("bindingType", bindingType == null ? binding_type : bindingType);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceAssetBindingService.listBindings(filters));
    }

    @GetMapping("/bindings/{bindingId}")
    public ApiResult<Map<String, Object>> getBinding(@PathVariable String bindingId,
                                                     HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceAssetBindingService.getBinding(bindingId, filters.get("tenantId")));
    }

    @GetMapping("/assets/{assetType}/{assetCode}/bindings")
    public ApiResult<List<Map<String, Object>>> getBindingsByAsset(
            @PathVariable String assetType,
            @PathVariable String assetCode,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceAssetBindingService.getBindingsByAsset(
                assetType, assetCode, filters.get("tenantId")));
    }

    @GetMapping("/source-documents/{documentCode}/bindings")
    public ApiResult<List<Map<String, Object>>> getBindingsByDocument(
            @PathVariable String documentCode,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(sourceAssetBindingService.getBindingsByDocument(
                documentCode, filters.get("tenantId")));
    }
}
