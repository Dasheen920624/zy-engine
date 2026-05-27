package com.medkernel.engine.embed;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * 嵌入启动令牌存储库。
 *
 * <p>提供根据 token 查询和持久化启动令牌的接口方法。
 */
public interface EmbedLaunchTokenRepository extends CrudRepository<EmbedLaunchToken, Long> {

    /**
     * 根据启动令牌查询实体。
     *
     * @param token 安全启动令牌
     * @return 启动令牌实体实例包装
     */
    Optional<EmbedLaunchToken> findByToken(String token);
}
