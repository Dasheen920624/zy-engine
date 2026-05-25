package com.medkernel.engine.knowledge;

/**
 * 知识资产版本的风险等级。对应 {@code knowledge_asset_version.risk_level} CHECK 约束。
 *
 * <p>配合宪法 §6 的体验门禁：
 * <ul>
 *   <li>{@link #LOW}：可批量审核激活</li>
 *   <li>{@link #MEDIUM}：单条二次确认</li>
 *   <li>{@link #HIGH}：双审、强提醒、必须留下激活说明</li>
 * </ul>
 *
 * <p>注意与 {@link com.medkernel.engine.security.PermissionCode.Risk} 区分：
 * 后者是权限动作风险（执行某操作的危险性），本枚举是知识内容风险（错误激活带来的临床危害）。
 */
public enum KnowledgeRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
