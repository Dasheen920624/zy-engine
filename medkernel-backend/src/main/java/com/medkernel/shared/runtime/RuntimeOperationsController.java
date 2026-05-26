package com.medkernel.shared.runtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;

@RestController
@RequestMapping("/api/v1/system")
public class RuntimeOperationsController {

    private final RuntimeOperationsService service;

    public RuntimeOperationsController(RuntimeOperationsService service) {
        this.service = service;
    }

    @GetMapping("/operations")
    public ApiResult<RuntimeOperationsSnapshot> operations() {
        return ApiResult.ok(service.snapshot());
    }
}
