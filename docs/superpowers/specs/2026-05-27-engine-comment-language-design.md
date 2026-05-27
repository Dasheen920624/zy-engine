# 引擎层中文注释治理设计

> 状态：当前有效设计
> 适用范围：`medkernel-backend` 引擎层 Java 代码与生产方言 SQL 迁移脚本

## 1. 背景与现状

仓库的 `docs/DOCUMENTATION_LANGUAGE_POLICY.md` 把"代码块、SQL"列入"可以保留英文"，因此对代码注释和数据库表注释**没有任何强制要求**。

`main` 分支量化现状：

- Java 源码 335 个文件，含中文 156 个（≈47%）。但分布严重不均：
  - 老模块（`shared/*`、`engine/context`、`engine/knowledge`、`engine/security`、`engine/org`、`compliance/audit`）覆盖率 90%+
  - 2026-05-27 前后批量生成的 5 个新引擎 API 模块覆盖率极低：
    - `engine/evaluation` 1/39
    - `engine/pathway` 2/49
    - `engine/recommendation` 1/29
    - `engine/rule` 2/33
    - `engine/terminology` 1/21
- SQL 迁移 70 个文件，仅 12 个用 `COMMENT ON`，且全部在 V2–V7 早期迁移；V8–V14（observability/audit_event_outcome/clinical_event/rule/pathway/recommendation/evaluation）一律没补。

读起来困难的原因：新模块大量裸代码（无 Javadoc、无行注释），新表只有英文字段名 + 英文 CHECK 枚举。

## 2. 目标与非目标

**目标**：

- 引擎层（`com.medkernel.engine.**`、`com.medkernel.shared.**`）公共类与公共方法的 Javadoc 一律使用简体中文，覆盖业务含义、关键约束、抛出的 `ApiException` 错误码。
- Oracle / PostgreSQL / Kingbase 迁移脚本中**所有 `CREATE TABLE`** 至少包含 `COMMENT ON TABLE`；**所有枚举列、状态列、外键列**包含 `COMMENT ON COLUMN`。
- CI 设置软门禁：新增/修改的引擎层文件必须满足上述要求（fail）；存量缺口仅 warn，不阻断 PR。

**非目标**：

- 不强制 record 字段、private 方法、DTO 字段逐项 Javadoc。
- 不动 H2 迁移脚本（仅测试 baseline，文件首行中文标题已足够）。
- 不改前端 TypeScript 注释规则（按 `frontend/README` 现行约定）。
- 不重写已有英文注释（少数 `shared/*` 历史英文残留留待自然演进）。

## 3. 方案

### 3.1 PR 拆分

| PR | 范围 | 体量 |
|---|---|---|
| PR1 | 规范文档 + CI 软门禁 | ~5 文件 |
| PR2 | `engine/terminology` Javadoc + V4 顺手回补 | 21 java + 3 sql |
| PR3 | `engine/recommendation` Javadoc + V13 COMMENT | 29 java + 3 sql |
| PR4 | `engine/rule` Javadoc + V11 COMMENT | 33 java + 3 sql |
| PR5 | `engine/evaluation` Javadoc + V14 COMMENT | 39 java + 3 sql |
| PR6 | `engine/pathway` Javadoc + V12 COMMENT | 49 java + 3 sql |

PR1 必须先合并，否则后续 PR 会被 CI 新增门禁挡住（diff 中新文件不含中文 Javadoc 会 fail，但这些 PR 本身就是要补中文，所以不会被挡）。

PR2-6 互相独立、可并行 review。顺序选"小先大后"，让第一个真实存量 PR 在最小模块上暴露 CI 误判。

V2-V7 oracle/postgres 早期残缺的 COMMENT，按业务相关性挂到对应模块 PR：

- V4 terminology → PR2
- V5 audit_chain、V6 security_permission、V7 clinical_context 历史 PR 已补充充分，不再回扫；如 CI warn 提示缺口再单独治理。

### 3.2 Javadoc 模板

**类级**（每个 .java 必须）：

```java
/**
 * GA-ENG-API-08 评估指标定义实体（DRAFT→PUBLISHED→ACTIVE 三态）。
 *
 * <p>承担质控管线"指标版本控制"的写库形态：tenant + indicator_code + version_no 全局唯一，
 * 由 {@link EvaluationEngineService#createIndicator} 创建、{@link EvaluationEngineService#publishIndicator}
 * 推进到 PUBLISHED；激活由 {@code EvaluationEngineService#activateIndicator} 切到 ACTIVE。
 */
```

