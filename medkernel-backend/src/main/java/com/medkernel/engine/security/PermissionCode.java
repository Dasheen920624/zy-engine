package com.medkernel.engine.security;

import java.util.Arrays;
import java.util.Optional;

/**
 * MedKernel v1.0 GA · 统一权限编码枚举。
 *
 * <p>命名约定：{@code <域>.<动作>}，小写点号分隔。
 * 域大致对应一级业务模块（org/tenant/rule/pathway/knowledge/recommendation/evaluation/followup/package/audit/system 等）。
 *
 * <p>新增权限只能在末尾追加，不得删除已发布权限（避免 DB 已绑定关系失效）；
 * 废弃权限标 {@code @Deprecated} 但保留枚举值。
 *
 * <p>风险等级用于产品宪法第 6 条的 6 态体验和"高风险逐条确认"门禁；
 * 高风险动作不允许批量；中风险要二次确认；低风险可批量。
 */
public enum PermissionCode {

    // ─── 组织（GA-ENG-BASE-01）────────────────────────────────────
    ORG_READ("org.read", Risk.LOW, "查看组织树"),
    ORG_WRITE("org.write", Risk.MEDIUM, "新增 / 修改组织单元"),
    ORG_PUBLISH("org.publish", Risk.HIGH, "激活 / 暂停 / 归档组织单元"),

    // ─── 租户与配置包（GA-ENG-API-10）────────────────────────────
    TENANT_READ("tenant.read", Risk.LOW, "查看租户与生命周期"),
    TENANT_WRITE("tenant.write", Risk.HIGH, "开通 / 关闭租户"),
    PACKAGE_READ("package.read", Risk.LOW, "查看配置包 / 知识包"),
    PACKAGE_PUBLISH("package.publish", Risk.HIGH, "灰度 / 全量发布配置包"),
    PACKAGE_ROLLBACK("package.rollback", Risk.HIGH, "回滚配置包"),

    // ─── 知识资产（GA-ENG-API-03 / GA-ENG-KNOW-01/02）─────────
    KNOWLEDGE_READ("knowledge.read", Risk.LOW, "查看知识资产 / 来源 / 候选"),
    KNOWLEDGE_WRITE("knowledge.write", Risk.MEDIUM, "新增 / 修改知识候选草稿与来源登记"),
    KNOWLEDGE_REVIEW("knowledge.review", Risk.MEDIUM, "审核 AI 候选知识"),
    KNOWLEDGE_PUBLISH("knowledge.publish", Risk.HIGH, "激活新版知识并失效旧版"),
    KNOWLEDGE_WITHDRAW("knowledge.withdraw", Risk.HIGH, "紧急撤回已发布知识版本"),
    KNOWLEDGE_EXPORT("knowledge.export", Risk.MEDIUM, "异步导出知识资产 / 引用 / 历史"),

    // ─── 字典（GA-ENG-TERM-01）─────────────────────────────────
    TERM_READ("term.read", Risk.LOW, "查看标准字典 / 院内映射"),
    TERM_WRITE("term.write", Risk.MEDIUM, "修改字典 / 映射"),
    TERM_PUBLISH("term.publish", Risk.HIGH, "发布字典映射包"),

    // ─── 规则（GA-ENG-RULE-01）─────────────────────────────────
    RULE_READ("rule.read", Risk.LOW, "查看规则"),
    RULE_WRITE("rule.write", Risk.MEDIUM, "新增 / 修改规则草稿"),
    RULE_PUBLISH("rule.publish", Risk.HIGH, "灰度 / 全量发布规则"),

    // ─── 路径（GA-ENG-PATH-01）─────────────────────────────────
    PATHWAY_READ("pathway.read", Risk.LOW, "查看路径模板 / 患者路径"),
    PATHWAY_WRITE("pathway.write", Risk.MEDIUM, "编辑路径模板"),
    PATHWAY_PUBLISH("pathway.publish", Risk.HIGH, "发布路径模板"),

    // ─── CDSS / 推荐（GA-ENG-CDSS-01）──────────────────────────
    RECOMMENDATION_READ("recommendation.read", Risk.LOW, "查看推荐 / 提醒"),
    RECOMMENDATION_ACCEPT("recommendation.accept", Risk.MEDIUM, "采纳或拒绝推荐（医师权限）"),

    // ─── 评估质控（GA-ENG-EVAL-01）─────────────────────────────
    EVALUATION_READ("evaluation.read", Risk.LOW, "查看评估指标和结果"),
    EVALUATION_WRITE("evaluation.write", Risk.MEDIUM, "修改评估指标"),
    EVALUATION_PUBLISH("evaluation.publish", Risk.HIGH, "发布质控指标"),

    // ─── 审计与证据（GA-ENG-EVID-01）──────────────────────────
    AUDIT_READ("audit.read", Risk.LOW, "查看审计日志"),
    AUDIT_EXPORT("audit.export", Risk.MEDIUM, "导出审计快照 / 证据包"),

    // ─── 标准上下文（GA-ENG-API-01）────────────────────────────
    CONTEXT_READ("context.read", Risk.LOW, "查看标准上下文 snapshot"),
    CONTEXT_WRITE("context.write", Risk.MEDIUM, "创建标准上下文 snapshot"),

    // ─── 临床事件（GA-ENG-API-02）──────────────────────────────
    EVENT_READ("event.read", Risk.LOW, "查看临床事件"),
    EVENT_WRITE("event.write", Risk.MEDIUM, "创建 / 重放临床事件"),

    // ─── 系统运维（GA-ENG-BASE-07）─────────────────────────────
    SYSTEM_READ("system.read", Risk.LOW, "查看系统状态 / Provider"),
    SYSTEM_MANAGE("system.manage", Risk.HIGH, "运维操作（重启、密钥轮换、降级开关）"),

    // ─── 追加权限（保持已发布枚举顺序稳定）──────────────────────────
    RECOMMENDATION_WRITE("recommendation.write", Risk.MEDIUM, "创建推荐触发和候选提醒事实");

    private final String code;
    private final Risk risk;
    private final String displayName;

    PermissionCode(String code, Risk risk, String displayName) {
        this.code = code;
        this.risk = risk;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public Risk risk() {
        return risk;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<PermissionCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
            .filter(p -> p.code.equalsIgnoreCase(normalized))
            .findFirst();
    }

    /** 风险级别。配合产品宪法第 6 条 / §10.2 体验门禁使用。 */
    public enum Risk {
        /** 低风险 — 可批量，无需二次确认 */
        LOW,
        /** 中风险 — 单条二次确认 */
        MEDIUM,
        /** 高风险 — 强提醒 + 灰度 + 留证 */
        HIGH
    }
}
