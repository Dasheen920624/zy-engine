package com.medkernel.rule;

/**
 * 规则动作模式枚举
 * 定义规则命中后的交互方式
 */
public enum ActionMode {
    /**
     * 通知模式：仅展示信息，不阻断操作
     */
    NOTICE,
    
    /**
     * 软提醒模式：展示提醒，但允许用户继续操作
     */
    SOFT,
    
    /**
     * 阻断模式：必须确认才能继续，用于高风险医疗安全场景
     * 必须配合 decision_required=true 使用
     */
    BLOCK
}