package com.medkernel.common.dataclass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级数据分级标注。
 *
 * <p>标注实体类（POJO Entity / DTO）整体的数据敏感度，由
 * {@link DataClassRegistry} 在启动时扫描并登记。
 *
 * <h2>使用方式</h2>
 * <pre>
 * &#64;DataClass(DataClassification.HEALTH_DATA)
 * public class PatientEntity { ... }
 * </pre>
 *
 * <h2>语义</h2>
 *
 * <p>类的分级 = 类中所有字段的<strong>最高</strong>分级。
 * 字段未单独标 {@link Encrypted} 时，按类的默认级别对待。
 *
 * <p>类无标注时，默认 {@link DataClassification#INTERNAL}。
 *
 * @see DataClassification
 * @see Encrypted
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataClass {

    /** 数据分级。 */
    DataClassification value();
}
