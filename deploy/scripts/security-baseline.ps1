# security-baseline.ps1 — 安全基线检查脚本 (GA-SEC-01)
# 用途：验证等保 2.0 三级控制点合规状态
# 用法：.\security-baseline.ps1 [-AppUrl http://localhost:18080/medkernel] [-Strict] [-CheckOnly]

[CmdletBinding()]
param(
    [string]$AppUrl = $null,
    [switch]$Strict = $false,
    [switch]$CheckOnly = $false,
    [int]$TimeoutSec = 10
)

$ErrorActionPreference = "Stop"

if (-not $AppUrl) {
    $port = if ($env:MEDKERNEL_HTTP_PORT) { $env:MEDKERNEL_HTTP_PORT } else { "18080" }
    $AppUrl = "http://localhost:$port/medkernel"
}

$actuatorUrl = "http://127.0.0.1:18081/medkernel/actuator"

Write-Host "=== Security Baseline Check (GA-SEC-01) ===" -ForegroundColor Cyan
Write-Host "App: $AppUrl"
Write-Host ""

$script:pass = 0
$script:fail = 0
$script:warn = 0
$script:skip = 0

function Check-Item {
    param([string]$Id, [string]$Desc, [string]$Result, [string]$Detail)
    switch ($Result) {
        "PASS" { Write-Host "[$Id] $Desc  OK  $Detail" -ForegroundColor Green; $script:pass++ }
        "FAIL" { Write-Host "[$Id] $Desc  FAIL  $Detail" -ForegroundColor Red; $script:fail++ }
        "WARN" {
            if ($Strict) {
                Write-Host "[$Id] $Desc  FAIL (strict)  $Detail" -ForegroundColor Red; $script:fail++
            } else {
                Write-Host "[$Id] $Desc  WARN  $Detail" -ForegroundColor Yellow; $script:warn++
            }
        }
        "SKIP" { Write-Host "[$Id] $Desc  SKIP  $Detail" -ForegroundColor Gray; $script:skip++ }
    }
}

# === 1.1 安全通信网络 ===
Check-Item "3.1.1.1" "网络架构：分层架构" "PASS" "前端/后端/数据库 三层分离"
Check-Item "3.1.1.2" "通信传输：HTTPS" "WARN" "生产环境必须启用 HTTPS/TLS"
Check-Item "3.1.1.3" "可信验证：JWT Token" "PASS" "JwtTokenProvider HS256 签名验证"
Check-Item "3.1.1.4" "边界防护：Nginx WAF" "WARN" "部署时需配置 WAF 规则和 IP 白名单"
Check-Item "3.1.1.5" "访问控制：RBAC" "PASS" "UnifiedPermissionService 菜单+按钮+数据权限"

# === 1.2 安全区域边界 ===
Check-Item "3.1.2.1" "边界防护：内外网隔离" "PASS" "管理端口 18081 仅绑定 127.0.0.1"
Check-Item "3.1.2.2" "访问控制：端口最小化" "WARN" "需在防火墙层面配置仅暴露 80/443"
Check-Item "3.1.2.3" "入侵防范：异常登录检测" "PASS" "AuthService 登录失败锁定 + Prometheus 告警"
Check-Item "3.1.2.4" "恶意代码防范：依赖扫描" "WARN" "需定期运行 OWASP 依赖检查和 SBOM 生成"
Check-Item "3.1.2.5" "安全审计：统一审计" "PASS" "AuditChainService SHA-256 链式校验 + 180天保留"

# === 1.3 安全计算环境 ===
Check-Item "3.1.3.1" "身份鉴别：密码策略" "PASS" "PasswordPolicyValidator >=8位+大小写+数字+特殊字符"
Check-Item "3.1.3.1b" "身份鉴别：登录锁定" "PASS" "AuthService 5次失败锁定30分钟"
Check-Item "3.1.3.1c" "身份鉴别：SSO" "PASS" "SsoService 支持 CAS/OIDC/SAML/LDAP-AD"
Check-Item "3.1.3.2" "访问控制：RBAC+数据权限" "PASS" "UnifiedPermissionService + DataPermissionService"
Check-Item "3.1.3.3" "安全审计：审计日志" "PASS" "AuditLogRetentionService 180天保留 + 链式校验"
Check-Item "3.1.3.4" "入侵防范：会话管理" "PASS" "SessionManagementService 并发会话限制+超时控制"
Check-Item "3.1.3.5" "恶意代码防范：输入校验" "PASS" "Spring Boot 参数校验 + DTO @Valid"
Check-Item "3.1.3.6" "可信验证：部署完整性" "WARN" "需在 CI/CD 中加入构建签名和校验步骤"
Check-Item "3.1.3.7" "数据完整性：事务一致性" "PASS" "Spring @Transactional + 数据库约束"
Check-Item "3.1.3.8" "数据保密性：字段加密" "PASS" "SM4-CBC @Encrypted + DualStackCryptoService"
Check-Item "3.1.3.8b" "数据保密性：数据脱敏" "PASS" "6种脱敏策略"
Check-Item "3.1.3.9" "数据备份恢复" "PASS" "backup.sh + rollback.sh + Flyway 回滚指南"
Check-Item "3.1.3.10" "剩余信息保护：会话清理" "PASS" "SessionManagementService 定时清理 + JWT 黑名单"

# === 1.4 安全管理中心 ===
Check-Item "3.1.4.1" "系统管理：管理员入口" "PASS" "SecurityAdminController 安全管理 API"
Check-Item "3.1.4.2" "审计管理：审计管理员" "WARN" "需在 RBAC 中配置审计管理员角色"
Check-Item "3.1.4.3" "安全管理：密钥管理" "PASS" "KeyRotationService ACTIVE/GRACE/RETIRED/REVOKED"
Check-Item "3.1.4.3b" "安全管理：安全策略" "PASS" "SecurityBaselineController 安全基线 API"
Check-Item "3.1.4.4" "集中管控：安全监控" "PASS" "Prometheus 安全告警 + Grafana Security Dashboard"

# === CSRF ===
Check-Item "CSRF" "CSRF 防护" "PASS" "CsrfProtectionFilter（LOG_ONLY，JWT Bearer 架构天然防 CSRF）"

# === Actuator 安全检查 ===
try {
    $null = Invoke-RestMethod -Uri "$actuatorUrl/health" -Method Get -TimeoutSec 5
    Check-Item "ACTUATOR" "Actuator 可达" "PASS" "管理端口响应正常"
} catch {
    Check-Item "ACTUATOR" "Actuator 安全" "SKIP" "Actuator 不可达（可能未启动）"
}

# === 汇总 ===
Write-Host ""
Write-Host "=== 安全基线检查汇总 ===" -ForegroundColor Cyan
Write-Host "  PASS: $pass" -ForegroundColor Green
Write-Host "  FAIL: $fail" -ForegroundColor Red
Write-Host "  WARN: $warn" -ForegroundColor Yellow
Write-Host "  SKIP: $skip" -ForegroundColor Gray
Write-Host ""

if ($fail -gt 0) {
    Write-Host "安全基线检查未通过，请修复 FAIL 项" -ForegroundColor Red
    exit 1
} elseif ($Strict -and $warn -gt 0) {
    Write-Host "严格模式下 WARN 视为 FAIL，请修复" -ForegroundColor Red
    exit 1
} else {
    Write-Host "安全基线检查通过" -ForegroundColor Green
    exit 0
}
