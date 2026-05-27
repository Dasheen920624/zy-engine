package com.medkernel.engine.recommendation;

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
@RequestMapping("/api/v1/engine/recommendations")
@DataScope(requireTenant = true)
public class RecommendationEngineController {

    private final RecommendationEngineService service;

    public RecommendationEngineController(RecommendationEngineService service) {
        this.service = service;
    }

    @PostMapping("/triggers")
    @PreAuthorize("@perm.has('recommendation.write')")
    public ResponseEntity<ApiResult<RecommendationTriggerResponse>> trigger(
            @RequestBody @Valid RecommendationTriggerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.trigger(request)));
    }

    @GetMapping("/cards")
    @PreAuthorize("@perm.has('recommendation.read')")
    public ApiResult<PageResponse<RecommendationCard>> cards(
            @RequestParam(required = false) RecommendationCardStatus status,
            @RequestParam(required = false) RecommendationRiskLevel riskLevel,
            @RequestParam(required = false) String scenarioCode,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listCards(
            new RecommendationCardFilter(status, riskLevel, scenarioCode, patientId),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/cards/{cardId}")
    @PreAuthorize("@perm.has('recommendation.read')")
    public ApiResult<RecommendationCardDetailResponse> cardDetail(@PathVariable String cardId) {
        return ApiResult.ok(service.cardDetail(cardId));
    }

    @GetMapping("/cards/{cardId}/sources")
    @PreAuthorize("@perm.has('recommendation.read')")
    public ApiResult<List<RecommendationSource>> sources(@PathVariable String cardId) {
        return ApiResult.ok(service.sources(cardId));
    }

    @PostMapping("/cards/{cardId}/feedback")
    @PreAuthorize("@perm.has('recommendation.accept')")
    public ApiResult<RecommendationFeedbackResponse> feedback(
            @PathVariable String cardId,
            @RequestBody @Valid RecommendationFeedbackRequest request) {
        return ApiResult.ok(service.feedback(cardId, request));
    }

    @GetMapping("/fatigue-signals")
    @PreAuthorize("@perm.has('recommendation.read')")
    public ApiResult<PageResponse<RecommendationFatigueSignal>> fatigueSignals(
            @RequestParam(required = false) String fatigueKey,
            @RequestParam(required = false) RecommendationFatigueSignalType signalType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.fatigueSignals(
            new RecommendationFatigueSignalFilter(fatigueKey, signalType),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/triggers/{triggerId}/diagnose")
    @PreAuthorize("@perm.has('recommendation.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String triggerId) {
        return ApiResult.ok(service.diagnose(triggerId));
    }
}
