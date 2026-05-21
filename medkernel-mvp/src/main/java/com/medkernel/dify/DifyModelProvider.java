package com.medkernel.dify;

import com.medkernel.llm.ModelProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DifyModelProvider implements ModelProvider {

    private final DifyService difyService;
    private final DifyProperties difyProperties;

    public DifyModelProvider(DifyService difyService, DifyProperties difyProperties) {
        this.difyService = difyService;
        this.difyProperties = difyProperties;
    }

    @Override
    public String getProviderType() {
        return "DIFY";
    }

    @Override
    public boolean isReady() {
        return difyProperties.ready();
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> request) {
        return difyService.runWorkflow(request);
    }

    @Override
    public String getProviderName() {
        return "DifyWorkflowProvider";
    }
}
