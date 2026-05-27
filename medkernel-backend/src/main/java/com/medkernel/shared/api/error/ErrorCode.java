package com.medkernel.shared.api.error;

import java.util.Arrays;
import java.util.Optional;

/**
 * MedKernel v1.0 GA 统一错误码。
 *
 * <p>命名前缀：
 * <ul>
 *   <li>{@code ENG-API-*}：API 契约（参数、鉴权、HTTP 语义）</li>
 *   <li>{@code ENG-BASE-*}：基础底座（租户、组织、权限上下文）</li>
 *   <li>{@code ENG-SYS-*}：系统级（内部错误、下游故障）</li>
 *   <li>{@code ENG-OBS-*}：可观测性骨干（GA-ENG-OBS-01）</li>
 *   <li>{@code ENG-CONTEXT-*}、{@code ENG-EVENT-*} 等业务域</li>
 * </ul>
 *
 * <p>每个 ErrorCode 含 errorClass（INPUT/AUTH/DATA/EXTERNAL/INTERNAL）+ retryable，
 * 用于客户端决策与状态历史持久化分类。
 *
 * <p>code 一旦发布对客户端可见，禁止改名；只能新增或废弃（标记 @Deprecated 并保留）。
 */
public enum ErrorCode {

    OK("OK", 200, "操作成功", ErrorClass.INTERNAL, false), // OK 非错误，errorClass 占位

    BAD_REQUEST("ENG-API-001", 400, "请求参数无效", ErrorClass.INPUT, false),
    VALIDATION_FAILED("ENG-API-002", 400, "请求参数校验失败", ErrorClass.INPUT, false),
    UNAUTHORIZED("ENG-API-003", 401, "未授权访问", ErrorClass.AUTH, false),
    FORBIDDEN("ENG-API-004", 403, "无权限执行该操作", ErrorClass.AUTH, false),
    NOT_FOUND("ENG-API-005", 404, "资源不存在", ErrorClass.DATA, false),
    METHOD_NOT_ALLOWED("ENG-API-006", 405, "方法不允许", ErrorClass.INPUT, false),
    CONFLICT("ENG-API-007", 409, "资源冲突", ErrorClass.DATA, false),
    TOO_MANY_REQUESTS("ENG-API-008", 429, "请求过于频繁，请稍后重试", ErrorClass.INPUT, true),
    UNSUPPORTED_MEDIA_TYPE("ENG-API-009", 415, "不支持的请求媒体类型", ErrorClass.INPUT, false),

    TENANT_CONTEXT_MISSING("ENG-BASE-001", 400, "租户上下文缺失", ErrorClass.AUTH, false),
    TENANT_FORBIDDEN("ENG-BASE-002", 403, "无权访问该租户数据", ErrorClass.AUTH, false),
    DATA_SCOPE_DENIED("ENG-BASE-003", 403, "数据范围权限不足", ErrorClass.AUTH, false),

    INTERNAL_ERROR("ENG-SYS-001", 500, "服务内部错误", ErrorClass.INTERNAL, false),
    DOWNSTREAM_UNAVAILABLE("ENG-SYS-002", 503, "下游服务不可用", ErrorClass.EXTERNAL, true),
    MODEL_DEGRADED("ENG-SYS-003", 503, "AI 模型不可用，已降级到无模型基线", ErrorClass.EXTERNAL, true),

    ENG_CONTEXT_001("ENG-CONTEXT-001", 400, "上下文 schema 校验失败", ErrorClass.INPUT, false),
    ENG_CONTEXT_002("ENG-CONTEXT-002", 400, "包版本不存在", ErrorClass.DATA, false),
    ENG_CONTEXT_003("ENG-CONTEXT-003", 400, "标准上下文 quality_status=INVALID 被拒绝", ErrorClass.DATA, false),
    ENG_CONTEXT_004("ENG-CONTEXT-004", 409, "幂等键冲突且 payload 不一致", ErrorClass.DATA, false),

    ENG_OBS_001("ENG-OBS-001", 404, "payload 不存在或已归档", ErrorClass.DATA, false),
    ENG_OBS_002("ENG-OBS-002", 500, "状态历史写入失败", ErrorClass.INTERNAL, false),

    ENG_EVENT_001("ENG-EVENT-001", 400, "事件 schema 校验失败", ErrorClass.INPUT, false),
    ENG_EVENT_002("ENG-EVENT-002", 409, "事件 ID 已存在且 payload 不一致", ErrorClass.INPUT, false),
    ENG_EVENT_003("ENG-EVENT-003", 404, "临床事件不存在", ErrorClass.DATA, false),
    ENG_EVENT_004("ENG-EVENT-004", 503, "payload 存储不可用", ErrorClass.EXTERNAL, true),
    ENG_EVENT_005("ENG-EVENT-005", 500, "事件处理失败已进入死信", ErrorClass.INTERNAL, false),
    ENG_EVENT_006("ENG-EVENT-006", 400, "当前状态不允许重放", ErrorClass.INPUT, false),

    ENG_RULE_001("ENG-RULE-001", 400, "规则 DSL 校验失败", ErrorClass.INPUT, false),
    ENG_RULE_002("ENG-RULE-002", 404, "规则不存在", ErrorClass.DATA, false),
    ENG_RULE_003("ENG-RULE-003", 404, "规则版本不存在", ErrorClass.DATA, false),
    ENG_RULE_004("ENG-RULE-004", 409, "发布门禁失败", ErrorClass.DATA, false),
    ENG_RULE_005("ENG-RULE-005", 500, "规则执行失败", ErrorClass.INTERNAL, false),
    ENG_RULE_006("ENG-RULE-006", 409, "当前规则状态不允许该操作", ErrorClass.DATA, false),

    ENG_PATHWAY_001("ENG-PATHWAY-001", 400, "路径模板校验失败", ErrorClass.INPUT, false),
    ENG_PATHWAY_002("ENG-PATHWAY-002", 404, "路径模板不存在", ErrorClass.DATA, false),
    ENG_PATHWAY_003("ENG-PATHWAY-003", 404, "患者路径不存在", ErrorClass.DATA, false),
    ENG_PATHWAY_004("ENG-PATHWAY-004", 409, "路径模板发布门禁失败", ErrorClass.DATA, false),
    ENG_PATHWAY_005("ENG-PATHWAY-005", 409, "当前路径状态不允许该操作", ErrorClass.DATA, false),
    ENG_PATHWAY_006("ENG-PATHWAY-006", 400, "路径推进事件不合法", ErrorClass.INPUT, false),
    ENG_PATHWAY_007("ENG-PATHWAY-007", 404, "专病包不存在", ErrorClass.DATA, false),
    ;

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;
    private final ErrorClass errorClass;
    private final boolean retryable;

    ErrorCode(String code, int httpStatus, String defaultMessage,
              ErrorClass errorClass, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
        this.errorClass = errorClass;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public ErrorClass errorClass() {
        return errorClass;
    }

    public boolean retryable() {
        return retryable;
    }

    public static Optional<ErrorCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
            .filter(c -> c.code.equalsIgnoreCase(normalized))
            .findFirst();
    }

    public enum ErrorClass {
        /** 输入数据问题：客户端可修复 */
        INPUT,
        /** 权限/认证问题 */
        AUTH,
        /** 业务数据不一致：管理员排查 */
        DATA,
        /** 外部依赖：可重试 */
        EXTERNAL,
        /** 系统内部错误：研发排查 */
        INTERNAL
    }
}
