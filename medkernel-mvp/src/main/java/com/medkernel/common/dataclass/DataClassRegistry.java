package com.medkernel.common.dataclass;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

/**
 * 数据分级注册表：启动时扫描 {@code com.medkernel} 包下所有 {@link DataClass}
 * 标注的类，建立 entity → DataClassification 映射，并打印合规审计行。
 *
 * <p>本注册表是一种「自检设施」：
 * <ul>
 *   <li>合规审计可一眼看到哪些 entity 是 HEALTH_DATA / SENSITIVE</li>
 *   <li>启动日志中固化分级清单，便于 release 验收</li>
 *   <li>提供 {@link #classOf(Class)} 给 Service 层做分级检查</li>
 * </ul>
 *
 * <h2>性能</h2>
 *
 * <p>扫描仅在 ApplicationStartedEvent 触发一次（Spring Boot 启动完成后），
 * 后续访问直接走 {@link ConcurrentHashMap}。
 *
 * @see DataClass
 */
@Service
public class DataClassRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataClassRegistry.class);
    private static final String SCAN_BASE_PACKAGE = "com.medkernel";

    private final Map<Class<?>, DataClassification> registry = new ConcurrentHashMap<>();

    /**
     * 查询类型的分级。无 {@link DataClass} 标注时返回 {@link DataClassification#INTERNAL}。
     */
    public DataClassification classOf(Class<?> type) {
        DataClassification cached = registry.get(type);
        if (cached != null) {
            return cached;
        }
        DataClass annotation = type.getAnnotation(DataClass.class);
        DataClassification result = annotation != null ? annotation.value() : DataClassification.INTERNAL;
        registry.putIfAbsent(type, result);
        return result;
    }

    /**
     * 启动时扫描全部 entity 并记录到日志。
     *
     * <p>仅扫描类名以 {@code Entity} 结尾或带 {@link DataClass} 注解的类，
     * 减少 ClassNotFoundException 噪音。
     */
    @EventListener(ApplicationStartedEvent.class)
    public void scanAndReport() {
        Map<Class<?>, DataClassification> discovered = new LinkedHashMap<>();
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(SCAN_BASE_PACKAGE) + "/**/*.class";
            org.springframework.core.io.Resource[] resources = resolver.getResources(pattern);
            for (org.springframework.core.io.Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                String className = reader.getClassMetadata().getClassName();
                if (!reader.getAnnotationMetadata().hasAnnotation(DataClass.class.getName())) {
                    continue;
                }
                try {
                    Class<?> type = Class.forName(className, false, getClass().getClassLoader());
                    DataClass annotation = type.getAnnotation(DataClass.class);
                    if (annotation != null) {
                        discovered.put(type, annotation.value());
                        registry.putIfAbsent(type, annotation.value());
                    }
                } catch (ClassNotFoundException ignored) {
                    // skip — classpath edge case (test class without main runtime)
                }
            }
        } catch (Exception e) {
            log.warn("DataClassRegistry 扫描失败（不影响业务，仅缺少启动日志）", e);
            return;
        }
        log.info("=== 数据分级清单（合规审计）===");
        log.info("共扫描到 {} 个标注 @DataClass 的类：", discovered.size());
        discovered.forEach((type, level) -> {
            int encryptedCount = countEncryptedFields(type);
            log.info("  - [{}] {} （加密字段：{}）", level, type.getName(), encryptedCount);
        });
        log.info("=== 数据分级清单结束 ===");
    }

    /**
     * 不可变快照（按字段顺序遍历）。
     */
    public Map<Class<?>, DataClassification> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registry));
    }

    private static int countEncryptedFields(Class<?> type) {
        int n = 0;
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(Encrypted.class)) {
                    n++;
                }
            }
            current = current.getSuperclass();
        }
        return n;
    }
}
