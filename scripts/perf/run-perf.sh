#!/bin/bash
# MedKernel 性能测试运行器
# 用法: ./run-perf.sh [test-name] [options]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${RESULTS_DIR}"

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_USER="${AUTH_USER:-admin}"
AUTH_PASS="${AUTH_PASS:-admin123}"
TENANT_ID="${TENANT_ID:-default}"

export BASE_URL AUTH_USER AUTH_PASS TENANT_ID

run_test() {
  local test_name=$1
  local test_file="${SCRIPT_DIR}/${test_name}.js"
  local result_file="${RESULTS_DIR}/${test_name}-result.json"

  if [ ! -f "${test_file}" ]; then
    echo "ERROR: 测试文件不存在: ${test_file}"
    return 1
  fi

  echo "=========================================="
  echo "运行测试: ${test_name}"
  echo "时间: $(date)"
  echo "=========================================="

  k6 run \
    --out json="${result_file}" \
    --summary-export="${RESULTS_DIR}/${test_name}-summary.json" \
    "${test_file}" 2>&1 | tee "${RESULTS_DIR}/${test_name}-output.log"

  echo ""
  echo "结果已保存到: ${RESULTS_DIR}/${test_name}-*"
}

case "${1:-all}" in
  cdss)
    run_test "cdss-evaluate"
    ;;
  pathway)
    run_test "pathway-admit"
    ;;
  rule)
    run_test "rule-engine-evaluate"
    ;;
  adapter)
    run_test "adapter-query"
    ;;
  auth)
    run_test "auth-login"
    ;;
  mixed)
    run_test "mixed-workload"
    ;;
  all)
    echo "运行全部性能测试..."
    echo "结果将保存到: ${RESULTS_DIR}"
    echo ""
    run_test "auth-login"
    run_test "cdss-evaluate"
    run_test "pathway-admit"
    run_test "rule-engine-evaluate"
    run_test "adapter-query"
    run_test "mixed-workload"
    echo ""
    echo "=========================================="
    echo "全部测试完成！"
    echo "结果目录: ${RESULTS_DIR}"
    echo "=========================================="
    ;;
  *)
    echo "用法: $0 {all|cdss|pathway|rule|adapter|auth|mixed}"
    echo ""
    echo "环境变量:"
    echo "  BASE_URL   - 目标服务器地址 (默认: http://localhost:8080)"
    echo "  AUTH_USER  - 认证用户名 (默认: admin)"
    echo "  AUTH_PASS  - 认证密码 (默认: admin123)"
    echo "  TENANT_ID  - 租户ID (默认: default)"
    exit 1
    ;;
esac
