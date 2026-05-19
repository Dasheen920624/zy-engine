package com.medkernel.rule;

/**
 * 规则动作模式枚举。
 * <ul>
 *   <li>{@code NOTICE} — 信息提示，不阻断操作</li>
 *   <li>{@code SOFT} — 软提醒，可跳过但需确认</li>
 *   <li>{@code BLOCK} — 硬阻断，必须满足条件才能继续（如填写理由 + 知情同意）</li>
 * </ul>
 *
 * <p>发布前校验：BLOCK 类型必须配合 {@code decision_required=true}。</p>
 */
public enum ActionMode {
    /** 信息提示，不阻断操作 */
    NOTICE,
    /** 软提醒，可跳过但需确认 */
    SOFT,
    /** 硬阻断，必须满足条件才能继续 */
    BLOCK;

    /**
     * 判断给定字符串是否为合法的 ActionMode。
     */
    public static boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (ActionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析字符串为 ActionMode，忽略大小写。无效值返回 null。
     */
    public static ActionMode parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (ActionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
