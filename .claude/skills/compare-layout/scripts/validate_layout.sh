#!/bin/bash
#
# 比較レイアウト定義の検証 (compare_files --lintLayout のラッパー)
#   1. PATH 上の compare_files
#   2. リポジトリの go run (このスキルがリポジトリ内にある場合)
#   3. 公開 Docker イメージ
# の順で実行手段を探す。exit 0: エラーなし / 6: エラーあり
#
set -u

if [ $# -lt 1 ]; then
  echo "Usage: validate_layout.sh <layout.json>" >&2
  exit 2
fi
layout="$1"
if [ ! -f "$layout" ]; then
  echo "ERROR: ファイルが存在しません: $layout" >&2
  exit 2
fi

if command -v compare_files >/dev/null 2>&1; then
  exec compare_files --lintLayout "$layout"
fi

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/../../../.." && pwd)
if [ -f "$repo_root/go.mod" ] && command -v go >/dev/null 2>&1; then
  layout_abs=$(cd "$(dirname "$layout")" && pwd)/$(basename "$layout")
  cd "$repo_root" && exec go run ./cmd/compare_files --lintLayout "$layout_abs"
fi

if command -v docker >/dev/null 2>&1; then
  layout_dir=$(cd "$(dirname "$layout")" && pwd)
  exec docker run --rm -v "$layout_dir:/lint" \
    ghcr.io/scenario-test-framework/compare-files:latest \
    /app/bin/compare_files --lintLayout "/lint/$(basename "$layout")"
fi

echo "ERROR: compare_files / go / docker のいずれも見つかりません" >&2
exit 2
