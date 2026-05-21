package com.medkernel.llm;

import java.util.Map;

public interface ModelProvider {
    String getProviderType();

    boolean isReady();

    Map<String, Object> invoke(Map<String, Object> request);

    String getProviderName();
}
