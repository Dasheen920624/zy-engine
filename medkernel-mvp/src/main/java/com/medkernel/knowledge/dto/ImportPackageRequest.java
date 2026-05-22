package com.medkernel.knowledge.dto;

/**
 * 导入知识包请求 DTO。
 */
public class ImportPackageRequest {

    private String conflictStrategy;

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }
}
