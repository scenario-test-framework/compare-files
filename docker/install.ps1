#===================================================================================================
#
# Compare Files Docker Environment Installer for Windows
#
#===================================================================================================

Write-Host "=== Compare Files Docker Environment Setup ===" -ForegroundColor Green

try {
    # ディレクトリ作成
    Write-Host "Creating directories..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path config, bin, sample/left, sample/right | Out-Null

    # compose.yamlをダウンロード
    Write-Host "Downloading compose.yaml..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/compose.yaml" -OutFile "compose.yaml"

    # 設定ファイルをダウンロード
    Write-Host "Downloading configuration files..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/resources/compare_files.json" -OutFile "config/compare_files.json"

    # ラッパースクリプトをダウンロード
    Write-Host "Downloading wrapper scripts..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_files.sh" -OutFile "bin/compare_files.sh"
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/compare_regex.sh" -OutFile "bin/compare_regex.sh"

    # Windows用バッチファイルも作成
    Write-Host "Creating Windows batch files..." -ForegroundColor Yellow
    @"
@echo off
docker compose run --rm compare-files %*
"@ | Out-File -FilePath "bin/compare_files.cmd" -Encoding ASCII

    @"
@echo off
docker compose run --rm compare-regex %*
"@ | Out-File -FilePath "bin/compare_regex.cmd" -Encoding ASCII

    # サンプルファイルをダウンロード
    Write-Host "Downloading sample files..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/left/TEXT_PLAINTEXT/plaintext_ok.txt" -OutFile "sample/left/plaintext_ok.txt"
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/right/TEXT_PLAINTEXT/plaintext_ok.txt" -OutFile "sample/right/plaintext_ok.txt"
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/right/TEXT_PLAINTEXT/plaintext_ng.txt" -OutFile "sample/right/plaintext_ng.txt"
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/src/main/scripts/sample/compare_target.csv" -OutFile "sample/compare_target.csv"

    Write-Host ""
    Write-Host "=== Setup Complete! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Files created:" -ForegroundColor White
    Write-Host "  compose.yaml                  - Docker Compose configuration" -ForegroundColor Gray
    Write-Host "  config\compare_files.json     - Default configuration" -ForegroundColor Gray
    Write-Host "  bin\compare_files.cmd         - File comparison wrapper (Windows)" -ForegroundColor Gray
    Write-Host "  bin\compare_regex.cmd         - Regex comparison wrapper (Windows)" -ForegroundColor Gray
    Write-Host "  bin\compare_files.sh          - File comparison wrapper (Unix)" -ForegroundColor Gray
    Write-Host "  bin\compare_regex.sh          - Regex comparison wrapper (Unix)" -ForegroundColor Gray
    Write-Host "  sample\                       - Sample files for testing" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Usage (Windows):" -ForegroundColor Cyan
    Write-Host "  bin\compare_files.cmd --help"
    Write-Host "  bin\compare_files.cmd sample/left/plaintext_ok.txt sample/right/plaintext_ng.txt"
    Write-Host "  bin\compare_regex.cmd sample/compare_target.csv"
    Write-Host ""
    Write-Host "Usage (WSL/Unix):" -ForegroundColor Cyan
    Write-Host "  .\bin\compare_files.sh --help"
    Write-Host "  .\bin\compare_files.sh sample/left/plaintext_ok.txt sample/right/plaintext_ng.txt"
    Write-Host "  .\bin\compare_regex.sh sample/compare_target.csv"
    Write-Host ""
    Write-Host "To test the installation:" -ForegroundColor Yellow
    Write-Host "  bin\compare_files.cmd sample/left/plaintext_ok.txt sample/right/plaintext_ok.txt"

} catch {
    Write-Host "Error during setup: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}