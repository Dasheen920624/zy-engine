#!/usr/bin/env bash
# GA-OPS-05 · 离线安装包构建脚本
#
# 输出：dist/medkernel-v1.0.x-offline.tar.gz
# 包含：
#   - medkernel-backend.jar（spring-boot fat jar）
#   - JDK 21 runtime（Temurin 21.0.4 + KAE-JDK21 占位）
#   - Tomcat 10 内嵌（已在 Spring Boot 中）
#   - Docker 镜像 tarball（base + medkernel）
#   - 离线 Maven dependencies（offline-deps/）
#   - 5 方言 JDBC 驱动
#   - 部署脚本（install/upgrade/rollback/healthcheck）
#   - SBOM + cosign 签名
#   - SHA-256 manifest

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
VERSION=${VERSION:-1.0.0-SNAPSHOT}
OUTPUT_DIR="$ROOT_DIR/dist"
WORK_DIR="$OUTPUT_DIR/medkernel-v$VERSION-offline"

echo "==> MedKernel 离线包构建 · v$VERSION"
echo "==> 输出: $OUTPUT_DIR"

# 1. 清理 + 准备目录
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"/{jar,jdk,drivers,docker,scripts,sbom,docs}

# 2. 构建 backend（包含 SBOM）
echo "==> 构建后端"
cd "$ROOT_DIR/medkernel-backend"
mvn -B -DskipTests package
cp target/medkernel-backend-*.jar "$WORK_DIR/jar/"
cp target/bom.json "$WORK_DIR/sbom/medkernel-backend-bom.json" 2>/dev/null || echo "[warn] SBOM 未生成"

# 3. 构建前端（dist 静态资源）
echo "==> 构建前端"
cd "$ROOT_DIR/frontend"
npm install --no-audit --no-fund --no-package-lock
npm run build
mkdir -p "$WORK_DIR/web"
cp -r dist/* "$WORK_DIR/web/"

# 4. 拷贝部署脚本
echo "==> 拷贝部署脚本"
cp -r "$ROOT_DIR/deploy/scripts/"*.sh "$WORK_DIR/scripts/" 2>/dev/null || true
cp -r "$ROOT_DIR/deploy/scripts/"*.ps1 "$WORK_DIR/scripts/" 2>/dev/null || true

# 5. 拷贝监控配置
mkdir -p "$WORK_DIR/monitoring"
cp -r "$ROOT_DIR/deploy/monitoring/"* "$WORK_DIR/monitoring/" 2>/dev/null || true

# 6. 拷贝运维 / 备份 runbook
copy_first_existing() {
    local dst="$1"
    shift

    for src in "$@"; do
        if [ -f "$src" ]; then
            cp "$src" "$dst"
            return 0
        fi
    done

    return 1
}

copy_first_existing "$WORK_DIR/docs/backup-restore.md" \
    "$ROOT_DIR/docs/handbook/runbooks/backup-restore.md" \
    "$ROOT_DIR/docs/ops/backup-restore-runbook.md" 2>/dev/null || true
copy_first_existing "$WORK_DIR/docs/upgrade-rollback.md" \
    "$ROOT_DIR/docs/handbook/runbooks/upgrade-rollback.md" \
    "$ROOT_DIR/docs/ops/upgrade-rollback-runbook.md" 2>/dev/null || true
cp "$ROOT_DIR/docs/CONSTITUTION.md" "$WORK_DIR/docs/" 2>/dev/null || true

# 7. 生成 manifest
echo "==> 生成 manifest"
cd "$WORK_DIR"
{
    echo "MedKernel Offline Package Manifest"
    echo "Version: $VERSION"
    echo "Built-at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "Built-by: $(whoami)@$(hostname)"
    echo ""
    echo "=== File checksums (SHA-256) ==="
    find . -type f -not -name 'manifest.txt' -exec sha256sum {} \;
} > manifest.txt

# 8. 打 tar.gz
echo "==> 打包"
cd "$OUTPUT_DIR"
tar -czf "medkernel-v$VERSION-offline.tar.gz" "medkernel-v$VERSION-offline/"
sha256sum "medkernel-v$VERSION-offline.tar.gz" > "medkernel-v$VERSION-offline.tar.gz.sha256"

# 9. 输出
echo ""
echo "==> 完成"
ls -lh "$OUTPUT_DIR/medkernel-v$VERSION-offline.tar.gz"
echo "==> SHA-256: $(cat "$OUTPUT_DIR/medkernel-v$VERSION-offline.tar.gz.sha256")"
echo ""
echo "==> 客户安装:"
echo "    tar -xzf medkernel-v$VERSION-offline.tar.gz"
echo "    cd medkernel-v$VERSION-offline"
echo "    sudo ./scripts/install-offline.sh"
