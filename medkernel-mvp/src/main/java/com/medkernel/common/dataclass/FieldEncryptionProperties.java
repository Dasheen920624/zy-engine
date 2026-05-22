package com.medkernel.common.dataclass;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 字段加密配置（{@code medkernel.security.field-encryption.*}）。
 *
 * <h2>密钥来源（生产部署强烈建议）</h2>
 *
 * <p>{@link #masterKeyBase64} 是 SM4 master key 的 Base64 表示，必须满足
 * <strong>明文 16 字节 = Base64 解码后 16 字节</strong>。配置来源优先级：
 *
 * <ol>
 *   <li><strong>HSM / KMS</strong>（生产）—— 通过 启动时机密注入到环境变量 {@code MEDKERNEL_SM4_MASTER_KEY}</li>
 *   <li><strong>K8s Secret / Vault</strong>（云原生部署）—— 同样通过环境变量</li>
 *   <li><strong>application-prod.yml</strong>（小型部署）—— 加密的配置中心拉取后注入</li>
 *   <li><strong>application.yml 默认值</strong>（开发 / 演示）—— 仅本地能用，CI 检查阻止</li>
 * </ol>
 *
 * <h2>密钥派生</h2>
 *
 * <p>master key 不直接加密业务数据；每张敏感表使用「master key + 表名 SM3 哈希」
 * 派生独立 dek。详见 {@link FieldEncryptionService#deriveTableKey(String)}。
 *
 * <p>这样设计的好处：
 * <ul>
 *   <li>不同表的密文不可互换，密文泄露半径限于单表</li>
 *   <li>master key 一次轮换 → 所有表 dek 自动跟随</li>
 *   <li>密文头部不需要存表名（派生 key 隐含上下文）</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "medkernel.security.field-encryption")
public class FieldEncryptionProperties {

    /**
     * 是否启用字段加密。
     * <p>开发环境可关闭以便直接读数据库定位问题；生产强制 true。
     */
    private boolean enabled = true;

    /**
     * SM4 master key（Base64 编码，16 字节明文）。
     * <p>默认值仅供 H2 内嵌开发使用（明文 "medkernel-devkey" 共 16 字节）；
     * 生产部署必须通过 {@code MEDKERNEL_SM4_MASTER_KEY} 环境变量覆盖。
     */
    private String masterKeyBase64 = "bWVka2VybmVsLWRldmtleQ==";

    /**
     * 密钥派生盐（域分离），SM3 派生表 key 时拼接。
     * <p>不需要保密，但建议每次部署独立。
     */
    private String derivationSalt = "medkernel-dataclass-v1";

    /**
     * 严格模式：
     * <ul>
     *   <li>{@code true}（生产推荐）：解密失败抛异常</li>
     *   <li>{@code false}（开发友好）：解密失败保留原字符串（用于旧数据未加密迁移期）</li>
     * </ul>
     */
    private boolean strictDecrypt = true;

    /**
     * 旧 SM4 master key（Base64 编码，16 字节明文）。
     *
     * <p>密钥轮换时配置此项。配置后：
     * <ul>
     *   <li>新加密使用当前 master key，密文版本头为 "SM4:v2:"</li>
     *   <li>解密时自动尝试旧 key（v1 密文）和当前 key（v2 密文）</li>
     *   <li>读取到的 v1 密文会在 encryptEntity 时自动 re-encrypt 为 v2</li>
     * </ul>
     *
     * <p>轮换完成后（所有 v1 密文已迁移为 v2），清除此配置即可。
     */
    private String previousMasterKeyBase64;

    /**
     * 兼容模式：同时支持国密和 RSA/AES。
     *
     * <p>当 {@code true} 时，系统同时支持国密（SM2/SM3/SM4）和 RSA/AES 两种加密套件，
     * 便于从国际算法平滑迁移到国密算法。
     * <ul>
     *   <li>新数据使用国密算法加密</li>
     *   <li>旧数据（RSA/AES 加密）仍可正常解密</li>
     *   <li>读取时自动 re-encrypt 为国密</li>
     * </ul>
     *
     * <p>当 {@code false}（默认）时，仅使用国密算法。
     */
    private boolean compatibilityMode = false;

    /**
     * HSM（硬件安全模块）配置。
     *
     * <p>当 {@code true} 时，密钥操作委托给 HSM，不在内存中存储明文密钥。
     * <p>生产环境强烈建议启用。
     */
    private boolean hsmEnabled = false;

    /**
     * HSM 提供者类型（如 "SoftHSM", "ThalesLuna", "SansecSJJ1012"）。
     */
    private String hsmProvider = "SoftHSM";

    /**
     * HSM 密钥标签（HSM 中存储的密钥名称）。
     */
    private String hsmKeyLabel = "medkernel-sm4-master";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMasterKeyBase64() {
        return masterKeyBase64;
    }

    public void setMasterKeyBase64(String masterKeyBase64) {
        this.masterKeyBase64 = masterKeyBase64;
    }

    public String getDerivationSalt() {
        return derivationSalt;
    }

    public void setDerivationSalt(String derivationSalt) {
        this.derivationSalt = derivationSalt;
    }

    public boolean isStrictDecrypt() {
        return strictDecrypt;
    }

    public void setStrictDecrypt(boolean strictDecrypt) {
        this.strictDecrypt = strictDecrypt;
    }

    public String getPreviousMasterKeyBase64() {
        return previousMasterKeyBase64;
    }

    public void setPreviousMasterKeyBase64(String previousMasterKeyBase64) {
        this.previousMasterKeyBase64 = previousMasterKeyBase64;
    }

    public boolean isCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    public boolean isHsmEnabled() {
        return hsmEnabled;
    }

    public void setHsmEnabled(boolean hsmEnabled) {
        this.hsmEnabled = hsmEnabled;
    }

    public String getHsmProvider() {
        return hsmProvider;
    }

    public void setHsmProvider(String hsmProvider) {
        this.hsmProvider = hsmProvider;
    }

    public String getHsmKeyLabel() {
        return hsmKeyLabel;
    }

    public void setHsmKeyLabel(String hsmKeyLabel) {
        this.hsmKeyLabel = hsmKeyLabel;
    }
}
