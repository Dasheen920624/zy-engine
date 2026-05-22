package com.medkernel.common.dataclass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.medkernel.common.crypto.SmCryptoException;
import com.medkernel.common.crypto.SmCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 字段加密服务：HEALTH_DATA / SENSITIVE 字段透明 SM4 加密。
 *
 * <h2>密文格式（v1）</h2>
 * <pre>
 *   Base64( versionHeader || iv(16字节) || ciphertext )
 *   versionHeader = ASCII "SM4:v1:"  (7 字节)
 * </pre>
 *
 * <p>携带版本头的目的：将来 master key 轮换或换算法时，旧密文仍可识别版本号
 * 走兼容解密分支。
 *
 * <h2>密钥派生</h2>
 *
 * <p>每张表（或 entity 类）使用独立 dek（数据加密密钥）：
 * <pre>
 *   dek = SM3( master_key || ":" || derivation_salt || ":" || tableName )[0..16)
 * </pre>
 *
 * <p>SM3 输出 32 字节，取前 16 字节作为 SM4 密钥。Master key 仅做派生不直接加密。
 *
 * <h2>线程安全</h2>
 *
 * <p>Service 是 Spring 单例 + stateless（仅缓存 dek，{@link ConcurrentHashMap}）。
 * 所有方法可并发调用。
 *
 * <h2>使用方式（Service / Repository 调用方）</h2>
 * <pre>
 * &#64;Service
 * public class PatientService {
 *     private final FieldEncryptionService encryption;
 *     // ...
 *     public void savePatient(PatientEntity p) {
 *         encryption.encryptEntity(p);          // 加密敏感字段
 *         repository.insert(p);
 *         encryption.decryptEntity(p);          // 还原供后续使用（如果调用方需要）
 *     }
 *
 *     public PatientEntity findById(Long id) {
 *         PatientEntity p = repository.findById(id);
 *         encryption.decryptEntity(p);          // 读取后解密
 *         return p;
 *     }
 * }
 * </pre>
 *
 * @see DataClass
 * @see Encrypted
 * @see SmCryptoService
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);

    /** 密文版本前缀。 */
    static final String VERSION_HEADER_V1 = "SM4:v1:";
    private static final byte[] VERSION_HEADER_V1_BYTES =
            VERSION_HEADER_V1.getBytes(StandardCharsets.US_ASCII);
    private static final int IV_LENGTH = 16;
    private static final int SM4_KEY_LENGTH = 16;

    private final SmCryptoService smCrypto;
    private final FieldEncryptionProperties properties;

    /** 类级元数据缓存：扫描一次后缓存所有 @Encrypted 字段（性能关键）。 */
    private final Map<Class<?>, List<Field>> encryptedFieldsCache = new ConcurrentHashMap<>();

    /** 表级派生密钥缓存：tableName → 16 字节 dek。 */
    private final Map<String, byte[]> derivedKeyCache = new ConcurrentHashMap<>();

    private final byte[] masterKey;

    public FieldEncryptionService(SmCryptoService smCrypto, FieldEncryptionProperties properties) {
        this.smCrypto = smCrypto;
        this.properties = properties;
        this.masterKey = decodeMasterKey(properties.getMasterKeyBase64());
        if (log.isInfoEnabled()) {
            log.info("FieldEncryptionService 已就绪：enabled={}, strict-decrypt={}, salt={}",
                    properties.isEnabled(), properties.isStrictDecrypt(),
                    properties.getDerivationSalt());
        }
    }

    // ============================================================
    // 公开 API：entity 加解密
    // ============================================================

    /**
     * 加密 entity 中所有 {@link Encrypted} 字段（就地修改 String 字段）。
     *
     * <p>若 {@link FieldEncryptionProperties#isEnabled()} 为 false，本方法 no-op。
     * <p>已加密的字段（带 {@link #VERSION_HEADER_V1} 前缀）跳过，避免重复加密。
     * <p>{@code null} 字段保持 {@code null}。
     */
    public void encryptEntity(Object entity) {
        if (entity == null || !properties.isEnabled()) {
            return;
        }
        Class<?> type = entity.getClass();
        List<Field> encryptedFields = lookupEncryptedFields(type);
        if (encryptedFields.isEmpty()) {
            return;
        }
        byte[] dek = getDerivedKey(type);
        for (Field field : encryptedFields) {
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }
                if (!(value instanceof String)) {
                    throw new IllegalStateException("@Encrypted 仅支持 String 字段，但发现："
                            + type.getName() + "." + field.getName() + " 类型为 " + value.getClass());
                }
                String plain = (String) value;
                if (isCipherText(plain)) {
                    continue;
                }
                String cipher = encryptString(plain, dek);
                field.set(entity, cipher);
            } catch (IllegalAccessException e) {
                throw new SmCryptoException("反射访问字段失败：" + field, e);
            }
        }
    }

    /**
     * 解密 entity 中所有 {@link Encrypted} 字段（就地修改 String 字段）。
     *
     * <p>非密文字段（无版本头）按策略保留或抛异常（{@link FieldEncryptionProperties#isStrictDecrypt()}）。
     * <p>{@code null} 字段保持 {@code null}。
     */
    public void decryptEntity(Object entity) {
        if (entity == null || !properties.isEnabled()) {
            return;
        }
        Class<?> type = entity.getClass();
        List<Field> encryptedFields = lookupEncryptedFields(type);
        if (encryptedFields.isEmpty()) {
            return;
        }
        byte[] dek = getDerivedKey(type);
        for (Field field : encryptedFields) {
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }
                if (!(value instanceof String)) {
                    throw new IllegalStateException("@Encrypted 仅支持 String 字段，但发现："
                            + type.getName() + "." + field.getName() + " 类型为 " + value.getClass());
                }
                String stored = (String) value;
                if (!isCipherText(stored)) {
                    if (properties.isStrictDecrypt()) {
                        throw new SmCryptoException("严格模式下要求所有 @Encrypted 字段必须是密文，"
                                + "但发现非密文：" + type.getName() + "." + field.getName());
                    }
                    // 非严格模式：旧数据未加密时保留原值
                    continue;
                }
                String plain = decryptString(stored, dek);
                field.set(entity, plain);
            } catch (IllegalAccessException e) {
                throw new SmCryptoException("反射访问字段失败：" + field, e);
            }
        }
    }

    /**
     * 加密单个字符串（外部直接调用，例如查询条件按密文匹配）。
     *
     * @param plain 明文，非 null
     * @param tableName 表名（决定派生 key）
     */
    public String encryptString(String plain, String tableName) {
        byte[] dek = getDerivedKey(tableName);
        return encryptString(plain, dek);
    }

    /**
     * 解密单个字符串。
     *
     * @param cipher 密文（含版本头），非 null
     * @param tableName 表名（决定派生 key）
     */
    public String decryptString(String cipher, String tableName) {
        byte[] dek = getDerivedKey(tableName);
        return decryptString(cipher, dek);
    }

    /**
     * 判断一个字符串是否为本服务产生的密文（用版本头识别）。
     */
    public boolean isCipherText(String value) {
        if (value == null || value.length() < VERSION_HEADER_V1.length()) {
            return false;
        }
        // 密文是 Base64(versionHeader + iv + ct)，Base64 解码后头 7 字节是 ASCII "SM4:v1:"
        try {
            byte[] raw = Base64.getDecoder().decode(value);
            if (raw.length < VERSION_HEADER_V1_BYTES.length + IV_LENGTH) {
                return false;
            }
            for (int i = 0; i < VERSION_HEADER_V1_BYTES.length; i++) {
                if (raw[i] != VERSION_HEADER_V1_BYTES[i]) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException notBase64) {
            return false;
        }
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private String encryptString(String plain, byte[] dek) {
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
        byte[] iv = smCrypto.generateSm4Iv();
        byte[] ct = smCrypto.sm4EncryptCbc(plainBytes, dek, iv);

        ByteBuffer buffer = ByteBuffer.allocate(VERSION_HEADER_V1_BYTES.length + IV_LENGTH + ct.length);
        buffer.put(VERSION_HEADER_V1_BYTES);
        buffer.put(iv);
        buffer.put(ct);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private String decryptString(String stored, byte[] dek) {
        byte[] raw = Base64.getDecoder().decode(stored);
        int minLen = VERSION_HEADER_V1_BYTES.length + IV_LENGTH;
        if (raw.length < minLen) {
            throw new SmCryptoException("密文长度不足：" + raw.length + " < " + minLen);
        }
        for (int i = 0; i < VERSION_HEADER_V1_BYTES.length; i++) {
            if (raw[i] != VERSION_HEADER_V1_BYTES[i]) {
                throw new SmCryptoException("密文版本头不匹配，期望 " + VERSION_HEADER_V1);
            }
        }
        byte[] iv = Arrays.copyOfRange(raw, VERSION_HEADER_V1_BYTES.length, VERSION_HEADER_V1_BYTES.length + IV_LENGTH);
        byte[] ct = Arrays.copyOfRange(raw, VERSION_HEADER_V1_BYTES.length + IV_LENGTH, raw.length);
        byte[] pt = smCrypto.sm4DecryptCbc(ct, dek, iv);
        return new String(pt, StandardCharsets.UTF_8);
    }

    /** 派生表级密钥（按类型缓存）。 */
    byte[] getDerivedKey(Class<?> entityType) {
        String tableName = resolveTableName(entityType);
        return getDerivedKey(tableName);
    }

    byte[] getDerivedKey(String tableName) {
        return derivedKeyCache.computeIfAbsent(tableName, this::deriveTableKey);
    }

    /**
     * 表级密钥派生：SM3(master || ":" || salt || ":" || tableName)[0..16)
     */
    byte[] deriveTableKey(String tableName) {
        byte[] saltBytes = properties.getDerivationSalt().getBytes(StandardCharsets.UTF_8);
        byte[] tableBytes = tableName.getBytes(StandardCharsets.UTF_8);
        byte[] material = new byte[masterKey.length + 1 + saltBytes.length + 1 + tableBytes.length];
        int offset = 0;
        System.arraycopy(masterKey, 0, material, offset, masterKey.length);
        offset += masterKey.length;
        material[offset++] = ':';
        System.arraycopy(saltBytes, 0, material, offset, saltBytes.length);
        offset += saltBytes.length;
        material[offset++] = ':';
        System.arraycopy(tableBytes, 0, material, offset, tableBytes.length);

        byte[] digest = smCrypto.sm3(material);
        return Arrays.copyOf(digest, SM4_KEY_LENGTH);
    }

    /** 优先取 @DataClass 标的 entity 类名（FQN）作为派生上下文。 */
    private String resolveTableName(Class<?> entityType) {
        return entityType.getName();
    }

    /** 扫描类型的 @Encrypted 字段并缓存（保留字段顺序便于调试）。 */
    List<Field> lookupEncryptedFields(Class<?> type) {
        return encryptedFieldsCache.computeIfAbsent(type, FieldEncryptionService::scanEncryptedFields);
    }

    private static List<Field> scanEncryptedFields(Class<?> type) {
        Map<String, Field> declared = new LinkedHashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(Encrypted.class) && !declared.containsKey(f.getName())) {
                    if (f.getType() != String.class) {
                        throw new IllegalStateException("@Encrypted 仅支持 String 字段："
                                + type.getName() + "." + f.getName() + " 类型为 " + f.getType());
                    }
                    f.setAccessible(true);
                    declared.put(f.getName(), f);
                }
            }
            current = current.getSuperclass();
        }
        return new java.util.ArrayList<>(declared.values());
    }

    private static byte[] decodeMasterKey(String base64) {
        if (base64 == null || base64.isEmpty()) {
            throw new SmCryptoException("master key 未配置：medkernel.security.field-encryption.master-key-base64");
        }
        byte[] decoded = Base64.getDecoder().decode(base64);
        if (decoded.length != SM4_KEY_LENGTH) {
            throw new SmCryptoException("master key 长度必须为 " + SM4_KEY_LENGTH + " 字节，实际：" + decoded.length);
        }
        return decoded;
    }

    /** 供测试 / 健康检查：列出已缓存的派生密钥表名。 */
    public Map<String, Integer> describeKeyCache() {
        Map<String, Integer> snapshot = new HashMap<>();
        derivedKeyCache.forEach((k, v) -> snapshot.put(k, v.length));
        return snapshot;
    }
}
