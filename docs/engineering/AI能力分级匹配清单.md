# AI 能力分级与任务匹配清单

> 解决"AI 能力差异导致同任务输出不一致"的问题。  
> 强制方式：[`verify-task-prereq.ps1 -Level`](../../scripts/verify-task-prereq.ps1) 接手前自评，等级不匹配阻断 claim。

---

## 1. 三档能力分级

| 等级 | 代表模型 | 能力上限 |
|---|---|---|
| **初级（junior）** | GPT-3.5、Claude Haiku、Gemini Flash、本地 7B 模型 | 单文件、模式明确、有完整样板的任务 |
| **中级（middle）** | GPT-4、Claude Sonnet 3.5、Gemini Pro | 多文件、需要少量推理、有部分样板的任务 |
| **高级（senior）** | GPT-4-turbo、Claude Opus / Sonnet 4+、Gemini 2 Pro | 架构变更、跨模块、性能优化、新设计 |

---

## 2. 任务等级判定标准

任务难度由 V2 实施手册 [PR 卡片](../05_AI实施手册.md) "难度等级"字段定义：

### 2.1 初级任务（junior）

**特征：**
- 只改 1-3 个文件
- 有完整可复制的 [参考实现样板](reference-implementations/)
- 不涉及业务逻辑判断
- 不涉及数据库 / API 契约
- 单测明确

**示例：**
- PR-V2-01 设计 Token 落地（虽然涉及多个 token，但都是 CSS 值替换）
- 文档 typo 修复
- UI 文案中文化
- 单测补齐
- 已有组件的 storybook story 补齐

**初级 AI 接手时必做：**
1. 通读对应参考样板（不许跳过）
2. 严格按样板结构实现
3. 不允许自创新模式
4. 必须跑 verify-pr.ps1 通过

### 2.2 中级任务（middle）

**特征：**
- 改 5-10 个文件
- 有部分样板可复用，但需做业务适配
- 涉及组件 / 页面 / Service 实现
- 不涉及架构调整
- 可能需要写新单测

**示例：**
- PR-V2-02 公共组件库（5 个组件，每个 5 文件）
- PR-V2-05 配置包列表重做 + 发布向导
- PR-V2-06 路径模板列表
- PR-V2-08 字典映射工作台
- PR-V2-11 质控预警列表
- 新增单一 REST Controller

**中级 AI 接手时必做：**
1. 通读对应剧本（[02_场景剧本图.md](../02_场景剧本图.md)）
2. 通读对应页面规格（[04_页面规格书.md](../04_页面规格书.md)）
3. 通读相关 ADR
4. 跑 verify-task-prereq + verify-pr 通过

### 2.3 高级任务（senior）

**特征：**
- 涉及 10+ 文件 / 跨模块
- 需要做架构判断
- 涉及性能 / 安全 / 可用性
- 可能要新增 ADR
- 涉及 DDL 变更 / 数据迁移

**示例：**
- PR-V2-03 路由 + 顶级菜单 + AppLayout
- PR-V2-04 SEC-001 用户体系（含 6 张新表）
- PR-V2-07 路径模板编辑器（X6 画布 + 复杂状态管理）
- PR-V2-09 临床嵌入器框架（独立打包 + WebSocket）
- PR-V2-10 医嘱拦截弹窗（涉及 ADR-0004 关键不变量）
- PR-V2-12 院级驾驶舱（性能 + 数据聚合）
- 任何 REFIT-* 任务（已实现能力改造）
- 任何 SEC-* / OPS-* / COMP-* 任务
- 数据库 schema 变更

**高级 AI 接手时必做：**
1. 通读金本位 5 份全部
2. 检查相关 ADR
3. 涉及新设计先开 ADR
4. 涉及 DDL 必须 4 套同步（Oracle/DM/PG/H2）
5. 必须有 feature_acceptance 记录

---

## 3. 等级降级处理

若高级 AI 短期不可用，中级 AI 想接高级任务：

**❌ 禁止：** 强行领取高级任务

