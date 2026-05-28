package com.medkernel.engine.tenant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 租户定制个性化品牌 REST 控制器。
 *
 * <p>提供 GET 读取与 POST 品牌修改接口。
 */
@RestController
@RequestMapping("/api/v1/platform/branding")
@DataScope(requireTenant = true)
public class BrandingController {

    private final TenantPilotService service;

    public BrandingController(TenantPilotService service) {
        this.service = service;
    }

    private String requireTenantId() {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw ApiException.tenantMissing();
        }
        return tenantId;
    }

    /**
     * 获取当前租户的品牌是个性化配置。
     *
     * @return 统一返回格式包
     */
    @GetMapping
    @PreAuthorize("@perm.has('tenant.read')")
    public ApiResult<Branding> getBranding() {
        String tenantId = requireTenantId();
        return ApiResult.ok(service.getBranding(tenantId));
    }

    /**
     * 更新当前租户的品牌个性化配置。
     *
     * @param dto 输入配置
     * @return 统一返回格式包
     */
    @PostMapping
    @PreAuthorize("@perm.has('tenant.write')")
    public ApiResult<Branding> saveBranding(@Valid @RequestBody BrandingUpdateDto dto) {
        String tenantId = requireTenantId();
        Branding input = new Branding(
            null,
            tenantId,
            dto.hospitalName(),
            dto.logoUrl(),
            dto.themeColor(),
            dto.expertMode(),
            dto.customBrandingJson(),
            null, null, null, null
        );
        return ApiResult.ok(service.saveBranding(tenantId, input));
    }

    /**
     * 品牌更新输入传输对象。
     */
    public record BrandingUpdateDto(
        @NotBlank(message = "医院物理名称不能为空")
        @Size(max = 128, message = "医院名称长度超限")
        String hospitalName,

        @Size(max = 512, message = "Logo图片URL过长")
        String logoUrl,

        @Size(max = 32, message = "主题色值不合法")
        String themeColor,

        Boolean expertMode,

        @Size(max = 4000, message = "品牌扩展配置JSON超出最大限制")
        String customBrandingJson
    ) {}
}
