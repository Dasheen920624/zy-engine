package com.medkernel.engine.rule;

/**
 * 创建规则的出参（GA-ENG-API-05）：返回新建规则与初始草稿版本的业务键、状态及 traceId。
 */
public record RuleCreateResponse(
    String ruleId,
    String versionId,
    RuleDefinitionStatus status,
    String traceId
) {}
