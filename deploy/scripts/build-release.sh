#!/usr/bin/env bash
# 构建发布包 —— 在 dev / CI 机器上跑（需有外网或预热的本地仓库）
# 用法：./build-release.sh --version 1.2.3 [--include-frontend] [--include-jdk linux-x86_64,linux-aarch64] --output ./dist

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

VERSION=""
INCLUDE_FRONTEND=0
JDK_TARGETS=""
OUTPUT_DIR="./dist"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --include-frontend) INCLUDE_FRONTEND=1; shift ;;
    --include-jdk) JDK_TARGETS="$2"; shift 2 ;;
    --output) OUTPUT_DIR="$2"; shift 2 ;;
    -h|--help) sed -n '1,5p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

[ -n "$VERSION" ] || die "请指定 --version"

GIT_HASH="$(git -C "$REPO_ROOT" rev-parse --short HEAD)"
BUILD_TIME="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
PKG_NAME="zy-engine-v${VERSION}-${GIT_HASH}"
STAGING="$(mktemp -d)/$PKG_NAME"
mkdir -p "$STAGING"
log_info "Staging：$STAGING"

log_step "1. 构建后端 jar"
( cd "$REPO_ROOT/zy-engine-mvp" && mvn -B -q -DskipTests package )
JAR_FILE="$(ls "$REPO_ROOT/zy-engine-mvp/target/"zy-engine-mvp-*.jar | head -1)"
[ -f "$JAR_FILE" ] || die "未找到 jar"
mkdir -p "$STAGING/lib"
cp "$JAR_FILE" "$STAGING/lib/zy-engine.jar"
JAR_SHA=$(sha256sum "$STAGING/lib/zy-engine.jar" | awk '{print $1}')
log_ok "后端 jar：$JAR_SHA"

log_step "2. 构建前端 dist"
if [ "$INCLUDE_FRONTEND" -eq 1 ]; then
  if [ -d "$REPO_ROOT/frontend" ]; then
    ( cd "$REPO_ROOT/frontend" && npm ci --prefer-offline && npm run build )
    mkdir -p "$STAGING/frontend"
    cp -a "$REPO_ROOT/frontend/dist" "$STAGING/frontend/"
    FE_SHA=$(find "$STAGING/frontend/dist" -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}')
    log_ok "前端 dist：$FE_SHA"
  else
    log_warn "frontend 目录不存在，跳过前端"
    FE_SHA="-"
  fi
else
  log_skip "未指定 --include-frontend，跳过前端"
  FE_SHA="-"
fi

log_step "3. 拷贝 DDL / scripts / docs / profiles"
mkdir -p "$STAGING/db"
cp -a "$REPO_ROOT/zy-engine-mvp/db/." "$STAGING/db/"
mkdir -p "$STAGING/scripts"
cp -a "$SCRIPT_DIR/." "$STAGING/scripts/"
mkdir -p "$STAGING/systemd" "$STAGING/nginx" "$STAGING/profiles"
[ -d "$SCRIPT_DIR/../systemd" ]  && cp -a "$SCRIPT_DIR/../systemd/."  "$STAGING/systemd/"  || true
[ -d "$SCRIPT_DIR/../nginx" ]    && cp -a "$SCRIPT_DIR/../nginx/."    "$STAGING/nginx/"    || true
[ -d "$SCRIPT_DIR/../profiles" ] && cp -a "$SCRIPT_DIR/../profiles/." "$STAGING/profiles/" || true
cp "$REPO_ROOT/CHANGELOG.md" "$STAGING/CHANGELOG.md" 2>/dev/null || true

log_step "4. JDK"
if [ -n "$JDK_TARGETS" ]; then
  mkdir -p "$STAGING/jdk"
  IFS=',' read -ra TARGETS <<< "$JDK_TARGETS"
  for t in "${TARGETS[@]}"; do
    log_warn "JDK 打包占位：$t  → 请按企业内规由 CI 拉取并放置于 $STAGING/jdk/$t/"
  done
else
  log_skip "未指定 --include-jdk，发布包不含 JDK"
fi

log_step "5. 生成 manifest.json"
cat > "$STAGING/manifest.json" <<JSON
{
  "name": "zy-engine",
  "version": "$VERSION",
  "git_hash": "$GIT_HASH",
  "build_time": "$BUILD_TIME",
  "build_host": "$(hostname)",
  "components": {
    "backend":  {"jar": "lib/zy-engine.jar", "sha256": "$JAR_SHA"},
    "frontend": {"dist": "frontend/dist/",   "sha256": "$FE_SHA"}
  },
  "supported_os": [
    "centos7-x86_64",
    "openeuler-22.03-x86_64",
    "openeuler-22.03-aarch64",
    "uos-v20-x86_64",
    "uos-v20-aarch64",
    "kylin-v10-x86_64",
    "kylin-v10-aarch64",
    "windows-server-x86_64"
  ]
}
JSON

log_step "6. 计算 checksums.sha256"
( cd "$STAGING" && find . -type f -not -name "checksums.sha256" -print0 | sort -z | xargs -0 sha256sum > checksums.sha256 )

log_step "7. 打 tar.gz"
mkdir -p "$OUTPUT_DIR"
TAR_FILE="$OUTPUT_DIR/$PKG_NAME.tar.gz"
tar -czvf "$TAR_FILE" -C "$(dirname "$STAGING")" "$(basename "$STAGING")" >/dev/null
TAR_SHA=$(sha256sum "$TAR_FILE" | awk '{print $1}')
echo "$TAR_SHA  $(basename "$TAR_FILE")" > "$TAR_FILE.sha256"

log_ok "完成"
echo ""
echo "产物：$TAR_FILE"
echo "sha256：$TAR_SHA"
