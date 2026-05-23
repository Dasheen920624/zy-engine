# 后端 REST Controller 参考实现

> 用途：实现任何 REST Controller 时复制本样板。  
> 关联 PR：PR-V2-04 SEC-001 用户体系（及所有后端 PR）

## 文件结构

```
medkernel-mvp/src/main/java/com/medkernel/<domain>/
├── XxxController.java       REST 端点
├── XxxService.java          业务逻辑
├── XxxEntity.java           持久化实体（如需要）
├── XxxRepository.java       数据访问（如需要）
└── XxxDto.java              传输对象（如需要）

medkernel-mvp/src/test/java/com/medkernel/<domain>/
└── XxxControllerTest.java   契约测试（@SpringBootTest + MockMvc）
```

## 1. Controller 标准模式

```java
package com.medkernel.user;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户管理 REST 端点。
 * 不变量：
 * - 所有写操作必须经 SecurityFilter 注入的 platform_user_id（不变量 #2）
 * - 所有响应必须包 ApiResult + trace_id
 * - 所有写操作必须写 ENGINE_AUDIT_LOG
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final OrganizationContextService orgService;

    @Autowired
    public UserController(UserService userService, OrganizationContextService orgService) {
        this.userService = userService;
        this.orgService = orgService;
    }

    /**
     * 查询用户列表（支持组织过滤）。
     */
    @GetMapping
    public ApiResult<UserListResponse> list(
            @RequestHeader HttpHeaders headers,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 1. 解析组织上下文（Header 全集）
        OrganizationContext ctx = orgService.resolveFromHeaders(headers);

        // 2. traceId（已由 TraceFilter 注入到 MDC）
        String traceId = TraceContext.current();

        // 3. 业务逻辑
        UserListResponse response = userService.list(ctx, search, page, size);

        // 4. 返回统一封装
        return ApiResult.success(response, traceId);
    }

    /**
     * 创建用户（写操作示例）。
     */
    @PostMapping
    public ApiResult<UserDto> create(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody CreateUserRequest request) {

        OrganizationContext ctx = orgService.resolveFromHeaders(headers);
        String traceId = TraceContext.current();
        Long actorId = SecurityContextHolder.getPlatformUserId();  // 来自 SecurityFilter

        try {
            UserDto created = userService.create(ctx, request, actorId);
            log.info("[traceId={}] User created: id={}, actor={}", traceId, created.getId(), actorId);
            return ApiResult.success(created, traceId);
        } catch (ValidationException ex) {
            log.warn("[traceId={}] Validation failed: {}", traceId, ex.getMessage());
            return ApiResult.error(ErrorCode.VALIDATION_ERROR, ex.getMessage(), traceId);
        } catch (DuplicateException ex) {
            return ApiResult.error(ErrorCode.VALIDATION_ERROR, "用户名已存在", traceId);
        }
        // UNKNOWN_ERROR 由 GlobalExceptionHandler 统一处理
    }
}
```

## 2. Service 标准模式

```java
package com.medkernel.user;

import com.medkernel.audit.AuditService;
import com.medkernel.organization.OrganizationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository repository;
    private final AuditService auditService;

    public UserService(UserRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * 创建用户。
     * 不变量：
     * - tenant_id 必须从 OrgContext 写入
     * - created_by 必须从 actorId 写入
     * - 必须写审计
     */
    @Transactional
    public UserDto create(OrganizationContext ctx, CreateUserRequest req, Long actorId) {
        // 1. 业务校验
        if (repository.existsByUsername(ctx.getTenantId(), req.getUsername())) {
            throw new DuplicateException("username already exists");
        }

        // 2. 构造实体（注入 tenant_id + created_by）
        UserEntity entity = new UserEntity();
        entity.setTenantId(ctx.getTenantId());
        entity.setUsername(req.getUsername());
        entity.setPasswordHash(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        entity.setCreatedBy(actorId);
        entity.setCreatedAt(Instant.now());

        // 3. 持久化
        UserEntity saved = repository.save(entity);

        // 4. 写审计（强制）
        auditService.write(AuditAction.USER_CREATE, actorId, ctx, "user_id=" + saved.getId());

        // 5. 转 DTO
        return UserDto.from(saved);
    }
}
```

## 3. 契约测试模式

```java
package com.medkernel.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void list_should_return_api_result_with_trace_id() throws Exception {
        mvc.perform(get("/api/users")
                .header("X-Tenant-Id", "TENANT_DEMO")
                .header("X-Hospital-Code", "HOSPITAL_DEMO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.trace_id").isNotEmpty())
            .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void create_with_invalid_username_should_return_validation_error() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("");  // invalid

        mvc.perform(post("/api/users")
                .contentType("application/json")
                .header("X-Tenant-Id", "TENANT_DEMO")
                .content(json.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_should_write_audit_log() throws Exception {
        // ... 创建用户
        // 验证 ENGINE_AUDIT_LOG 表有新记录
    }
}
```

## 关键约束（不许变）

1. ✅ Controller 必须返回 `ApiResult<T>`，不允许裸返回 entity
2. ✅ 必须解析 OrgContext 并传给 Service
3. ✅ 必须有 `traceId` 串联日志
4. ✅ 写操作必须 `@Transactional` + 写 AUDIT_LOG
5. ✅ 必须有契约测试（@SpringBootTest + MockMvc）
6. ✅ 业务异常用 `ErrorCode.*`，未知错误走 GlobalExceptionHandler
7. ✅ 日志不允许输出密码、API Key、患者完整身份（不变量 #10）
8. ✅ 实体表必须有 `tenant_id` `created_by` `created_at` `is_deleted` 字段（不变量 #1, #2）

## 禁止模式

- ❌ Controller 直接调 Repository（必须经 Service）
- ❌ 业务逻辑写在 Controller（必须在 Service）
- ❌ Service 直接 throw RuntimeException（必须用 ErrorCode + 业务异常）
- ❌ 不写审计（写操作必须写）
- ❌ 用 `System.out.println`（用 Logger）

## 相关文档

- 后端规范：[`docs/engineering/06_后端开发规范.md`](../06_后端开发规范.md)
- 22 不变量：[`docs/01_产品事实源.md §7`](../../01_产品事实源.md#7-22-个不变量任何-ai-不得违反)
- API 错误码：[`docs/01_产品事实源.md §8.2`](../../01_产品事实源.md)
