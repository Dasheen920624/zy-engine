package com.medkernel.engine.embed;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * 嵌入Origin安全域名白名单存储库。
 *
 * <p>管理租户级别 Origin 域白名单的持久化及安全阻断校验。
 */
public interface EmbedOriginWhitelistRepository extends CrudRepository<EmbedOriginWhitelist, Long> {

    /**
     * 根据租户ID拉取所有的授权域名白名单列表。
     *
     * @param tenantId 租户ID
     * @return 租户域名白名单列表
     */
    List<EmbedOriginWhitelist> findByTenantId(String tenantId);

    /**
     * 根据租户ID与域名Origin查询白名单是否存在。
     *
     * @param tenantId 租户ID
     * @param origin 域名Origin（如 https://his.hospital.com）
     * @return 域名白名单实例包装
     */
    Optional<EmbedOriginWhitelist> findByTenantIdAndOrigin(String tenantId, String origin);
}
