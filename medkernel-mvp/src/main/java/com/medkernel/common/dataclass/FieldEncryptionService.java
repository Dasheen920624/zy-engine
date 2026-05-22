package com.medkernel.common.dataclass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * <h2>密文格式</h2>
 * <ul>
 *   <li>v1: Base64( "SM4:v1:" || iv(16字节) || ciphertext )</li>
 *   <li>v2: Base64( "SM4:v2:" || iv(16字节) || ciphertext ) — 密钥轮换后新密文</li>
 * </ul>
 *
 * <h2>密钥轮换</h2>
 *
 * <p>当配置了 {@code previous-master-key-base64} 时，解密流程自动尝试：
 * <ol>
 *   <li>先用当前 master key 派生的 dek 解密</li>
 *   <li>若失败且存在 previous master key，用旧 key 派生的 dek 解密</li>
 *   <li>解密成功后，加密时始终使用当前 master key（自动 re-encrypt）</li>
 * </ol>
 *
 * <h2>密钥派生</h2>
 *
 * <p>每张表使用独立 dek（数据加密密钥）：
 * <pre>
 *   dek = SM3( master_key || ":" || derivation_salt || ":" || tableName )[0..16)
 * </pre>
 *
 * @see DataClass
 * @see Encrypted
 * @see SmCryptoService
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);

    /** 密文版本前缀 v1。 */
    static final String VERSION_HEADER_V1 = "SM4:v1:";
    private static final byte[] VERSION_HEADER_V1_BYTES =
            VERSION_HEADER_V1.getBytes(StandardCharsets.US_ASCII);

    /** 密文版本前缀 v2（密钥轮换后新密文）。 */
    static final String VERSION_HEADER_V2 = "SM4:v2:";
    private static final byte[] VERSION_HEADER_V2_BYTES =
            VERSION_HEADER_V2.getBytes(StandardCharsets.US_ASCII);

    private static final int IV_LENGTH = 16;
    private static final int SM4_KEY_LENGTH = 16;

    private final SmCryptoService smCrypto;
    private final FieldEncryptionProperties properties;

    /** 类级元数据缓存：扫描一次后缓存所有 @Encrypted 字段。 */
    private final Map<Class<?>, List<Field>> encryptedFieldsCache = new ConcurrentHashMap<>();

    /** 表级派生密钥缓存：tableName → 16 字节 dek。 */
    private final Map<String, byte[]> derivedKeyCache = new ConcurrentHashMap<>();

    /** 旧 master key 派生密钥缓存：tableName → 16 字节 dek。 */
    private final Map<String, byte[]> previousDerivedKeyCache = new ConcurrentHashMap<>();

    private final byte[] masterKey;
    private final byte[] previousMasterKey;

    /** 当前加密使用的版本头。 */
    private final byte[] currentVersionHeader;

    public FieldEncryptionService(SmCryptoService smCrypto, FieldEncryptionProperties properties) {
        this.smCrypto = smCrypto;
        this.properties = properties;
        this.masterKey = decodeMasterKey(properties.getMasterKeyBase64());
        this.previousMasterKey = decodeOptionalMasterKey(properties.getPreviousMasterKeyBase64());
        this.currentVersionHeader = previousMasterKey != null ? VERSION_HEADER_V2_BYTES : VERSION_HEADER_V1_BYTES;
        if (log.isInfoEnabled()) {
            log.info("FieldEncryptionService 已就绪：enabled={}, strict-decrypt={}, salt={}, key-rotation={}",
                    properties.isEnabled(), properties.isStrictDecrypt(),
                    properties.getDerivationSalt(),
                    previousMasterKey != null ? "ACTIVE(v2)" : "INACTIVE(v1)");
        }
    }

    // ============================================================
    // 公开 API：entity 加解密
    // ============================================================

    /**
     * 加密 entity 中所有 {@link Encrypted} 字段（就地修改 String 字段）。
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
                    // 已加密的旧版本密文，自动 re-encrypt
                    if (needsReEncrypt(plain)) {
                        String decrypted = decryptWithFallback(plain, type);
                        String reEncrypted = encryptString(decrypted, dek);
                        field.set(entity, reEncrypted);
                    }
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
                    continue;
                }
                String plain = decryptWithFallback(stored, type);
                field.set(entity, plain);
            } catch (IllegalAccessException e) {
                throw new SmCryptoException("反射访问字段失败：" + field, e);
            }
        }
    }

    /**
     * 加密单个字符串（外部直接调用）。
     */
    public String encryptString(String plain, String tableName) {
        byte[] dek = getDerivedKey(tableName);
        return encryptString(plain, dek);
    }

    /**
     * 解密单个字符串（支持密钥轮换回退）。
     */
    public String decryptString(String cipher, String tableName) {
        return decryptWithFallback(cipher, tableName);
    }

    /**
     * 判断一个字符串是否为本服务产生的密文（用版本头识别）。
     */
    public boolean isCipherText(String value) {
        if (value == null || value.length() < VERSION_HEADER_V1.length()) {
            return false;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(value);
            if (raw.length < VERSION_HEADER_V1_BYTES.length + IV_LENGTH) {
                return false;
            }
            return startsWithHeader(raw, VERSION_HEADER_V1_BYTES)
                    || startsWithHeader(raw, VERSION_HEADER_V2_BYTES);
        } catch (IllegalArgumentException notBase64) {
            return false;
        }
    }

    /**
     * 判断密文是否需要 re-encrypt（旧版本密文需要用新密钥重新加密）。
     */
    public boolean needsReEncrypt(String cipherText) {
        if (cipherText == null) {
            return false;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(cipherText);
            // 如果当前使用 v2 但密文是 v1，需要 re-encrypt
            if (previousMasterKey != null && startsWithHeader(raw, VERSION_HEADER_V1_BYTES)) {
                return true;
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取密钥轮换状态信息。
     */
    public Map<String, Object> getKeyRotationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentVersion", previousMasterKey != null ? "v2" : "v1");
        status.put("rotationActive", previousMasterKey != null);
        status.put("derivedKeyCount", derivedKeyCache.size());
        status.put("previousDerivedKeyCount", previousDerivedKeyCache.size());
        status.put("encryptionEnabled", properties.isEnabled());
        return status;
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private String encryptString(String plain, byte[] dek) {
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
        byte[] iv = smCrypto.generateSm4Iv();
        byte[] ct = smCrypto.sm4EncryptCbc(plainBytes, dek, iv);

        ByteBuffer buffer = ByteBuffer.allocate(currentVersionHeader.length + IV_LENGTH + ct.length);
        buffer.put(currentVersionHeader);
        buffer.put(iv);
        buffer.put(ct);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * 解密字符串，支持密钥轮换回退。
     * 先用当前 key 解密，失败后尝试旧 key。
     */
    private String decryptWithFallback(String stored, Class<?> entityType) {
        String tableName = resolveTableName(entityType);
        return decryptWithFallback(stored, tableName);
    }

    private String decryptWithFallback(String stored, String tableName) {
        byte[] raw = Base64.getDecoder().decode(stored);

        // 判断密文版本
        boolean isV1 = startsWithHeader(raw, VERSION_HEADER_V1_BYTES);
        boolean isV2 = startsWithHeader(raw, VERSION_HEADER_V2_BYTES);

        if (!isV1 && !isV2) {
            throw new SmCryptoException("密文版本头不匹配，期望 " + VERSION_HEADER_V1 + " 或 " + VERSION_HEADER_V2);
        }

        byte[] versionHeader = isV1 ? VERSION_HEADER_V1_BYTES : VERSION_HEADER_V2_BYTES;

        // 先尝试用对应版本的 key 解密
        try {
            byte[] dek;
            if (isV2 || previousMasterKey == null) {
                dek = getDerivedKey(tableName);
            } else {
                // v1 密文 + 有旧 key：优先用旧 key
                dek = getPreviousDerivedKey(tableName);
            }
            return decryptRaw(raw, versionHeader, dek);
        } catch (SmCryptoException e) {
            // 如果用旧 key 解密失败，尝试用当前 key（可能是 v1 密文但用当前 key 加密的过渡期）
            if (previousMasterKey != null && isV1) {
                try {
                    byte[] currentDek = getDerivedKey(tableName);
                    return decryptRaw(raw, versionHeader, currentDek);
                } catch (SmCryptoException e2) {
                    // 两次都失败
                    throw new SmCryptoException("密钥轮换解密失败：当前 key 和旧 key 均无法解密", e2);
                }
            }
            throw e;
        }
    }

    private String decryptRaw(byte[] raw, byte[] versionHeader, byte[] dek) {
        int minLen = versionHeader.length + IV_LENGTH;
        if (raw.length < minLen) {
            throw new SmCryptoException("密文长度不足：" + raw.length + " < " + minLen);
        }
        byte[] iv = Arrays.copyOfRange(raw, versionHeader.length, versionHeader.length + IV_LENGTH);
        byte[] ct = Arrays.copyOfRange(raw, versionHeader.length + IV_LENGTH, raw.length);
        byte[] pt = smCrypto.sm4DecryptCbc(ct, dek, iv);
        return new String(pt, StandardCharsets.UTF_8);
    }

    private static boolean startsWithHeader(byte[] raw, byte[] header) {
        if (raw.length < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (raw[i] != header[i]) {
                return false;
            }
        }
        return true;
    }

    /** 派生表级密钥（按类型缓存）。 */
    byte[] getDerivedKey(Class<?> entityType) {
        String tableName = resolveTableName(entityType);
        return getDerivedKey(tableName);
    }

    byte[] getDerivedKey(String tableName) {
        return derivedKeyCache.computeIfAbsent(tableName, this::deriveTableKey);
    }

    /** 派生旧 master key 的表级密钥。 */
    byte[] getPreviousDerivedKey(String tableName) {
        if (previousMasterKey == null) {
            throw new SmCryptoException("未配置 previous-master-key，无法解密旧密文");
        }
        return previousDerivedKeyCache.computeIfAbsent(tableName, this::derivePreviousTableKey);
    }

    /**
     * 表级密钥派生：SM3(master || ":" || salt || ":" || tableName)[0..16)
     */
    byte[] deriveTableKey(String tableName) {
        return deriveTableKeyWithMaster(masterKey, tableName);
    }

    byte[] derivePreviousTableKey(String tableName) {
        return deriveTableKeyWithMaster(previousMasterKey, tableName);
    }

    private byte[] deriveTableKeyWithMaster(byte[] mk, String tableName) {
        byte[] saltBytes = properties.getDerivationSalt().getBytes(StandardCharsets.UTF_8);
        byte[] tableBytes = tableName.getBytes(StandardCharsets.UTF_8);
        byte[] material = new byte[mk.length + 1 + saltBytes.length + 1 + tableBytes.length];
        int offset = 0;
        System.arraycopy(mk, 0, material, offset, mk.length);
        offset += mk.length;
        material[offset++] = ':';
        System.arraycopy(saltBytes, 0, material, offset, saltBytes.length);
        offset += saltBytes.length;
        material[offset++] = ':';
        System.arraycopy(tableBytes, 0, material, offset, tableBytes.length);

        byte[] digest = smCrypto.sm3(material);
        return Arrays.copyOf(digest, SM4_KEY_LENGTH);
    }

    private String resolveTableName(Class<?> entityType) {
        return entityType.getName();
    }

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
        return new ArrayList<>(declared.values());
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

    private static byte[] decodeOptionalMasterKey(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(base64);
        if (decoded.length != SM4_KEY_LENGTH) {
            throw new SmCryptoException("previous master key 长度必须为 " + SM4_KEY_LENGTH + " 字节，实际：" + decoded.length);
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
