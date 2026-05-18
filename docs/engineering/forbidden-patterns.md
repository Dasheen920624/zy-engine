# 禁用模式清单

> 列出"绝对不能这么写"的模式。**这些是硬性约束，不允许任何例外。**  
> 强制方式：ESLint 自定义规则、CI 检查、code review checklist、`verify-pr.ps1` 自动检测。
>
> ℹ️ **本文档为"禁用规则讲解"，会出现 `zy-engine` `ZyEngine` `--zy-` 等已禁用名称用作示例。这是合法的自我引用 — 文档必须能"说出"旧名才能教别人"禁用"。除本文档、ESLint 规则文件、verify-pr.ps1 之外，其它任何位置出现这些名称都属违规。**

---

## 1. 视觉硬编码（前端）

### 1.1 禁止硬编码颜色

❌ 错误示例：
```tsx
<div style={{ color: '#1890ff' }}>主要</div>
<Button style={{ background: 'rgb(255, 0, 0)' }}>错误</Button>
```

```css
.my-button {
  color: #1890ff;  /* ❌ */
  background: rgb(255, 0, 0);  /* ❌ */
}
```

✅ 正确：
```tsx
<div style={{ color: 'var(--mk-primary)' }}>主要</div>
<Button className="mk-button-danger">错误</Button>
```

```css
.my-button {
  color: var(--mk-primary);
  background: var(--mk-danger);
}
```

**例外：** 仅 `frontend/src/styles/tokens.css` 允许 hex/rgb 定义。

**强制方式：** ESLint `no-hardcoded-color.js`。

### 1.2 禁止硬编码字号/间距

❌ 错误：`padding: 16px`、`font-size: 14px`  
✅ 正确：`padding: var(--mk-space-4)`、`font-size: var(--mk-text-base)`

### 1.3 禁止直接用 Ant Design 默认色

❌ 错误：依赖 `<Button type="primary">` 默认蓝色  
✅ 正确：在 `App.tsx` 用 `<ConfigProvider theme={{ token: { colorPrimary: 'var(--mk-primary)' } }}>` 覆盖

---

## 2. 类型安全（TypeScript）

### 2.1 禁止 `any` 类型

❌ 错误：`function handle(data: any) { ... }`  
✅ 正确：`function handle<T extends BaseDto>(data: T) { ... }` 或 `data: unknown` + 类型守卫

**强制方式：** `tsconfig.json` 启用 `strict`、`noImplicitAny`、`strictNullChecks`。

### 2.2 禁止 `// @ts-ignore`

❌ 错误：`// @ts-ignore`  
✅ 正确：`// @ts-expect-error <理由>`（必须有理由说明）

---

## 3. 后端 API（Spring Boot）

### 3.1 禁止跳过 ApiResult 包装

❌ 错误：
```java
@GetMapping("/users")
public List<User> list() { return repo.findAll(); }
```

✅ 正确：
```java
@GetMapping("/users")
public ApiResult<List<UserDto>> list() {
    return ApiResult.success(service.list(), TraceContext.current());
}
```

### 3.2 禁止 Controller 直接调 Repository

❌ 错误：Controller `@Autowired UserRepository repo`  
✅ 正确：Controller → Service → Repository（强制三层）

### 3.3 禁止业务逻辑写在 Controller

❌ 错误：Controller 里写 if-else 业务判断  
✅ 正确：Controller 只做参数解析 + 调用 Service + 包装响应

### 3.4 禁止吞异常

❌ 错误：`catch (Exception e) { return null; }`  
✅ 正确：`catch (BusinessException e) { throw new ApiException(ErrorCode.XXX, e); }`

### 3.5 禁止日志输出敏感信息（不变量 #10）

❌ 错误：`log.info("password: {}", password)`  
✅ 正确：`log.info("login attempt for user: {}", username)`（不打密码）

禁止字段：密码、API Key、患者完整身份证号、手机号（必须脱敏）

---

## 4. 医学/医保/质控（业务规则）

### 4.1 禁止缺来源直接发布（ADR-0004）

❌ 错误：跳过 `SourceReviewChecker` 直接调 `publish()`  
✅ 正确：发布前必须查 `source_review`，缺则返回 `MISSING_SOURCE`

### 4.2 禁止跳过医生确认（不变量 #6）

❌ 错误：阻断类规则自动执行  
✅ 正确：必须有 `action_mode=BLOCK` + `doctor_decision` 字段，医生填理由后才能继续

### 4.3 禁止规则发布缺审核

❌ 错误：`rule.publish()` 不检查 `review_status`  
✅ 正确：必须 `review_status='APPROVED'` 才允许发布

---

