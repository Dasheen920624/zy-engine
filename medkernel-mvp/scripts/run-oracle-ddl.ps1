param(
  [string]$Connect = $env:MEDKERNEL_DB_CONNECT,
  [string]$Username = $env:MEDKERNEL_DB_USERNAME,
  [string]$Password = $env:MEDKERNEL_DB_PASSWORD
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$env:NLS_LANG = "AMERICAN_AMERICA.AL32UTF8"

. (Join-Path $PSScriptRoot "oracle-env.ps1")
Import-MedKernelOracleLocalEnv -ScriptRoot $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($Connect)) {
  $Connect = $env:MEDKERNEL_DB_CONNECT
}
if ([string]::IsNullOrWhiteSpace($Username)) {
  $Username = $env:MEDKERNEL_DB_USERNAME
}
if ([string]::IsNullOrWhiteSpace($Password)) {
  $Password = $env:MEDKERNEL_DB_PASSWORD
}
if ([string]::IsNullOrWhiteSpace($Connect)) {
  $Connect = "//192.168.4.25:1521/ORCL"
}
if ([string]::IsNullOrWhiteSpace($Username)) {
  $Username = "ZYENGINE"
}
if ([string]::IsNullOrWhiteSpace($Password)) {
  throw "Please set MEDKERNEL_DB_PASSWORD before running this script."
}

$sqlplus = (Get-Command sqlplus -ErrorAction Stop).Source
$root = Split-Path -Parent $PSScriptRoot
$ddl = Join-Path $root "db\oracle\medkernel_core_ddl_with_comments.sql"
$orgMigration = Join-Path $root "db\oracle\medkernel_org_context_migration.sql"
$comments = Join-Path $root "db\oracle\medkernel_comments_unistr.sql"

if (!(Test-Path -LiteralPath $ddl)) {
  throw "DDL file not found: $ddl"
}
if (!(Test-Path -LiteralPath $orgMigration)) {
  throw "Migration file not found: $orgMigration"
}
if (!(Test-Path -LiteralPath $comments)) {
  throw "Comment file not found: $comments"
}

$sql = @"
CONNECT $Username/$Password@$Connect
WHENEVER SQLERROR EXIT SQL.SQLCODE
@$ddl
@$orgMigration
@$comments
SELECT table_name, comments
  FROM user_tab_comments
 WHERE table_name IN (
   'PE_PATHWAY_DEF',
   'PE_PATHWAY_VERSION',
   'PE_PATIENT_INSTANCE',
   'PE_PATIENT_NODE_STATE',
   'PE_PATIENT_TASK_STATE',
   'PE_VARIATION_RECORD',
   'PE_RECOMMENDATION_RECORD',
   'RE_RULE_DEF',
   'RE_RULE_EXEC_LOG',
   'TM_STANDARD_CONCEPT',
   'TM_CONCEPT_MAPPING',
   'ADP_ADAPTER_DEF',
   'ADP_QUERY_DEF',
   'GE_GRAPH_VERSION',
   'ENGINE_AUDIT_LOG'
 )
 ORDER BY table_name;
EXIT
"@

$sql | & $sqlplus -S /nolog
