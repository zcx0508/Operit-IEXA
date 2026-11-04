#!/usr/bin/env bash
# 补齐 terminal 模块的大体积运行时资产（Ubuntu rootfs 镜像，约 61 MB）。
#
# 该文件来自上游子模块 AAswordman/OperitTerminalCore，为避免在源码仓库里
# 携带大二进制，本分支不跟踪它。首次编译前跑一次本脚本即可。
#
# 用法：
#   bash scripts/fetch-terminal-assets.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/terminal/src/main/assets"
FILE="ubuntu-noble-aarch64-pd-v4.18.0.tar.xz"
URL="https://github.com/AAswordman/OperitTerminalCore/raw/main/src/main/assets/$FILE"

mkdir -p "$DEST"

if [ -f "$DEST/$FILE" ]; then
  echo "已存在，跳过：$DEST/$FILE"
  exit 0
fi

echo "下载 $FILE ..."
if command -v curl >/dev/null 2>&1; then
  curl -L --fail --retry 3 -o "$DEST/$FILE" "$URL"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$DEST/$FILE" "$URL"
else
  echo "需要 curl 或 wget" >&2
  exit 1
fi

echo "完成：$DEST/$FILE"
