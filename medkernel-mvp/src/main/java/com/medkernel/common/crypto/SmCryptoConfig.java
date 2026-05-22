package com.medkernel.common.crypto;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 国密配置：注册 BouncyCastle Provider 到 JCA。
 *
 * <p>本 @Configuration 在 Spring 容器启动时被加载一次：
 * 1. 检查 {@link Security#getProvider(String)} 是否已含 "BC"
 * 2. 若未注册则调用 {@link Security#addProvider} 注册 {@link BouncyCastleProvider}
 *
 * <p>注册顺序：默认追加在 Provider 列表末尾。本项目不依赖 BC 覆盖 JDK 默认算法
 * （如 RSA / AES），仅使用 BC 独有的 SM2 / SM3 / SM4 / GMObjectIdentifiers，
 * 因此追加位置即可，不需要 insertProviderAt(provider, 1)。
 *
 * <p>线程安全：JCA Provider 注册本身是同步操作，多线程并发调用 {@link #bouncyCastleProvider()}
 * 由 Spring 单例容器保证只执行一次。
 *
 * @see SmCryptoService
 */
@Configuration
public class SmCryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(SmCryptoConfig.class);

    /**
     * 注册 BouncyCastle Provider 到 JCA 全局列表。
     *
     * <p>返回 Provider 实例供其它 Bean 直接注入使用（如显式 {@code Cipher.getInstance("SM4/CBC/PKCS5Padding", bcProvider)}）。
     *
     * <p>幂等：重复启动不会重复注册。
     */
    @Bean
    public BouncyCastleProvider bouncyCastleProvider() {
        // 先解除 JCE 强度限制（兼容 JDK < 8u151），再注册 BC Provider。
        JceUnlimitedStrengthEnabler.enable();
        BouncyCastleProvider provider = (BouncyCastleProvider) Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            log.info("已注册 BouncyCastle Provider（{} v{}）— SM2/SM3/SM4 国密可用",
                    BouncyCastleProvider.PROVIDER_NAME, provider.getVersion());
        } else {
            log.debug("BouncyCastle Provider 已存在（{} v{}），跳过重复注册",
                    BouncyCastleProvider.PROVIDER_NAME, provider.getVersion());
        }
        return provider;
    }
}