**✅ 允许：** 把高级任务**拆成多个中级子任务**：
1. 高级 AI 先开 ADR（半小时）
2. 高级 AI 写架构骨架（半小时）
3. 中级 AI 按骨架补实现
4. 高级 AI review

---

## 4. PR-V2-01 ~ PR-V2-12 等级映射

| PR | 名称 | 等级 | 推荐能力 |
|---|---|:---:|---|
| PR-V2-01 | 设计 Token 落地 | 初级 | GPT-3.5 / Haiku |
| PR-V2-02 | 公共组件库 v1（C01-C05） | 中级 | GPT-4 / Sonnet 3.5 |
| PR-V2-03 | 路由 + 顶级菜单 + AppLayout | 中级偏高 | GPT-4 / Sonnet |
| PR-V2-04 | SEC-001 用户体系最小可用 | 高级 | Sonnet 4 / Opus |
| PR-V2-05 | 配置包列表重做 + 发布向导 | 高级 | Sonnet 4 |
| PR-V2-06 | 路径模板列表 | 中级 | Sonnet 3.5 |
| PR-V2-07 | 路径模板编辑器 | 高级 | Sonnet 4 / Opus |
| PR-V2-08 | 字典映射工作台 | 中级 | Sonnet 3.5 |
| PR-V2-09 | 临床嵌入器 + EM01 AMI | 高级 | Sonnet 4 / Opus |
| PR-V2-10 | 医嘱拦截弹窗 EM04 | 高级 | Sonnet 4 / Opus |
| PR-V2-11 | 质控预警列表 + 派单 | 中级 | Sonnet 3.5 |
| PR-V2-12 | 院级质控驾驶舱 | 高级 | Sonnet 4 |

---

## 5. 接手前自检（强制）

```powershell
.\scripts\verify-task-prereq.ps1 -TaskId PR-V2-XX -Level <junior|middle|senior>
```

脚本会检查：

- 任务编号在台账存在
- 你的等级 ≥ 任务难度
- 依赖任务全部 DONE
- 工作树干净
- 远端 main 同步
- 无 active claim 冲突
- 文档体系完整

任何一项 FAIL 都不允许创建 active claim。

---

## 6. 完成后自检（强制）

```powershell
.\scripts\verify-pr.ps1 -TaskId PR-V2-XX
```

脚本会检查：

- 工作树有改动
- 不违反 ADR 不变量（硬编码颜色 / ZyEngine 命名 / 引用废弃路径）
- 后端 build + test PASS
- 前端 lint + test + build PASS
- UTF-8 无 BOM
- 独占文件冲突
- DoD 检查表自动抽取
- feature acceptance 记录（高风险 PR 必须）

任何 FAIL 都不允许 commit + push。

---

## 7. AI 自评等级（自我声明）

不同模型自评等级参考：

| 模型 | 自评等级 |
|---|---|
| GPT-3.5-turbo | junior |
| Claude Haiku 3 / 3.5 | junior |
| Gemini 1.5 Flash | junior |
| 本地 7B/13B 模型 | junior |
| GPT-4 | middle |
| GPT-4o | middle |
| Claude Sonnet 3.5 | middle（偏高） |
| Gemini 1.5 Pro | middle |
| GPT-4-turbo | senior（保守） |
| Claude Sonnet 4 / 4.5 / 4.6 / 4.7 | senior |
| Claude Opus 3 / 4 / 4.5 / 4.6 / 4.7 | senior |
| Gemini 2 Pro / Ultra | senior |
| 任何 long context（200K+）+ 推理强模型 | senior |

接手时在 active claim 里**必须**填 "ai_level: junior|middle|senior"，与脚本 `-Level` 参数一致。

---

## 8. 升级路径

初级 AI 想升级到中级？

1. 完成 5 个初级 PR 且全部 GOLD 验收
2. review 通过率 ≥ 90%
3. 单测覆盖率 ≥ 80%
4. 由用户/管理员标记 ai_promoted=true

（中级升高级同理）
