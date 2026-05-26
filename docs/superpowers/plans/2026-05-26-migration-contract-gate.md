# 数据迁移合同门禁实施计划

> **供执行代理使用：** 实施本计划时必须使用 `superpowers:test-driven-development`，逐项执行并在完成后使用 `superpowers:verification-before-completion`。本次由 Codex 根据用户的连续执行授权在当前会话内实施。

**目标：** 为五方言 `V1` 至 `V6` 迁移建立可在普通 CI 阻断结构漂移的合同门禁，并将现有 Flyway smoke 收紧到完整基线。

**架构：** 测试层新增一个资源合同测试，直接读取五个迁移目录并检查版本、结构和字段不变量；现有可执行 Flyway 测试负责验证 H2 以及可提供容器时的 PostgreSQL/Oracle 解析执行。允许 Oracle/达梦数值布尔约束这一明确的方言差异。

**技术栈：** Java 21、JUnit 5、AssertJ、Flyway、Maven、SQL 资源文件。

---

### 任务 1：固定五方言静态合同

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/platform/migration/MigrationBaselineContractTest.java`

- [x] **步骤 1：编写合同测试**

新增测试类，从测试类路径定位 `db/migration/{dialect}` 后使用 `Files.list` 读取迁移资源，使用不区分大小写的正则提取 `CREATE TABLE`、`CREATE INDEX` 和 `CONSTRAINT` 名称，建立以下测试：

```java
@Test
void everyDialectPublishesTheSameAuthoritativeMigrationSequence() {
    for (String dialect : DIALECTS) {
        assertThat(migrationFiles(dialect)).containsExactlyElementsOf(EXPECTED_MIGRATIONS);
    }
}

@Test
void everyDialectPreservesRequiredTablesIndexesAndBusinessConstraints() {
    for (String dialect : DIALECTS) {
        String ddl = combinedDdl(dialect);
        assertThat(names(TABLE_PATTERN, ddl)).containsExactlyInAnyOrderElementsOf(REQUIRED_TABLES);
        assertThat(names(INDEX_PATTERN, ddl)).containsExactlyInAnyOrderElementsOf(REQUIRED_INDEXES);
        assertThat(names(CONSTRAINT_PATTERN, ddl)).containsAll(COMMON_CONSTRAINTS);
    }
}
```

- [x] **步骤 2：执行测试确认合同可识别当前真实结构**

运行：

```bash
mvn -B -Dtest=MigrationBaselineContractTest test
```

预期：若集合或允许的方言差异声明遗漏，测试先失败并显示具体表、索引或约束差异；补全合同声明后通过。该任务新增的是验证门禁本身，不修改业务运行代码。

- [x] **步骤 3：增加字段级不变量检查**

在同一测试类中按 `CREATE TABLE ... );` 表块检查租户字段、可变主数据和安全配置的四列审计字段，以及状态/版本实体的关键字段。运行作业和审计链头使用各自的生命周期审计字段，不纳入四列主数据集合：

```java
@Test
void tenantIsolationAuditAndLifecycleColumnsRemainPresent() {
    for (String dialect : DIALECTS) {
        Map<String, String> tables = tableBlocks(combinedDdl(dialect));
        TENANT_TABLES.forEach(table -> assertThat(tables.get(table)).contains("tenant_id"));
        MUTABLE_AUDITED_TABLES.forEach(table ->
            assertThat(tables.get(table)).contains("created_at", "created_by", "updated_at", "updated_by"));
    }
}
```

- [x] **步骤 4：复跑合同测试**

运行：

```bash
mvn -B -Dtest=MigrationBaselineContractTest test
```

预期：合同测试通过，若发现当前基线不符合声明则先修正当前五方言基线，再重新验证。

### 任务 2：收紧可执行 Flyway 门禁

**文件：**
- 修改：`medkernel-backend/src/test/java/com/medkernel/platform/migration/H2BaselineMigrationTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/platform/migration/FlywayMultiDialectSmokeTest.java`

- [x] **步骤 1：更新 H2 完整版本断言**

将 H2 测试命名和断言更新为完整基线：

```java
assertThat(result.migrationsExecuted).as("V1 至 V6 全部应用").isEqualTo(6);
assertThat(applied).extracting(info -> info.getVersion().getVersion())
    .containsExactly("1", "2", "3", "4", "5", "6");
```

- [x] **步骤 2：更新可运行多方言 smoke 断言**

将通用 `runFlyway` 的断言改为：

```java
assertThat(result.migrationsExecuted).as("%s 六个基线迁移执行", vendorName).isEqualTo(6);
assertThat(applied).extracting(info -> info.getVersion().getVersion())
    .containsExactly("1", "2", "3", "4", "5", "6");
```

- [x] **步骤 3：运行迁移专项测试**

运行：

```bash
mvn -B -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest,FlywayMultiDialectSmokeTest test
```

预期：H2 与静态合同通过；无容器环境下 PostgreSQL、Oracle 相关 smoke 被测试框架跳过且有明确记录。

### 任务 3：交付口径与完整验证

**文件：**
- 修改：`docs/backlog.md`

- [x] **步骤 1：更新中文台账**

将 `GA-ENG-BASE-05` owner 改为 `codex`、状态改为 `done`，新增修订记录，描述五方言合同、完整 Flyway 基线与允许方言差异。

- [x] **步骤 2：运行完成前验证**

运行：

```bash
mvn -B clean test
git diff --check
```

预期：后端全部可运行测试通过，容器缺失引起的既有跳过如实记录，差异无格式问题。

- [x] **步骤 3：确认远程主干交付路径**

本任务只通过中文 commit、中文 PR、远端检查通过后合并到远程 `main` 的路径交付；最终合并提交与功能分支删除事实以对应 PR 记录为证。