## 5. 数据完整性

### 5.1 禁止业务表缺 `tenant_id`

❌ 错误：`CREATE TABLE my_table (id, name, ...)` 没有 tenant_id  
✅ 正确：所有业务表必须含 `tenant_id VARCHAR(64) NOT NULL`

### 5.2 禁止写操作缺 `created_by`

❌ 错误：`INSERT INTO my_table (name) VALUES (?)` 缺 created_by  
✅ 正确：`created_by` 必须从 `SecurityContextHolder.getPlatformUserId()` 取

### 5.3 禁止物理删除审计日志

❌ 错误：`DELETE FROM ENGINE_AUDIT_LOG WHERE ...`  
✅ 正确：审计表禁止 DELETE 和 UPDATE（数据库触发器拦截）

---

## 6. 多 AI 协作

### 6.1 禁止修改非独占文件

❌ 错误：领了 PR-V2-01 顺手改了 frontend/src/pages/Dashboard.tsx  
✅ 正确：只改 active claim 声明的独占文件清单内的文件

### 6.2 禁止覆盖其它 AI 未提交工作

❌ 错误：看到 git status 有别人改动直接 `git reset --hard`  
✅ 正确：发现未提交改动 → 询问用户、不动它

### 6.3 禁止跳过 active claim

❌ 错误：直接改代码不创建 claim  
✅ 正确：领任务 → push active claim → 才能改代码

---

## 7. 命名空间

### 7.1 禁止使用 `ZyEngine*` / `zy-engine` 等历史标识

V2 重命名为 MedKernel 后：

❌ 错误：`class ZyEngineApplication`、`/zy-engine/api`、`com.zyengine.*`  
✅ 正确：`class MedKernelApplication`、`/medkernel/api`、`com.medkernel.*`

**例外：** CSS Design Token `--mk-*` 保留（设计 token 命名空间，与项目名巧合）

### 7.2 禁止使用历史 FE-XXX 编号作 V2 任务主键

V2 用 `PR-V2-XX` 命名空间，与历史 FE-XXX 隔离（ADR-0002）。

---

## 8. 工程纪律

### 8.1 禁止 `console.log` 进 production

❌ 错误：`console.log('debug')` 提交到 main  
✅ 正确：开发用 `console.log`，提交前删除或改 `console.debug`（CI 检测）

### 8.2 禁止用 `alert()` `confirm()` `prompt()`

❌ 错误：`if (confirm('确认？')) { ... }`  
✅ 正确：用 `<Modal.confirm>` 或 V2 `<DangerConfirm>`

### 8.3 禁止 emoji 替代功能

❌ 错误：用 🚀 代表"发布"按钮  
✅ 正确：明确文字"发布" + 可选图标。emoji 只用于状态标签（如 ⚠ ✅）和 AI 标识 🤖

### 8.4 禁止超长函数

单个函数 > 80 行必须拆分（ESLint `max-lines-per-function`）。

### 8.5 禁止超长文件

单文件 > 500 行必须拆分（ESLint `max-lines`）。

---

## 9. 文档纪律

### 9.1 禁止引用 `docs/_archive/` 内文档

`_archive/` 已物理删除（commit `631f08c`），如发现引用必修。

### 9.2 禁止"先实现后补设计"

新增功能必须先更新 V2 [`01_产品事实源.md`](../01_产品事实源.md) §5 能力清单 + [`04_页面规格书.md`](../04_页面规格书.md)。

### 9.3 禁止跳过 ADR

修改架构必须先开 ADR（用 [`adr/template.md`](adr/template.md) 模板）。

---

## 强制方式汇总

| 禁用项 | 强制方式 |
|---|---|
| 硬编码颜色 | ESLint `no-hardcoded-color` |
| `any` 类型 | TypeScript strict + `noImplicitAny` |
| `// @ts-ignore` | ESLint `ban-ts-comment` |
| 跳过 ApiResult | Java 注解处理器（PR-V2-04 中实现） + code review |
| Controller 调 Repo | Spring 启动时扫描注入关系 |
| 缺来源发布 | Java `SourceReviewChecker` 强制 |
| 缺医生确认 | Java `ActionModeValidator` 强制 |
| 缺 tenant_id | 数据库 NOT NULL 约束 |
| 改非独占文件 | `check-ai-collaboration.ps1` |
| 用 `ZyEngine*` | `verify-pr.ps1` grep 检测 |
| console.log | ESLint `no-console` + CI grep |
| alert() | ESLint `no-alert` |
| 引用 _archive | `verify-pr.ps1` grep 检测 |

`verify-pr.ps1` 集成所有 grep 类检测，提交前必跑。
