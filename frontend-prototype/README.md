# 医疗智能引擎平台 · 内网管理台高保真原型

本目录是 **FE-001 信息架构与高保真原型** 的可视化交付物。

## 用途

- 客户走查、内部评审、UAT 前的设计参考。
- 给后续 FE-002（前端工程脚手架）和 FE-003～FE-010（业务页面）提供视觉与交互基准。
- 给 9 角色（产品经理 / 产品设计师 / 架构师 / 后端 / 前端 / 测试 / 信息科 / 医生 / 院领导）一个"产品能交付什么"的共识图。

> 本目录是 **纯静态 HTML+CSS**，不连接真实后端。正式前端工程在 `../frontend/`（FE-002 落地后）。

## 如何打开

任意浏览器双击打开 `index.html` 即可。不需要 Node、不需要构建。

或在内网启动一个最简单的静态服务器：

```powershell
# Python 3
python -m http.server 8000 -d frontend-prototype
# 然后浏览器访问 http://localhost:8000
```

## 页面清单

| 文件 | 页面 | 服务的核心剧本 |
|---|---|---|
| `index.html` | 原型导航 | — |
| `workbench.html` | 工作台（首屏）| 内部排查 / 实施 / 运维 |
| `demo-validation.html` | 演示与校验工作台 | AMI 推荐 / EMR_QC / INSURANCE_QC / ORDER_SAFETY |
| `config-package-center.html` | 配置包中心 | 信息科：导入 → review → diff → publish → 回滚 |
| `provenance.html` | 来源追溯 | 医学审核 / 质控 / 院领导：文献库 + 资产绑定 + 影响分析 |
| `hospital-dashboard.html` | 院级驾驶舱 | 院领导：7 个核心问题 5 分钟内可答 |

## 设计参考体系

- **Ant Design Pro** — 企业级中后台结构（侧边栏 + 顶栏 + 主内容）
- **IBM Carbon Data Table** — 数据密集型表格密度与工具栏模式
- **NHS App Design System** — 医疗服务可读性与可访问性
- **GOV.UK Design System Validation** — 错误恢复（顶部摘要 + 字段级 + 行号定位）
- **AntV X6** — 路径画布与图谱关系（FE-005/FE-006 用，本原型暂未画）
- **Apache ECharts** — 看板与报表（驾驶舱中以 CSS 占位模拟）
- **Monaco Editor** — JSON/DSL 编辑（演示页中以 textarea 占位模拟）

## 设计原则（已贯彻到原型）

| # | 原则 | 在哪些页面可见 |
|---|---|---|
| 1 | 首屏是工作台，不是营销页 | workbench |
| 2 | 显式组织上下文（顶栏 + 切换） | 所有页面 |
| 3 | 状态颜色 + 图标 + 文字三重编码（非颜色单一） | 所有 `.tag` |
| 4 | AI 生成内容显式标识（AI 徽标） | workbench / 标准化建议 |
| 5 | 危险操作二次确认 + 影响范围 | config-package-center publish 区 |
| 6 | 来源卡片：标题/机构/版本/章节/摘要/审核人/适用组织 | demo-validation / provenance |
| 7 | 缺来源阻断发布 | config-package-center / provenance |
| 8 | dry-run 默认不写正式状态 | demo-validation 右栏 |
| 9 | 命中规则展示：命中条件 / 患者事实 / 建议动作 / 来源 / traceId | demo-validation |
| 10 | 多组织五级下钻（集团→医院→院区→科室→医生） | hospital-dashboard 下钻面包屑 |
| 11 | 阻断类规则需上级授权 + 覆盖原因 | demo-validation（ORDER_SAFETY 剧本设计） |
| 12 | 发布责任链可追溯（起草/审核/批准） | hospital-dashboard 发布台账 |
| 13 | 可访问性 WCAG AA：focus 可见、aria-label、键盘可达 | base.css `:focus-visible` |
| 14 | 错别字 / 空态 / 加载态 / 错误态四态 | base.css `.state-*` |
| 15 | 提醒疲劳防护：高忽略率规则上看板 | workbench / hospital-dashboard |

## 9 角色覆盖

| 角色 | 主要看哪几页 |
|---|---|
| 顶级产品经理 | 全部（核心：index + workbench + demo-validation + hospital-dashboard）|
| 顶级产品设计师 | 全部（关注 CSS token、状态编码、错误恢复、来源卡片）|
| 顶级架构师 | workbench Provider 区 + config-package-center 生命周期 |
| 顶级后端 | demo-validation 字段口径 + config-package-center 接口结构 |
| 顶级前端 | 全部（作为 FE-002 实现基准）|
| 顶级测试 | demo-validation 命中字段、运行摘要、审计区 |
| 信息科 | config-package-center + provenance + hospital-dashboard 适配器稳定性 |
| 顶级临床医生 | demo-validation（AMI / ORDER_SAFETY tab）|
| 院领导 | hospital-dashboard 全部 |

## 与文档的对应

| 文档 | 对应原型 |
|---|---|
| [`../docs/04_页面规格书.md`](../docs/04_页面规格书.md) | 全部页面（18 个目标页面） |
| [`../docs/03_设计系统.md`](../docs/03_设计系统.md) | Design Tokens 与组件 API |
| [`../docs/02_场景剧本图.md`](../docs/02_场景剧本图.md) 6 大剧本 | 5 个原型页面分别对应 S1-S5 |
| `../ai-dev-input/06_samples/sample_emr_qc_case.json` | demo-validation 输入区 |
| `../ai-dev-input/06_samples/sample_insurance_qc_case.json` | demo-validation tab 三 |
| `../ai-dev-input/06_samples/sample_order_safety_case.json` | demo-validation tab 四 |

## 后续工作

- **FE-002**（已登记台账）：把本原型用 React + TS + AntD 实现成正式工程，连接真实后端。
- **FE-003**：演示与校验工作台业务实现（含 4 类 tab 的真实 API 对接）。
- **FE-004**：配置包中心业务实现。
- **FE-007**：质控看板（含本原型 hospital-dashboard 的图表用 ECharts 实现）。
- **FE-008**：来源追溯前端实现。

## 维护约定

- 修改原型属于 `FE-001` 任务的"第 N+1 批"。
- 修改 `base.css` 需同步说明影响哪些页面。
- 添加新页面需更新 `index.html` 导航 + 本 README 页面清单。
- 不在原型中放真实患者数据、真实 API 凭据、真实医院名称。所有数据为占位。
