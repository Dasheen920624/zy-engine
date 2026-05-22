package com.medkernel.common.dataclass;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * 数据脱敏服务：API 响应前按 {@link Encrypted.MaskPolicy} 改写字段。
 *
 * <p>调用方在序列化 entity 给前端 / 第三方系统时调用 {@link #maskEntity}，
 * 即可按字段注解自动脱敏。
 *
 * <h2>脱敏规则</h2>
 *
 * <p>详见 {@link Encrypted.MaskPolicy} 各枚举值描述。所有方法均基于 GB 11643-1999
 * 身份证规范 + 三大运营商手机号规范。
 *
 * <h2>线程安全</h2>
 *
 * <p>Stateless，仅缓存反射字段（{@link ConcurrentHashMap}）。
 *
 * <h2>与 {@link FieldEncryptionService} 的协作</h2>
 *
 * <p>典型 Service 层调用顺序：
 * <pre>
 *   PatientEntity p = repository.findById(id);
 *   fieldEncryption.decryptEntity(p);    // 1. 解密成明文（内部使用）
 *   // ... 业务逻辑 ...
 *   if (!hasViewFullPermission) {
 *       maskingService.maskEntity(p);    // 2. 给前端前脱敏
 *   }
 *   return ApiResult.ok(p);
 * </pre>
 *
 * @see Encrypted
 * @see FieldEncryptionService
 */
@Service
public class DataMaskingService {

    /** {@code @Encrypted} 字段缓存（与 FieldEncryptionService 独立，避免循环依赖）。 */
    private final Map<Class<?>, List<Field>> maskableFieldsCache = new ConcurrentHashMap<>();

    /**
     * 就地脱敏 entity 所有 {@link Encrypted} 字段。
     *
     * <p>{@code null} 字段保持 {@code null}；其它按 {@link Encrypted.MaskPolicy} 处理。
     * <p>{@link Encrypted.MaskPolicy#NONE} 字段不动。
     */
    public void maskEntity(Object entity) {
        if (entity == null) {
            return;
        }
        List<Field> fields = maskableFieldsCache.computeIfAbsent(
                entity.getClass(), DataMaskingService::scanEncryptedFields);
        for (Field field : fields) {
            try {
                Object value = field.get(entity);
                if (!(value instanceof String)) {
                    continue;
                }
                Encrypted annotation = field.getAnnotation(Encrypted.class);
                String masked = mask((String) value, annotation.maskPolicy());
                field.set(entity, masked);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("反射访问字段失败：" + field, e);
            }
        }
    }

    /**
     * 按策略脱敏单个字符串。null 或空字符串原样返回。
     */
    public String mask(String value, Encrypted.MaskPolicy policy) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        switch (policy) {
            case NONE:
                return value;
            case FULL:
                return repeat('*', Math.min(value.length(), 8));
            case ID_CARD:
                return maskIdCard(value);
            case PHONE:
                return maskPhone(value);
            case EMAIL:
                return maskEmail(value);
            case NAME:
                return maskName(value);
            case ADDRESS:
                return maskAddress(value);
            default:
                return repeat('*', Math.min(value.length(), 8));
        }
    }

    // ============================================================
    // 各类脱敏策略实现（GB 11643-1999 + 行业惯例）
    // ============================================================

    /** 身份证：保留前 4 + 后 4，中间 10 个 * （15/18 位都适配）。 */
    private static String maskIdCard(String idCard) {
        if (idCard.length() < 8) {
            return repeat('*', idCard.length());
        }
        return idCard.substring(0, 4) + repeat('*', idCard.length() - 8) + idCard.substring(idCard.length() - 4);
    }

    /** 手机号：保留前 3 + 后 4，中间 4 个 * （11 位） */
    private static String maskPhone(String phone) {
        if (phone.length() < 7) {
            return repeat('*', phone.length());
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱：保留 @ 前首字母 + @ 后整段（local 部分越短保留越少）。 */
    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return repeat('*', email.length());
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() == 1) {
            return "*" + domain;
        }
        return local.charAt(0) + repeat('*', Math.min(local.length() - 1, 6)) + domain;
    }

    /** 姓名：1 字保留；2 字「张*」；3+ 字「张**」。 */
    private static String maskName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + repeat('*', name.length() - 1);
    }

    /** 地址：保留前 6 个字符 + "***"。 */
    private static String maskAddress(String address) {
        if (address.length() <= 6) {
            return repeat('*', address.length());
        }
        return address.substring(0, 6) + "***";
    }

    private static String repeat(char c, int times) {
        if (times <= 0) {
            return "";
        }
        char[] buf = new char[times];
        java.util.Arrays.fill(buf, c);
        return new String(buf);
    }

    private static List<Field> scanEncryptedFields(Class<?> type) {
        java.util.List<Field> list = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(Encrypted.class) && f.getType() == String.class) {
                    f.setAccessible(true);
                    list.add(f);
                }
            }
            current = current.getSuperclass();
        }
        return list;
    }
}
