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
}
