# 6 大剧本 E2E Fixture 数据
#
# 每个剧本对应一组 fixture JSON，用于 E2E 测试的一键加载。
# 加载方式：pwsh scripts/load-e2e-fixtures.ps1
#
# Fixture 文件映射：
#   S1 AMI/STEMI 路径推荐与入径 → sample_patient_context_ami.json, sample_ami_pathway.json, sample_ami_rules.json
#   S2 病历内涵质控           → sample_emr_qc_case.json
#   S3 医保智能审核           → sample_insurance_qc_case.json
#   S4 医嘱安全实时拦截       → sample_order_safety_case.json
#   S5 配置包跨环境发布       → sample_config_package.json
#   S6 院级质控驾驶舱         → sample_org_context.json, sample_org_units.json

$s1 = @(
  "sample_patient_context_ami.json",
  "sample_ami_pathway.json",
  "sample_ami_rules.json"
)

$s2 = @(
  "sample_emr_qc_case.json"
)

$s3 = @(
  "sample_insurance_qc_case.json"
)

$s4 = @(
  "sample_order_safety_case.json"
)

$s5 = @(
  "sample_config_package.json"
)

$s6 = @(
  "sample_org_context.json",
  "sample_org_units.json"
)

$allScenarios = @{
  "S1" = $s1
  "S2" = $s2
  "S3" = $s3
  "S4" = $s4
  "S5" = $s5
  "S6" = $s6
}

# 输出映射关系
foreach ($scenario in $allScenarios.GetEnumerator()) {
  Write-Output "$($scenario.Key): $($scenario.Value -join ', ')"
}
