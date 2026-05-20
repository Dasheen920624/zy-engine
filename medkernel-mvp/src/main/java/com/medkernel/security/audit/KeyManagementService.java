package com.medkernel.security.audit;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 密钥管理服务：管理加密密钥的生命周期，支持密钥轮换。
 */
@Service
public class KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementService.class);
    private static final String DEFAULT_ALGORITHM = "AES-256-GCM";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final EnginePersistenceProperties properties;

    public KeyManagementService(EnginePersistenceProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取当前活跃的加密密钥。
     */
    public EncryptionKey getActiveKey() {
        String sql = "SELECT id, key_id, key_version, algorithm, key_material, status, " +
                     "activated_at, deprecated_at, expires_at, description, " +
                     "created_by, created_time, updated_by, updated_time " +
                     "FROM sec_encryption_key WHERE status = 'ACTIVE' ORDER BY key_version DESC";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapEncryptionKey(rs);
            }
        } catch (SQLException e) {
            log.error("Failed to get active encryption key", e);
        }
        return null;
    }

    /**
     * 根据密钥 ID 和版本获取密钥。
     */
    public EncryptionKey getKey(String keyId, int keyVersion) {
        String sql = "SELECT id, key_id, key_version, algorithm, key_material, status, " +
                     "activated_at, deprecated_at, expires_at, description, " +
                     "created_by, created_time, updated_by, updated_time " +
                     "FROM sec_encryption_key WHERE key_id = ? AND key_version = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, keyId);
            ps.setInt(2, keyVersion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapEncryptionKey(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get encryption key {}:{}", keyId, keyVersion, e);
        }
        return null;
    }

    /**
     * 生成新的加密密钥并执行轮换。
     *
     * @param description 密钥描述
     * @param rotatedBy   操作人
     * @return 新密钥
     */
    public EncryptionKey rotateKey(String description, String rotatedBy) {
        // 1. 获取当前活跃密钥
        EncryptionKey currentKey = getActiveKey();

        // 2. 生成新密钥
        String newKeyId = currentKey != null ? currentKey.getKeyId() : "master-key-" + System.currentTimeMillis();
        int newVersion = currentKey != null ? currentKey.getKeyVersion() + 1 : 1;

        // 3. 生成密钥材料
        String keyMaterial = generateKeyMaterial();

        // 4. 创建新密钥记录
        EncryptionKey newKey = new EncryptionKey();
        newKey.setId(Ids.next());
        newKey.setKeyId(newKeyId);
        newKey.setKeyVersion(newVersion);
        newKey.setAlgorithm(DEFAULT_ALGORITHM);
        newKey.setKeyMaterial(keyMaterial);
        newKey.setStatus("ACTIVE");
        newKey.setActivatedAt(LocalDateTime.now());
        newKey.setDescription(description);
        newKey.setCreatedBy(rotatedBy);
        newKey.setCreatedTime(LocalDateTime.now());

        // 5. 保存新密钥
        saveKey(newKey);

        // 6. 将旧密钥标记为 DEPRECATED
        if (currentKey != null) {
            deprecateKey(currentKey.getId(), rotatedBy);
        }

        log.info("Encryption key rotated: {} version {}", newKeyId, newVersion);
        return newKey;
    }

    /**
     * 使用当前活跃密钥加密数据。
     */
    public String encrypt(String plaintext) {
        EncryptionKey key = getActiveKey();
        if (key == null) {
            throw new IllegalStateException("No active encryption key available");
        }
        return encrypt(plaintext, key);
    }

    /**
     * 使用指定密钥加密数据。
     */
    public String encrypt(String plaintext, EncryptionKey key) {
        try {
            SecretKey secretKey = decodeKey(key.getKeyMaterial());
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合 IV + ciphertext 并 Base64 编码
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return "ENC(" + Base64.getEncoder().encodeToString(combined) + ")";
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密数据。自动尝试所有活跃和历史密钥。
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || !encryptedText.startsWith("ENC(") || !encryptedText.endsWith(")")) {
            throw new IllegalArgumentException("Encrypted text must use ENC(...) format");
        }

        String base64Data = encryptedText.substring(4, encryptedText.length() - 1);
        byte[] combined = Base64.getDecoder().decode(base64Data);

        // 尝试所有可用密钥
        List<EncryptionKey> keys = getAllActiveKeys();
        for (EncryptionKey key : keys) {
            try {
                return decrypt(combined, key);
            } catch (Exception e) {
                // 尝试下一个密钥
                log.debug("Failed to decrypt with key {}:{}", key.getKeyId(), key.getKeyVersion());
            }
        }

        throw new RuntimeException("Failed to decrypt with any available key");
    }

    /**
     * 使用指定密钥解密数据。
     */
    public String decrypt(byte[] combined, EncryptionKey key) throws Exception {
        SecretKey secretKey = decodeKey(key.getKeyMaterial());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // 提取 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // 提取 ciphertext
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * 列出所有密钥。
     */
    public List<EncryptionKey> listKeys() {
        String sql = "SELECT id, key_id, key_version, algorithm, key_material, status, " +
                     "activated_at, deprecated_at, expires_at, description, " +
                     "created_by, created_time, updated_by, updated_time " +
                     "FROM sec_encryption_key ORDER BY key_version DESC";
        List<EncryptionKey> keys = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                keys.add(mapEncryptionKey(rs));
            }
        } catch (SQLException e) {
            log.error("Failed to list encryption keys", e);
        }
        return keys;
    }

    // 私有方法

    private String generateKeyMaterial() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(preferredAesKeySize());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES-256 not available", e);
        }
    }

    private SecretKey decodeKey(String keyMaterial) {
        String encoded = keyMaterial != null && keyMaterial.startsWith("base64:")
                ? keyMaterial.substring("base64:".length())
                : keyMaterial;
        byte[] decodedKey = Base64.getDecoder().decode(encoded);
        if (maxAllowedAesKeySize() < 256 && decodedKey.length > 16) {
            decodedKey = Arrays.copyOf(decodedKey, 16);
        }
        return new SecretKeySpec(decodedKey, "AES");
    }

    private int preferredAesKeySize() {
        return maxAllowedAesKeySize() >= 256 ? 256 : 128;
    }

    private int maxAllowedAesKeySize() {
        try {
            return Cipher.getMaxAllowedKeyLength("AES");
        } catch (Exception e) {
            return 128;
        }
    }

    private List<EncryptionKey> getAllActiveKeys() {
        String sql = "SELECT id, key_id, key_version, algorithm, key_material, status, " +
                     "activated_at, deprecated_at, expires_at, description, " +
                     "created_by, created_time, updated_by, updated_time " +
                     "FROM sec_encryption_key WHERE status IN ('ACTIVE', 'DEPRECATED') ORDER BY key_version DESC";
        List<EncryptionKey> keys = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                keys.add(mapEncryptionKey(rs));
            }
        } catch (SQLException e) {
            log.error("Failed to get all active keys", e);
        }
        return keys;
    }

    private void saveKey(EncryptionKey key) {
        String sql = "INSERT INTO sec_encryption_key (id, key_id, key_version, algorithm, key_material, " +
                     "status, activated_at, description, created_by, created_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, key.getId());
            ps.setString(2, key.getKeyId());
            ps.setInt(3, key.getKeyVersion());
            ps.setString(4, key.getAlgorithm());
            ps.setString(5, key.getKeyMaterial());
            ps.setString(6, key.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(key.getActivatedAt()));
            ps.setString(8, key.getDescription());
            ps.setString(9, key.getCreatedBy());
            ps.setTimestamp(10, Timestamp.valueOf(key.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save encryption key", e);
            throw new RuntimeException("Failed to save encryption key", e);
        }
    }

    private void deprecateKey(Long keyId, String updatedBy) {
        String sql = "UPDATE sec_encryption_key SET status = 'DEPRECATED', deprecated_at = ?, " +
                     "updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, updatedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, keyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to deprecate encryption key {}", keyId, e);
        }
    }

    private EncryptionKey mapEncryptionKey(ResultSet rs) throws SQLException {
        EncryptionKey key = new EncryptionKey();
        key.setId(rs.getLong("id"));
        key.setKeyId(rs.getString("key_id"));
        key.setKeyVersion(rs.getInt("key_version"));
        key.setAlgorithm(rs.getString("algorithm"));
        key.setKeyMaterial(rs.getString("key_material"));
        key.setStatus(rs.getString("status"));

        Timestamp activatedAt = rs.getTimestamp("activated_at");
        if (activatedAt != null) {
            key.setActivatedAt(activatedAt.toLocalDateTime());
        }
        Timestamp deprecatedAt = rs.getTimestamp("deprecated_at");
        if (deprecatedAt != null) {
            key.setDeprecatedAt(deprecatedAt.toLocalDateTime());
        }
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            key.setExpiresAt(expiresAt.toLocalDateTime());
        }

        key.setDescription(rs.getString("description"));
        key.setCreatedBy(rs.getString("created_by"));

        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            key.setCreatedTime(createdTime.toLocalDateTime());
        }
        key.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            key.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return key;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
    }
}