要点：
- 第一行：业务身份 + 关键状态机/约束的一句话总结。
- 可选 `<p>` 段：说明上下文协作类、关键不变量、关键 API 入口（用 `{@link}` 链接）。
- 枚举类至少列出所有枚举值含义。

**public 方法级**（Controller / Service 的公共方法必须）：

```java
/**
 * 创建评估指标草稿。
 *
 * <p>租户必须存在；同租户 indicator_code 唯一约束触发时抛 {@link ApiException} 错误码 ENG_EVAL_002。
 */
```

要点：
- 一句话说明做什么（动作 + 对象）。
- 关键前置条件、关键并发约束、可能抛出的 `ApiException` 错误码（不需要罗列全部，只列业务级失败）。

**Repository / DTO Request·Response**：

- Repository：类级一行 + 自定义查询方法各一行（说明业务意图）。
- DTO Request/Response：仅类级一行（"XXX 入参/出参，字段语义见 API spec"）。

**不要求**：

- record 字段、private 方法、`equals/hashCode/toString` 等模板方法、`getter/setter`、Spring 生命周期方法。

### 3.3 SQL COMMENT 模板

```sql
COMMENT ON TABLE evaluation_indicator IS '评估指标定义：DRAFT→PENDING_REVIEW→PUBLISHED→ACTIVE 全生命周期；tenant_id + indicator_code + version_no 唯一';

COMMENT ON COLUMN evaluation_indicator.subject_type IS '评估对象类型：PATIENT/MEDICAL_RECORD/DEPARTMENT/DOCTOR/DISEASE/PATHWAY/CLAIM/FOLLOWUP';
COMMENT ON COLUMN evaluation_indicator.status        IS '指标状态机：DRAFT 草稿 / PENDING_REVIEW 待评审 / PUBLISHED 已发布 / ACTIVE 生效 / OFFLINE 下线 / ARCHIVED 归档';
COMMENT ON COLUMN evaluation_indicator.indicator_id  IS '指标全局 ID（业务键，跨租户唯一）';
```

要点：
- 每个 `CREATE TABLE` 必有 `COMMENT ON TABLE`，说明业务身份 + 关键唯一约束。
- 枚举列 / 状态列 / 业务键列 / 外键列必有 `COMMENT ON COLUMN`，枚举值逐个中文化。
- 通用列（`created_at`、`created_by`、`updated_at`、`updated_by`、`trace_id`、自增 `id`）免注。

H2 迁移不补 `COMMENT ON`（文件首行中文标题即可）。

### 3.4 CI 软门禁

新增脚本 `scripts/check-comment-zh.sh`（bash），三项检查：

**检查 1（fail：仅新增文件）**：新增 Java 类必须含中文类级 Javadoc

```bash
# 仅看本次 PR 新增的 .java 文件（Added，不含 Modified）
git diff --name-only --diff-filter=A origin/main...HEAD -- 'medkernel-backend/src/main/java/com/medkernel/engine/**/*.java' \
                                                           'medkernel-backend/src/main/java/com/medkernel/shared/**/*.java'
```

对每个新增文件做：抓 `public (class|record|interface|enum)` 上方紧邻的 `/** ... */`，若该 Javadoc 不含 `[\x{4e00}-\x{9fa5}]` 字符 → 退出码 1。

**检查 2（fail：仅新增文件）**：新增 SQL 迁移必须含中文 `COMMENT ON TABLE`

```bash
git diff --name-only --diff-filter=A origin/main...HEAD -- 'medkernel-backend/src/main/resources/db/migration/oracle/**/*.sql' \
                                                           'medkernel-backend/src/main/resources/db/migration/postgres/**/*.sql' \
                                                           'medkernel-backend/src/main/resources/db/migration/kingbase/**/*.sql'
```

新增文件 grep 全部 `CREATE TABLE (\w+)`，要求同文件出现匹配的 `COMMENT ON TABLE \1 IS '` 且 IS 后含中文字符 → 否则 fail。

**检查 3（warn-only）**：存量与修改文件覆盖率

