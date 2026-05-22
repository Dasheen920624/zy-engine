package com.medkernel.common.crypto;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCE 无限强度加密启用器（Java 8 早期版本兼容补丁）。
 *
 * <p>Java 8u151 之前的 JDK 默认启用「Cryptographic Jurisdiction Policy」限制，
 * 把对称密钥强度限制为 128 bit，使 SM2 内部对称加密 / 较长密钥 AES 等抛
 * {@code java.security.InvalidKeyException: Illegal key size or default parameters}。
 *
 * <p>从 8u151 起 Oracle 默认启用无限强度（{@code crypto.policy=unlimited}），
 * 但本项目仍兼容 8u51 等老 JDK（医院内网常见过时镜像），通过反射移除
 * {@code javax.crypto.JceSecurity#isRestricted} 标记达到等价效果。
 *
 * <h2>触发时机</h2>
 * <ul>
 *   <li>{@link SmCryptoConfig} 在 Spring 上下文启动时调用一次（容器内）。</li>
 *   <li>{@link SmCryptoService} 在 @BeforeAll 测试 hook 中调用一次（单测内）。</li>
 * </ul>
 *
 * <p>幂等：内部 {@code ENABLED} 标志位防止重复反射。
 *
 * <p>失败行为：日志降级为 WARN，不抛异常 — 因为 8u151+ JDK 上反射可能找不到字段
 * （字段在 9+ 被移除），但那种 JDK 本身就 unlimited，无需修复。
 *
 * @see <a href="https://www.oracle.com/java/technologies/javase-jce8-downloads.html">JCE Policy Files</a>
 */
final class JceUnlimitedStrengthEnabler {

    private static final Logger log = LoggerFactory.getLogger(JceUnlimitedStrengthEnabler.class);

    private static volatile boolean ENABLED = false;

    private JceUnlimitedStrengthEnabler() {
        // utility
    }

    /**
     * 启用 JCE 无限强度。多次调用安全 — 实际反射仅执行一次。
     */
    static synchronized void enable() {
        if (ENABLED) {
            return;
        }
        try {
            Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            // 字段 1: isRestricted (boolean)
            Field isRestricted = jceSecurity.getDeclaredField("isRestricted");
            isRestricted.setAccessible(true);
            removeFinalModifier(isRestricted);
            isRestricted.set(null, false);

            // 字段 2: defaultPolicy (PermissionCollection) — 加 AllPermission
            Field defaultPolicy = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicy.setAccessible(true);
            PermissionCollection policy = (PermissionCollection) defaultPolicy.get(null);

            // CryptoAllPermission 是 javax.crypto 包私有，反射拿
            Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoAllPermission");
            Field instance = cryptoPermissions.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            policy.add((Permission) instance.get(null));

            ENABLED = true;
            log.info("JCE Unlimited Strength 已启用（绕过 JDK 1.8 早期 128-bit 限制）");
        } catch (ClassNotFoundException e) {
            // 9+ JDK 没有 javax.crypto.JceSecurity，本身就是 unlimited
            ENABLED = true;
            log.debug("JceSecurity 不存在（JDK 9+ 默认 unlimited），跳过");
        } catch (NoSuchFieldException e) {
            // 较新 8 JDK（8u151+）已经默认 unlimited，字段可能已移除
            ENABLED = true;
            log.debug("JCE 限制字段已不存在（JDK 8u151+ 默认 unlimited），跳过");
        } catch (Exception e) {
            log.warn("自动启用 JCE Unlimited Strength 失败，"
                    + "若 SM2 加密报 Illegal key size，请升级 JDK 至 8u151+ 或手动安装 policy", e);
        }
    }

    private static void removeFinalModifier(Field field) throws ReflectiveOperationException {
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {
            // 9+ 移除了 modifiers 字段；那种 JDK 本身已 unlimited
        }
    }
}
