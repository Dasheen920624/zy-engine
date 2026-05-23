# ADR-0004: 医学/医保/质控内容必须有来源（MISSING_SOURCE 阻断发布）

- 状态：Accepted
- 日期：2026-05-18
- 决策者：医学专家 + 合规 + 架构
- 涉及范围：业务规则（全平台）

## 上下文

医院客户对临床决策支持类系统的核心担忧：

- "这条规则的医学依据是什么？" — 主治医师质疑
- "万一拦截错医嘱出医疗事故，平台担责吗？" — 医务处主任
- "JCI/HIMSS 评审会查证据链" — 信息科主任
- "医保超付追责时拿什么证明？" — 医保办

V1 阶段允许"先发后补来源"，结果客户验收时大量规则/路径没有医学依据，无法上线。

## 决策

**所有医学/医保/质控类配置（规则 / 路径节点 / 知识图谱关系 / 字典映射）必须有来源审核记录才能发布。**

具体：

- 每条"医学内容"必须绑定 `source_review_id`
- 来源审核必须有：审核人（platform_user_id）、审核时间、审核结论、引用文档、引用片段
- 发布前 review API 必须返回 `source_review` 状态
- 任何资产 `missing_count + expired_count + unreviewed_count > 0` 或 `allow_publish=false` 时，发布 API 必须返回错误码 `MISSING_SOURCE` 阻断

## 不变量

- I-1：规则/路径/图谱/字典/Dify 模板的实体表必须有 `source_review_id` 字段（可空仅限非医学内容）
- I-2：发布 API 必须执行 `SourceReviewChecker` 检查
- I-3：MISSING_SOURCE 错误**不允许**绕过（如 admin 权限强制发布）
- I-4：前端 `<SourceInfo>` 组件强制展示来源状态
- I-5：任何 ENGINE_AUDIT_LOG 中 publish 操作必须含 `source_review_passed=true`

## 替代方案及拒绝原因

- **来源审核作为告警不阻断** → 拒绝：客户验收时还是会缺
- **只对规则强制来源，路径可选** → 拒绝：路径节点引用的规则有来源不够，路径本身的临床证据也要有
- **批准后允许"应急发布"** → 拒绝：开了口子就关不上，且法律责任不清

## 影响

正面：
- 客户合规审计直接通过
- 医生信任度提升（来源透明）
- 系统责任边界清晰（来源审核人担责）

负面：
- 配置工作量增加（每条规则要补来源）
- 知识工程师角色变得关键（专职管理来源）
- 紧急医学知识发布流程需特殊设计（如绿色通道，但仍需事后审核）

## 强制方式

- **后端**：`ConfigPackagePublishService.publish()` 强制调 `SourceReviewChecker`
- **后端**：所有 `*PublishController` 单元测试必须含 MISSING_SOURCE 阻断测试
- **前端**：`<SourceInfo>` 在 `review.status === 'missing'` 时显示橙红警告底，且 `<PublishButton>` 禁用
- **CI**：契约测试套件 `SourceReviewBlockingTests` 覆盖所有 PublishController
- **客户演示**：剧本 S5（配置包发布）必须演示 MISSING_SOURCE 阻断

## 相关参考

- 产品事实源不变量 #8：[`docs/01_产品事实源.md §7.2`](../../01_产品事实源.md#72-医学安全5-项)
- 场景剧本 S5 帧 4：[`docs/02_场景剧本图.md`](../../02_场景剧本图.md#帧-4--来源审核步骤-3)
- 来源追溯任务：PROV-001 ~ PROV-007
- 相关 PR：PR-V2-05 配置包发布向导