两个子项均仅写 GitHub Actions step summary，不影响退出码：
- 修改但未新增的 .java/.sql 文件，按检查 1/2 同样规则检测，列出缺口让 reviewer 关注（"顺手补"）。
- 扫描全量 `engine/**.java` 与 oracle/postgres/kingbase 全量迁移，按模块/方言打印中文覆盖率统计。

集成到 `.github/workflows/ci.yml`，新增 job `comment-language-check`，依赖现有 maven build job。脚本本身也提供 `--mode=local` 选项让开发者本地跑（默认与 `origin/main` 比较）。

### 3.5 规范文档更新

**`AGENTS.md`** § 语言要求新增条目：

```
- `medkernel-backend` 引擎层（`com.medkernel.engine.**` 与 `com.medkernel.shared.**`）公共类、公共方法的 Javadoc，以及 Oracle/PostgreSQL/Kingbase 迁移脚本中新增表与枚举列的 `COMMENT ON`，必须使用简体中文。前端 TypeScript 注释仍按 `frontend/README` 既有约定，不在此次治理范围。
```

**`docs/DOCUMENTATION_LANGUAGE_POLICY.md`**：
- § 2「必须使用中文的内容」尾部新增同上条目。
- § 3「可以保留英文的内容」第 22 行原文「JSON / YAML / SQL / TypeScript / Java 代码块」改为「JSON / YAML 配置片段、代码标识符、SQL 字段名」，避免被误读为"代码不用中文也行"。

## 4. 数据流与失败模式

| 场景 | CI 行为 |
|---|---|
| 新建 `EngineFooController.java` 无类级 Javadoc | 检查 1 fail |
| 新建 `EngineFooController.java` 有英文 Javadoc | 检查 1 fail（不含中文字符） |
| 新建表 `engine_foo` 无 `COMMENT ON TABLE` | 检查 2 fail |
| 改动现有 `RecommendationCard.java`，原本就没中文 Javadoc | 检查 3 warn（step summary 提示缺口，但不阻断） |
| 改动现有迁移文件追加 `COMMENT ON COLUMN` 中文 | 检查 2 / 3 通过 |
| 完全不动后端、仅改前端/docs | 三个检查直接 skip |

存量改动只 warn 是 spec 设计的边界。若希望对存量也强制 fail，要等 PR2-6 全部合入 main、存量缺口已清零后再切。切换路径是改脚本的 `--diff-filter=A` 为 `AM` 并更新本 spec § 3.4。

## 5. 测试与验证

- PR1 自身：脚本提供 `--self-test` 用 `tests/scripts/check-comment-zh-fixtures/` 下的 fixture 验证 4 个用例（有中文/无 Javadoc/英文 Javadoc/SQL 缺 COMMENT）。
- PR1 合并前：手动跑 `scripts/check-comment-zh.sh --mode=full` 一次，确认存量警告统计与上面 § 1 的数据一致。
- PR2-6 合并前：每个 PR 必须看到 CI 中 `comment-language-check` 通过。
- 不需要写 JUnit 测试——脚本是 shell 检测工具，fixture 方式更直接。

## 6. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 中文 Javadoc 写得敷衍（"实体类。"） | spec 模板要求至少含业务身份和关键约束；review 时人工把关 |
| `git diff --diff-filter=AM` 与 PR base 不一致（merge 时 origin/main 已更新） | 脚本用 `git merge-base origin/main HEAD` 显式计算 base |
| Windows 开发者无 bash | 已存在 `scripts/*.ps1` 体系，但本次 CI 只跑 linux；本地校验给 PowerShell 移植作为 follow-up，不阻断当前治理 |
| 用 `[skip-comment-check]` 滥用 | 检查 1 即使被 skip 也输出 warning 行，review 时人工把关 |

## 7. 完成定义

- PR1-6 全部合并到 `origin/main`。
- 在 main 上跑一次完整 `scripts/check-comment-zh.sh --mode=full`，5 个目标模块覆盖率 100%（类 + 公共方法），oracle/postgres/kingbase 的 V8-V14 全部 `CREATE TABLE` 都有中文 `COMMENT ON TABLE`。
- 至少一次后续 PR 经过 CI `comment-language-check` 验证（任意业务 PR 都可）。
