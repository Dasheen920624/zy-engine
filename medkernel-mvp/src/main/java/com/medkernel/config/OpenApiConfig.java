package com.medkernel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 全局配置（PR-FINAL-20）。
 *
 * <p>Swagger UI 访问路径：{@code /medkernel/api-docs}（仅 management 端口 18081 暴露；
 * 生产环境通过 Nginx 屏蔽业务端口 18080 下的 /api-docs 路径）。
 *
 * <p>安全方案：HTTP Bearer（JWT）——请求头格式为：
 * {@code Authorization: Bearer <token>}
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI medkernelOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("MedKernel 集团医疗智能中枢 API")
                .description("专科诊疗路径引擎 REST API — v1.0 GA\n\n"
                        + "**鉴权**：调用受保护端点前请先 POST /api/auth/login 获取 JWT，"
                        + "然后在 Authorize 按钮中填入 Bearer token。\n\n"
                        + "**数据规范**：所有字段均为 snake_case；时间戳为 ISO-8601 字符串。")
                .version("1.0.0")
                .contact(new Contact()
                        .name("MedKernel 工程团队")
                        .email("dev@medkernel.internal"))
                .license(new License()
                        .name("内部专有协议 · Internal Proprietary")
                        .url("https://medkernel.internal/license"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("从 POST /api/auth/login 获取的 JWT token，"
                        + "在请求头中以 'Authorization: Bearer <token>' 形式传递。");
    }
}
