#!/bin/bash
#===================================================================================================
#
# Compare Files Docker Environment Installer
#
#   実行ディレクトリに以下を展開します:
#     compose.yaml / bin (ラッパー) / config (設定・比較レイアウト) /
#     sample (全形式のサンプル) / .claude/skills/compare-layout (レイアウト生成スキル)
#
#===================================================================================================

set -e

REPO="scenario-test-framework/compare-files"
BRANCH="${COMPAREFILES_INSTALL_BRANCH:-master}"
TARBALL_URL="https://github.com/${REPO}/archive/refs/heads/${BRANCH}.tar.gz"

echo "=== Compare Files Docker Environment Setup ==="

# リポジトリのスナップショットを一時ディレクトリに取得
echo "Downloading ${REPO}@${BRANCH} ..."
tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT
curl -sSL "$TARBALL_URL" | tar xz -C "$tmpdir" --strip-components=1

# compose.yaml
echo "Installing compose.yaml ..."
cp "$tmpdir/compose.yaml" ./compose.yaml

# ラッパースクリプト
echo "Installing wrapper scripts ..."
mkdir -p bin
cp "$tmpdir/docker/compare_files.sh" bin/compare_files.sh
cp "$tmpdir/docker/compare_regex.sh" bin/compare_regex.sh
cp "$tmpdir/docker/compare_files.cmd" bin/compare_files.cmd
cp "$tmpdir/docker/compare_regex.cmd" bin/compare_regex.cmd
chmod +x bin/compare_files.sh bin/compare_regex.sh

# 設定ファイルと比較レイアウト (既存の設定は上書きしない)
echo "Installing configuration ..."
mkdir -p config/compare_layout
if [ ! -f config/compare_files.json ]; then
  cp "$tmpdir/dist/config/compare_files.json" config/compare_files.json
fi
cp -R "$tmpdir/dist/config/compare_layout/." config/compare_layout/

# サンプルファイル (テキスト全形式 + 画像)
echo "Installing samples ..."
mkdir -p sample
cp -R "$tmpdir/dist/sample/." sample/

# 比較レイアウト生成スキル (Claude Code 用)
echo "Installing compare-layout skill for Claude Code ..."
mkdir -p .claude/skills
rm -rf .claude/skills/compare-layout
cp -R "$tmpdir/.claude/skills/compare-layout" .claude/skills/compare-layout
rm -rf .claude/skills/compare-layout/evals

# 利用者向けドキュメント
mkdir -p docs
cp "$tmpdir/docs/compare_layout.md" docs/compare_layout.md

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Files created:"
echo "  compose.yaml                  - Docker Compose configuration"
echo "  bin/compare_files.(sh|cmd)    - File comparison wrapper"
echo "  bin/compare_regex.(sh|cmd)    - Regex comparison wrapper"
echo "  config/compare_files.json     - Default configuration"
echo "  config/compare_layout/        - Sample compare layouts"
echo "  sample/                       - Sample files (CSV/TSV/Fixed/JSON/JsonList/Image)"
echo "  docs/compare_layout.md        - Compare layout reference"
echo "  .claude/skills/compare-layout - Claude Code skill (比較レイアウト生成)"
echo ""
echo "Try it:"
echo "  ./bin/compare_files.sh --help"
echo "  # テキスト比較 (プレーンテキスト)"
echo "  ./bin/compare_files.sh sample/left/TEXT_PLAINTEXT/plaintext_ok.txt sample/right/TEXT_PLAINTEXT/plaintext_ng.txt"
echo "  # レイアウトを使った CSV 比較 (項目単位の差分検出)"
echo "  ./bin/compare_files.sh sample/left/TEXT_CSV/csv_with-header_ng.csv sample/right/TEXT_CSV/csv_with-header_ng.csv"
echo "  # 画像比較 (差分を赤枠でマークした PNG が result/ に出力される)"
echo "  ./bin/compare_files.sh sample/left/IMAGE_PNG/png_ng.png sample/right/IMAGE_PNG/png_ng.png"
echo "  # ディレクトリ一括比較 / 正規表現指定比較"
echo "  ./bin/compare_files.sh sample/left sample/right"
echo "  ./bin/compare_regex.sh sample/compare_target.csv"
echo ""
echo "自分のファイル用の比較レイアウトを作るには (Claude Code):"
echo "  このディレクトリで Claude Code を開き「このファイルの比較レイアウトを作って」と依頼"
echo "  (compare-layout スキルが対象ファイルを分析してレイアウトを生成・検証します)"
