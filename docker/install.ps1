# ===================================================================================================
#
# Compare Files Docker Environment Installer (Windows PowerShell)
#
#   実行ディレクトリに以下を展開します:
#     compose.yaml / bin (ラッパー) / config (設定・比較レイアウト) /
#     sample (全形式のサンプル) / .claude/skills/compare-layout (レイアウト生成スキル)
#
# ===================================================================================================

$ErrorActionPreference = "Stop"

$Repo = "scenario-test-framework/compare-files"
$Branch = if ($env:COMPAREFILES_INSTALL_BRANCH) { $env:COMPAREFILES_INSTALL_BRANCH } else { "master" }
$ZipUrl = "https://github.com/$Repo/archive/refs/heads/$Branch.zip"

Write-Host "=== Compare Files Docker Environment Setup ==="

# リポジトリのスナップショットを一時ディレクトリに取得
Write-Host "Downloading $Repo@$Branch ..."
$TmpDir = Join-Path ([System.IO.Path]::GetTempPath()) ("compare-files-install-" + [System.Guid]::NewGuid())
New-Item -ItemType Directory -Path $TmpDir | Out-Null
try {
    $ZipPath = Join-Path $TmpDir "repo.zip"
    Invoke-WebRequest -Uri $ZipUrl -OutFile $ZipPath -UseBasicParsing
    Expand-Archive -Path $ZipPath -DestinationPath $TmpDir
    $Src = Get-ChildItem -Path $TmpDir -Directory | Where-Object { $_.Name -like "compare-files-*" } | Select-Object -First 1

    # compose.yaml
    Write-Host "Installing compose.yaml ..."
    Copy-Item (Join-Path $Src "compose.yaml") "compose.yaml" -Force

    # ラッパースクリプト
    Write-Host "Installing wrapper scripts ..."
    New-Item -ItemType Directory -Path "bin" -Force | Out-Null
    foreach ($f in @("compare_files.sh", "compare_regex.sh", "compare_files.cmd", "compare_regex.cmd")) {
        Copy-Item (Join-Path $Src "docker/$f") (Join-Path "bin" $f) -Force
    }

    # 設定ファイルと比較レイアウト (既存の設定は上書きしない)
    Write-Host "Installing configuration ..."
    New-Item -ItemType Directory -Path "config/compare_layout" -Force | Out-Null
    if (-not (Test-Path "config/compare_files.json")) {
        Copy-Item (Join-Path $Src "dist/config/compare_files.json") "config/compare_files.json"
    }
    Copy-Item (Join-Path $Src "dist/config/compare_layout/*") "config/compare_layout/" -Recurse -Force

    # サンプルファイル
    Write-Host "Installing samples ..."
    New-Item -ItemType Directory -Path "sample" -Force | Out-Null
    Copy-Item (Join-Path $Src "dist/sample/*") "sample/" -Recurse -Force

    # 比較レイアウト生成スキル (Claude Code 用)
    Write-Host "Installing compare-layout skill for Claude Code ..."
    New-Item -ItemType Directory -Path ".claude/skills" -Force | Out-Null
    if (Test-Path ".claude/skills/compare-layout") { Remove-Item ".claude/skills/compare-layout" -Recurse -Force }
    Copy-Item (Join-Path $Src ".claude/skills/compare-layout") ".claude/skills/compare-layout" -Recurse
    if (Test-Path ".claude/skills/compare-layout/evals") { Remove-Item ".claude/skills/compare-layout/evals" -Recurse -Force }

    # 利用者向けドキュメント
    New-Item -ItemType Directory -Path "docs" -Force | Out-Null
    Copy-Item (Join-Path $Src "docs/compare_layout.md") "docs/compare_layout.md" -Force
}
finally {
    Remove-Item $TmpDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Setup Complete! ==="
Write-Host ""
Write-Host "Files created:"
Write-Host "  compose.yaml                  - Docker Compose configuration"
Write-Host "  bin/compare_files.(sh|cmd)    - File comparison wrapper"
Write-Host "  bin/compare_regex.(sh|cmd)    - Regex comparison wrapper"
Write-Host "  config/compare_files.json     - Default configuration"
Write-Host "  config/compare_layout/        - Sample compare layouts"
Write-Host "  sample/                       - Sample files (CSV/TSV/Fixed/JSON/JsonList/Image)"
Write-Host "  docs/compare_layout.md        - Compare layout reference"
Write-Host "  .claude/skills/compare-layout - Claude Code skill (比較レイアウト生成)"
Write-Host ""
Write-Host "Try it (処理は container 上で実行されるため、パスは / 区切りです):"
Write-Host "  bin\compare_files.cmd --help"
Write-Host "  bin\compare_files.cmd sample/left/TEXT_PLAINTEXT/plaintext_ok.txt sample/right/TEXT_PLAINTEXT/plaintext_ng.txt"
Write-Host "  bin\compare_files.cmd sample/left/TEXT_CSV/csv_with-header_ng.csv sample/right/TEXT_CSV/csv_with-header_ng.csv"
Write-Host "  bin\compare_files.cmd sample/left sample/right"
Write-Host "  bin\compare_regex.cmd sample/compare_target.csv"
