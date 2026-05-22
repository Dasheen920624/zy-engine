#!/usr/bin/env bash
# DDL 一致性冒烟测试脚本
# 验证 Oracle / DM / PostgreSQL / KingbaseES 四方言的表结构存在性
#
# 用法：
#   ./smoke-ddl-consistency.sh --dialect oracle  [--connect ...] [--user ...] [--password ...]
#   ./smoke-ddl-consistency.sh --dialect dm      [--connect ...] [--user ...] [--password ...]
#   ./smoke-ddl-consistency.sh --dialect postgres [--host ...] [--port ...] [--db ...] [--user ...] [--password ...]
#   ./smoke-ddl-consistency.sh --dialect kingbase [--host ...] [--port ...] [--db ...] [--user ...] [--password ...]
#
# 环境变量（备选）：
#   MEDKERNEL_DB_HOST / MEDKERNEL_DB_PORT / MEDKERNEL_DB_NAME
#   MEDKERNEL_DB_USERNAME / MEDKERNEL_DB_PASSWORD / MEDKERNEL_DB_CONNECT

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/lib/common.sh"

# ---------------------------------------------------------------------------
# 25 张核心业务表
# ---------------------------------------------------------------------------
CORE_TABLES=(
  org_unit
  pe_pathway_def
  pe_pathway_version
  pe_patient_instance
  pe_patient_node_state
  pe_patient_task_state
  pe_variation_record
  pe_recommendation_record
  re_rule_def
  re_rule_exec_log
  tm_standard_concept
  tm_concept_mapping
  adp_adapter_def
  adp_query_def
  ge_graph_version
  engine_audit_log
  src_document
  src_citation
  src_asset_binding
  src_review_record
  src_runtime_evidence
  cfg_config_package
  tm_unmapped_queue
  src_dify_template
  md_patient
)

# ---------------------------------------------------------------------------
# 参数解析
# ---------------------------------------------------------------------------
DIALECT=""
DB_HOST="${MEDKERNEL_DB_HOST:-}"
DB_PORT="${MEDKERNEL_DB_PORT:-}"
DB_NAME="${MEDKERNEL_DB_NAME:-medkernel}"
DB_USER="${MEDKERNEL_DB_USERNAME:-}"
DB_PASSWORD="${MEDKERNEL_DB_PASSWORD:-}"
DB_CONNECT="${MEDKERNEL_DB_CONNECT:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dialect)   DIALECT="$2";   shift 2 ;;
    --host)      DB_HOST="$2";   shift 2 ;;
    --port)      DB_PORT="$2";   shift 2 ;;
    --db)        DB_NAME="$2";   shift 2 ;;
    --user)      DB_USER="$2";   shift 2 ;;
    --password)  DB_PASSWORD="$2"; shift 2 ;;
    --connect)   DB_CONNECT="$2"; shift 2 ;;
    -h|--help)
      echo "用法: $0 --dialect oracle|dm|postgres|kingbase [选项]"
      echo "  --dialect   数据库方言 (必填)"
      echo "  --host      数据库主机"
      echo "  --port      数据库端口"
      echo "  --db        数据库名"
      echo "  --user      用户名"
      echo "  --password  密码"
      echo "  --connect   Oracle 连接串 (user/pass@host:port:sid)"
      exit 0 ;;
    *) die "未知参数: $1" ;;
  esac
done

[ -z "$DIALECT" ] && die "必须指定 --dialect oracle|dm|postgres|kingbase"

# ---------------------------------------------------------------------------
# 生成验证 SQL
# ---------------------------------------------------------------------------
generate_smoke_sql() {
  local first=1
  printf "SELECT 'smoke_start' AS tbl, 0 AS cnt FROM DUAL WHERE 1=0\n"
  for tbl in "${CORE_TABLES[@]}"; do
    if [ "$first" -eq 1 ]; then
      printf "SELECT '%s' AS tbl, COUNT(*) AS cnt FROM %s WHERE 1=0\n" "$tbl" "$tbl"
      first=0
    else
      printf "UNION ALL SELECT '%s', COUNT(*) FROM %s WHERE 1=0\n" "$tbl" "$tbl"
    fi
  done
  printf ";\n"
}

# PG/KingbaseES 不支持 DUAL，去掉第一行
generate_smoke_sql_pg() {
  local first=1
  for tbl in "${CORE_TABLES[@]}"; do
    if [ "$first" -eq 1 ]; then
      printf "SELECT '%s' AS tbl, COUNT(*) AS cnt FROM %s WHERE 1=0\n" "$tbl" "$tbl"
      first=0
    else
      printf "UNION ALL SELECT '%s', COUNT(*) FROM %s WHERE 1=0\n" "$tbl" "$tbl"
    fi
  done
  printf ";\n"
}

