param(
  [string]$BaseUrl = "http://localhost:18082/zy-engine/api"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method Get
if (-not $health.success) {
  throw "Health check failed."
}

# 检查 Provider 状态
$providers = Invoke-RestMethod -Uri "$BaseUrl/system/providers" -Method Get
if (-not $providers.success) {
  throw "Provider status check failed."
}
$databaseMode = $providers.data.database_provider
Write-Host "Database provider: $databaseMode"

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$tenantId = "TENANT_ORG_SMOKE_$stamp"

# 1. 导入组织目录
$importBody = @{
  tenant_id = $tenantId
  operator_id = "ORG_SMOKE_TEST"
  units = @(
    @{
      level = "GROUP"
      code = "GRP_SMOKE"
      name = "SMOKE测试集团"
      status = "ACTIVE"
      display_order = 1
    },
    @{
      level = "HOSPITAL"
      code = "HOSP_SMOKE"
      name = "SMOKE测试医院"
      parent_level = "GROUP"
      parent_code = "GRP_SMOKE"
      status = "ACTIVE"
      display_order = 1
    },
    @{
      level = "CAMPUS"
      code = "CAMPUS_SMOKE"
      name = "SMOKE测试院区"
      parent_level = "HOSPITAL"
      parent_code = "HOSP_SMOKE"
      status = "ACTIVE"
      display_order = 1
    },
    @{
      level = "SITE"
      code = "SITE_SMOKE"
      name = "SMOKE测试卫生所"
      parent_level = "CAMPUS"
      parent_code = "CAMPUS_SMOKE"
      status = "ACTIVE"
      display_order = 1
    },
    @{
      level = "DEPARTMENT"
      code = "DEPT_CARD_SMOKE"
      name = "SMOKE测试心内科"
      parent_level = "SITE"
      parent_code = "SITE_SMOKE"
      status = "ACTIVE"
      display_order = 1
    }
  )
} | ConvertTo-Json -Depth 10

$imported = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Method Post -ContentType "application/json; charset=utf-8" -Body $importBody
if (-not $imported.success) {
  throw "Organization import failed."
}
if ($imported.data.imported_count -ne 5) {
  throw "Expected 5 imported units, got $($imported.data.imported_count)"
}
Write-Host "STEP 1 PASSED: Imported $($imported.data.imported_count) organization units"

# 2. 查询组织列表
$listResp = Invoke-RestMethod -Uri "$BaseUrl/organizations?tenant_id=$tenantId&level=DEPARTMENT" -Method Get
if (-not $listResp.success -or $listResp.data.Count -ne 1) {
  throw "Organization list filter failed. Count: $($listResp.data.Count)"
}
if ($listResp.data[0].code -ne "DEPT_CARD_SMOKE") {
  throw "Expected DEPT_CARD_SMOKE, got $($listResp.data[0].code)"
}
Write-Host "STEP 2 PASSED: List and filter organization units"

# 3. 查询单个组织单元（含子节点）
$getResp = Invoke-RestMethod -Uri "$BaseUrl/organizations/HOSPITAL/HOSP_SMOKE?tenant_id=$tenantId" -Method Get
if (-not $getResp.success) {
  throw "Get single organization unit failed."
}
if ($getResp.data.code -ne "HOSP_SMOKE") {
  throw "Expected HOSP_SMOKE, got $($getResp.data.code)"
}
if ($getResp.data.children.Count -ne 1) {
  throw "Expected 1 child for HOSP_SMOKE, got $($getResp.data.children.Count)"
}
Write-Host "STEP 3 PASSED: Get single organization unit with children"

# 4. 查询组织树
$treeResp = Invoke-RestMethod -Uri "$BaseUrl/organizations/tree?tenant_id=$tenantId" -Method Get
if (-not $treeResp.success) {
  throw "Organization tree query failed."
}
if ($treeResp.data.root_count -lt 1) {
  throw "Expected at least 1 root in tree, got $($treeResp.data.root_count)"
}
Write-Host "STEP 4 PASSED: Organization tree query (root_count=$($treeResp.data.root_count))"

# 5. 重新导入相同数据（UPSERT 验证）
$imported2 = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Method Post -ContentType "application/json; charset=utf-8" -Body $importBody
if (-not $imported2.success) {
  throw "Organization re-import (UPSERT) failed."
}
if ($imported2.data.imported_count -ne 5) {
  throw "Re-import expected 5, got $($imported2.data.imported_count)"
}
$listAll = Invoke-RestMethod -Uri "$BaseUrl/organizations?tenant_id=$tenantId" -Method Get
if ($listAll.data.Count -ne 5) {
  throw "After UPSERT, expected 5 total units, got $($listAll.data.Count)"
}
Write-Host "STEP 5 PASSED: UPSERT verification (no duplicates)"

# 6. 持久化验证：重启后从 DB 加载
# 注意：此步骤需要先停止服务、再启动服务后重新查询
# 本脚本在服务运行中验证 API 层，持久化层的契约测试由 EngineApiContractTests 覆盖
Write-Host "STEP 6 SKIPPED: Restart persistence verification covered by EngineApiContractTests"

Write-Host ""
Write-Host "Organization smoke test PASSED."
Write-Host "Tenant: $tenantId"
Write-Host "Database mode: $databaseMode"
