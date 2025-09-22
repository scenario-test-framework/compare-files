#!/bin/bash
#===================================================================================================
#
# Compare Files Docker Environment Installer
#
#===================================================================================================

set -e

echo "=== Compare Files Docker Environment Setup ==="

# ディレクトリ作成
echo "Creating directories..."
mkdir -p config bin sample/left sample/right

# compose.yamlをダウンロード
echo "Downloading compose.yaml..."
curl -sSL -o compose.yaml https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/compose.yaml

# 設定ファイルをダウンロード
echo "Downloading configuration files..."
curl -sSL -o config/compare_files.json https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/resources/compare_files.json

# ラッパースクリプトをダウンロード
echo "Downloading wrapper scripts..."
curl -sSL -o bin/compare_files.sh https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_files.sh
curl -sSL -o bin/compare_regex.sh https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_regex.sh
curl -sSL -o bin/compare_files.cmd https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_files.cmd
curl -sSL -o bin/compare_regex.cmd https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_regex.cmd
chmod +x bin/compare_files.sh bin/compare_regex.sh

# サンプルファイルをダウンロード
echo "Downloading sample files..."
curl -sSL -o sample/left/plaintext_ok.txt https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/left/TEXT_PLAINTEXT/plaintext_ok.txt
curl -sSL -o sample/right/plaintext_ok.txt https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/right/TEXT_PLAINTEXT/plaintext_ok.txt
curl -sSL -o sample/right/plaintext_ng.txt https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/right/TEXT_PLAINTEXT/plaintext_ng.txt
curl -sSL -o sample/compare_target.csv https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/compare_target.csv

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Files created:"
echo "  compose.yaml              - Docker Compose configuration"
echo "  config/compare_files.json - Default configuration"
echo "  bin/compare_files.sh      - File comparison wrapper (Linux/macOS)"
echo "  bin/compare_files.cmd     - File comparison wrapper (Windows)"
echo "  bin/compare_regex.sh      - Regex comparison wrapper (Linux/macOS)"
echo "  bin/compare_regex.cmd     - Regex comparison wrapper (Windows)"
echo "  sample/                   - Sample files for testing"
echo ""
echo "Usage:"
echo "  ./bin/compare_files.sh --help"
echo "  ./bin/compare_files.sh sample/left/plaintext_ok.txt sample/right/plaintext_ng.txt"
echo "  ./bin/compare_regex.sh sample/compare_target.csv"
echo ""
echo "To test the installation:"
echo "  ./bin/compare_files.sh sample/left/plaintext_ok.txt sample/right/plaintext_ok.txt"