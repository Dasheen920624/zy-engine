package com.medkernel.common.dataclass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级加密标注。
 *
 * <p>标注实体字段在持久化层需要 SM4 加密存储，由
 * {@link FieldEncryptionService} 在 Service 层透明处理。
 *
 * <h2>使用方式</h2>
 * <pre>
 * &#64;DataClass(DataClassification.HEALTH_DATA)
 * public class PatientEntity {
 *     &#64;Encrypted
 *     private String idCardNo;     // 身份证：SM4 加密后存数据库
 *     &#64;Encrypted(maskPolicy = MaskPolicy.PHONE)
 *     private String phone;        // 手机号：加密 + 脱敏 3+4
 *     private String patientId;    // 平台内部 ID，不加密
 * }
 * </pre>
 *
 * <h2>语义</h2>
 *
 * <p>标注 {@code @Encrypted} 的字段：
 * <ul>
 *   <li>写入数据库前由 {@link FieldEncryptionService#encryptEntity} 用 SM4/CBC + PKCS5
 *       加密 → Base64 字符串（IV 内嵌密文头部）</li>
 *   <li>读取后由 {@link FieldEncryptionService#decryptEntity} 还原明文</li>
 *   <li>序列化到外部 API 响应时按 {@link #maskPolicy} 脱敏</li>
 *   <li>仅支持 {@code String} 类型字段。其它类型抛 IllegalStateException</li>
 * </ul>
 *
 * <p>字段未标 {@code @Encrypted} 时不加密，但仍受类级 {@link DataClass} 控制访问。
 *
 * @see DataClass
 * @see FieldEncryptionService
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {

    /**
     * 脱敏策略，用于 API 响应。
     * <p>默认 {@link MaskPolicy#FULL} 全部隐藏，仅给「查看完整」权限的角色显示明文。
     */
    MaskPolicy maskPolicy() default MaskPolicy.FULL;

    /**
     * 脱敏策略枚举。
     */
    enum MaskPolicy {

        /** 全部隐藏：{@code ******} */
        FULL,

        /** 身份证：保留前 4 + 后 4，中间 {@code ************}（GB 11643-1999 风格） */
        ID_CARD,

        /** 手机号：保留前 3 + 后 4，中间 {@code ****}（如 138****5678） */
        PHONE,

        /** 邮箱：保留 @ 前首字母 + @ 后全部（如 {@code z***@medkernel.com}） */
        EMAIL,

        /** 中文姓名：保留首字 + 「**」（如 王**） */
        NAME,

        /** 地址：保留省/市级 + {@code ***}（如 北京市朝阳区***） */
        ADDRESS,

        /** 不脱敏（少数场景，如医疗机构编码） */
        NONE;
    }
}
