package com.medkernel.engine.pathway;

import java.util.List;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;
import com.medkernel.shared.observability.DiagnoseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/engine/pathways")
@DataScope(requireTenant = true)
public class PathwayEngineController {

    private final PathwayEngineService service;

    public PathwayEngineController(PathwayEngineService service) {
        this.service = service;
    }

    @PostMapping("/packages")
    @PreAuthorize("@perm.has('pathway.write')")
    public ResponseEntity<ApiResult<SpecialtyPackageResponse>> createPackage(
            @RequestBody @Valid SpecialtyPackageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.createPackage(request)));
    }

    @GetMapping("/packages")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<PageResponse<SpecialtyPackage>> listPackages(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listPackages(new PageRequest(page, size, sort)));
    }

    @PostMapping("/templates")
    @PreAuthorize("@perm.has('pathway.write')")
    public ResponseEntity<ApiResult<PathwayTemplateDetailResponse>> createTemplate(
            @RequestBody @Valid PathwayTemplateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.createTemplate(request)));
    }

    @GetMapping("/templates")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<PageResponse<PathwayTemplate>> listTemplates(
            @RequestParam(required = false) PathwayTemplateStatus status,
            @RequestParam(required = false) String diseaseCode,
            @RequestParam(required = false) String packageId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listTemplates(
            new PathwayTemplateFilter(status, diseaseCode, packageId),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/templates/{templateId}")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<PathwayTemplateDetailResponse> templateDetail(@PathVariable String templateId) {
        return ApiResult.ok(service.templateDetail(templateId));
    }

    @PostMapping("/templates/{templateId}/publish")
    @PreAuthorize("@perm.has('pathway.publish')")
    public ApiResult<PathwayTemplatePublishResponse> publishTemplate(@PathVariable String templateId) {
        return ApiResult.ok(service.publishTemplate(templateId));
    }

    @PostMapping("/templates/{templateId}/simulate")
    @PreAuthorize("@perm.has('pathway.write')")
    public ApiResult<PathwaySimulationResponse> simulate(
            @PathVariable String templateId,
            @RequestBody(required = false) @Valid PathwaySimulateRequest request) {
        return ApiResult.ok(service.simulate(templateId, request));
    }

    @PostMapping("/patients")
    @PreAuthorize("@perm.has('pathway.write')")
    public ResponseEntity<ApiResult<PatientPathwayDetailResponse>> enterPatientPathway(
            @RequestBody @Valid PatientPathwayEnterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.enterPatientPathway(request)));
    }

    @GetMapping("/patients/{patientPathwayId}")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<PatientPathwayDetailResponse> patientDetail(@PathVariable String patientPathwayId) {
        return ApiResult.ok(service.patientDetail(patientPathwayId));
    }

    @PostMapping("/advance")
    @PreAuthorize("@perm.has('pathway.write')")
    public ApiResult<PathwayAdvanceResponse> advance(@RequestBody @Valid PathwayAdvanceRequest request) {
        return ApiResult.ok(service.advance(request));
    }

    @GetMapping("/{patientPathwayId}/clocks")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<List<ClinicalClock>> clocks(@PathVariable String patientPathwayId) {
        return ApiResult.ok(service.clocks(patientPathwayId));
    }

    @GetMapping("/patients/{patientPathwayId}/diagnose")
    @PreAuthorize("@perm.has('pathway.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String patientPathwayId) {
        return ApiResult.ok(service.diagnose(patientPathwayId));
    }
}