# ---------------------------------------------------------------------------
# 执行验证
# ---------------------------------------------------------------------------
PASS=0
FAIL=0
SKIP=0

check_table_result() {
  local tbl="$1"
  local result="$2"
  if [ "$result" = "0" ]; then
    log_ok "表存在：${tbl}"
    PASS=$((PASS + 1))
  else
    log_err "表缺失或异常：${tbl} (result=${result})"
    FAIL=$((FAIL + 1))
  fi
}

run_oracle_smoke() {
  command -v sqlplus >/dev/null 2>&1 || die "sqlplus 未安装，无法执行 Oracle 冒烟"
  [ -z "$DB_CONNECT" ] && die "Oracle 需指定 --connect (user/pass@host:port:sid) 或设置 MEDKERNEL_DB_CONNECT"

  log_step "Oracle DDL 一致性冒烟"
  local sql
  sql="$(generate_smoke_sql)"
  local output
  output="$(echo "$sql" | sqlplus -s "${DB_CONNECT}" 2>&1)" || true

  for tbl in "${CORE_TABLES[@]}"; do
    if echo "$output" | grep -q "^${tbl}"; then
      check_table_result "$tbl" "0"
    else
      check_table_result "$tbl" "1"
    fi
  done
}

run_dm_smoke() {
  command -v disql >/dev/null 2>&1 || die "disql 未安装，无法执行达梦冒烟"
  [ -z "$DB_HOST" ] && die "达梦需指定 --host"

  log_step "达梦 DM DDL 一致性冒烟"
  local sql
  sql="$(generate_smoke_sql)"
  local conn="${DB_USER}/${DB_PASSWORD}@${DB_HOST}:${DB_PORT:-5236}"
  local output
  output="$(echo "$sql" | disql "$conn" 2>&1)" || true

  for tbl in "${CORE_TABLES[@]}"; do
    if echo "$output" | grep -q "^${tbl}"; then
      check_table_result "$tbl" "0"
    else
      check_table_result "$tbl" "1"
    fi
  done
}

run_pg_smoke() {
  command -v psql >/dev/null 2>&1 || die "psql 未安装，无法执行 PG 冒烟"
  [ -z "$DB_HOST" ] && die "PG 需指定 --host"

  log_step "PostgreSQL DDL 一致性冒烟"
  local sql
  sql="$(generate_smoke_sql_pg)"
  local output
  output="$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "${DB_PORT:-5432}" -U "$DB_USER" -d "$DB_NAME" -c "$sql" -At 2>&1)" || true

  for tbl in "${CORE_TABLES[@]}"; do
    if echo "$output" | grep -q "${tbl}|0"; then
      check_table_result "$tbl" "0"
    else
      check_table_result "$tbl" "1"
    fi
  done
}

run_kingbase_smoke() {
  local client=""
  if command -v ksql >/dev/null 2>&1; then
    client="ksql"
  elif command -v psql >/dev/null 2>&1; then
    client="psql"
  else
    die "ksql/psql 均未安装，无法执行 KingbaseES 冒烟"
  fi
  [ -z "$DB_HOST" ] && die "KingbaseES 需指定 --host"

  log_step "KingbaseES DDL 一致性冒烟"
  local sql
  sql="$(generate_smoke_sql_pg)"
  local output
  output="$(PGPASSWORD="$DB_PASSWORD" $client -h "$DB_HOST" -p "${DB_PORT:-54321}" -U "$DB_USER" -d "$DB_NAME" -c "$sql" -At 2>&1)" || true

  for tbl in "${CORE_TABLES[@]}"; do
    if echo "$output" | grep -q "${tbl}|0"; then
      check_table_result "$tbl" "0"
    else
      check_table_result "$tbl" "1"
    fi
  done
}

# ---------------------------------------------------------------------------
# 主入口
# ---------------------------------------------------------------------------
case "$DIALECT" in
  oracle)    run_oracle_smoke ;;
  dm)        run_dm_smoke ;;
  postgres)  run_pg_smoke ;;
  kingbase)  run_kingbase_smoke ;;
  *)         die "不支持的方言: $DIALECT (支持: oracle|dm|postgres|kingbase)" ;;
esac

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
echo ""
log_step "冒烟结果汇总"
TOTAL=$((PASS + FAIL + SKIP))
printf "  PASS: %d  FAIL: %d  SKIP: %d  TOTAL: %d\n" "$PASS" "$FAIL" "$SKIP" "$TOTAL"

if [ "$FAIL" -gt 0 ]; then
  log_err "DDL 一致性冒烟失败：${FAIL} 张表缺失"
  exit 1
else
  log_ok "DDL 一致性冒烟通过：${PASS}/${TOTAL} 张表全部存在"
  exit 0
fi
