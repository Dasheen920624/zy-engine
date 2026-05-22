package com.medkernel.adapter;

import java.util.List;
import java.util.Map;

public interface AdapterMockDataProvider {
    boolean supports(String adapterCode, String queryCode);
    List<Map<String, Object>> provideRows(String adapterCode, String queryCode, Map<String, Object> params);
}
