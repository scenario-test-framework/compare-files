const translations = {
  ja: {
    title: "compare-files — 差分を、判断できる形に。", description: "テキスト、構造化データ、画像、ディレクトリを一括比較する高速なCLIツール。差分をCSVや画像で明確に可視化します。", locale: "ja_JP",
    skip: "本文へスキップ", navLabel: "メインナビゲーション", navFeatures: "機能", navFormats: "対応形式", navStart: "使い方", navLayout: "レイアウト仕様", eyebrow: "FAST · REPRODUCIBLE · CI-READY",
    heroTitle: "差分を<br><em>判断できる形</em> に", heroLead: "テキストも、構造化データも、画像も。期待値と実績値を一括比較し、「どこが違うか」を人にもCIにもわかる成果物へ変換します。", heroCta: "今すぐ試す <span aria-hidden=\"true\">→</span>", download: "最新版をダウンロード", statsLabel: "プロダクトの特徴", statExit: "CI向け終了コード", statBinary: "Go製・JRE不要", statSpeed: "大規模CSVで最大", heroVisualLabel: "比較結果のイメージ", differences: "DIFFERENCES",
    flowInput: "<strong>INPUT</strong>期待値 / 実績値", flowCompare: "<strong>COMPARE</strong>レイアウト定義で比較", flowOutput: "<strong>OUTPUT</strong>CSV / PNG / 終了コード",
    featureKicker: "ONE TOOL, CLEAR ANSWERS", featureTitle: "比較作業を、<br><em>再現可能なテスト</em>に。", featureIntro: "目視確認や表計算ソフトでの突合を、設定として残せるコマンドライン処理に置き換えます。", textTitle: "行ではなく、項目で差分を見る", textBody: "比較キーで対応する行を特定し、セル単位の差分をCSVに出力。並び順が違うファイルも比較できます。", imageTitle: "画像差分を赤枠で特定", imageBody: "ピクセル差分を左右比較のPNGへ。動的な領域は除外エリアとして指定できます。", batchTitle: "ディレクトリをまとめて比較", batchBody: "サブディレクトリを再帰的に走査。ファイル形式に応じて比較方法を切り替え、サマリーを生成します。", ciTitle: "CI/CDに、そのまま組み込める", ciBody: "成功 0・差分あり 3・エラー 6 の明確な終了コード。ローカルとCIで同じ比較を再現できます。",
    formatsKicker: "STRUCTURED OR VISUAL", formatsTitle: "いつものファイルを、<br><em>ひとつの流儀</em>で比較。", formatsBody: "レイアウト定義により、比較キー・比較条件・無視する項目をファイル形式ごとに管理できます。", layoutLink: "比較レイアウト仕様を見る <span aria-hidden=\"true\">→</span>",
    startKicker: "START IN ONE COMMAND", startTitle: "まずはサンプルで、<br><em>差分を出してみる。</em>", startBody: "Dockerベースの実行環境、設定、全形式のサンプルをまとめてセットアップします。", installLabel: "インストール方法", copy: "コピー", copied: "コピー済み", copyFailed: "コピーできませんでした", readDocs: "READMEで詳しく見る <span aria-hidden=\"true\">→</span>", binaryDownload: "バイナリを取得", reportLabel: "比較レポート例", finalEyebrow: "STOP EYEBALLING DIFFS.", finalTitle: "その比較、<em>自動化できます。</em>", finalCta: "compare-files を試す <span aria-hidden=\"true\">→</span>", footerText: "Apache-2.0 · 再現可能な比較のためのCLI。", footerGitHub: "GitHub", footerIssues: "Issues"
  },
  en: {
    title: "compare-files — Turn differences into decisions.", description: "A fast CLI for batch-comparing text, structured data, images, and directories, with clear CSV and image outputs.", locale: "en_US",
    skip: "Skip to content", navLabel: "Main navigation", navFeatures: "Features", navFormats: "Formats", navStart: "Get started", navLayout: "Layout spec", eyebrow: "FAST · REPRODUCIBLE · CI-READY",
    heroTitle: "Turn differences<br>into <em>decisions.</em>", heroLead: "Compare text, structured data, images, or entire directories in one run. Turn every mismatch into an artifact that both people and CI can understand.", heroCta: "Get started <span aria-hidden=\"true\">→</span>", download: "Download latest", statsLabel: "Product highlights", statExit: "CI-friendly exit codes", statBinary: "Go binary, no JRE", statSpeed: "Faster on large CSVs", heroVisualLabel: "Comparison result preview", differences: "DIFFERENCES",
    flowInput: "<strong>INPUT</strong>Expected / actual", flowCompare: "<strong>COMPARE</strong>Apply layout rules", flowOutput: "<strong>OUTPUT</strong>CSV / PNG / exit code",
    featureKicker: "ONE TOOL, CLEAR ANSWERS", featureTitle: "Make comparison a<br><em>reproducible test.</em>", featureIntro: "Replace visual checks and spreadsheet matching with a command-line workflow you can save, review, and rerun.", textTitle: "See field-level, not just line-level diffs", textBody: "Match rows by comparison keys and export field-level differences to CSV—even when source files use a different order.", imageTitle: "Mark image differences in place", imageBody: "Export a side-by-side PNG with pixel differences boxed. Ignore dynamic regions through layout rules.", batchTitle: "Compare complete directories", batchBody: "Scan subdirectories recursively, choose the right comparator by file type, and generate a batch summary.", ciTitle: "Built to fit directly into CI/CD", ciBody: "Explicit exit codes: 0 for success, 3 for differences, and 6 for errors. Reproduce the same check locally and in CI.",
    formatsKicker: "STRUCTURED OR VISUAL", formatsTitle: "Compare familiar files<br>with <em>one workflow.</em>", formatsBody: "Layout definitions keep comparison keys, conditions, and ignored fields under version control for every format.", layoutLink: "Read the layout specification <span aria-hidden=\"true\">→</span>",
    startKicker: "START IN ONE COMMAND", startTitle: "Run the samples.<br><em>See your first diff.</em>", startBody: "Set up a Docker-based runtime with configuration and samples for every supported format.", installLabel: "Installation method", copy: "Copy", copied: "Copied", copyFailed: "Copy failed", readDocs: "Read the README <span aria-hidden=\"true\">→</span>", binaryDownload: "Get a binary", reportLabel: "Example comparison report", finalEyebrow: "STOP EYEBALLING DIFFS.", finalTitle: "Your file checks can be <em>automated.</em>", finalCta: "Try compare-files <span aria-hidden=\"true\">→</span>", footerText: "Apache-2.0 · Built for reproducible comparison.", footerGitHub: "GitHub", footerIssues: "Issues"
  }
};

const commands = {
  unix: "curl -sSL https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/install.sh | bash",
  windows: "Invoke-Expression (Invoke-WebRequest -Uri \"https://raw.githubusercontent.com/scenario-test-framework/compare-files/refs/heads/master/docker/install.ps1\" -UseBasicParsing).Content"
};

let currentLanguage = "en";

function resolveLanguage() {
  const query = new URLSearchParams(location.search).get("lang");
  if (query === "ja" || query === "en") return query;
  const stored = localStorage.getItem("compare-files-language");
  if (stored === "ja" || stored === "en") return stored;
  const browserLanguage = navigator.languages?.[0] || navigator.language || "en";
  return browserLanguage.toLowerCase().startsWith("ja") ? "ja" : "en";
}

function applyLanguage(language) {
  currentLanguage = language;
  const dictionary = translations[language];
  document.documentElement.lang = language;
  document.querySelectorAll("[data-i18n]").forEach((element) => { element.textContent = dictionary[element.dataset.i18n]; });
  document.querySelectorAll("[data-i18n-html]").forEach((element) => { element.innerHTML = dictionary[element.dataset.i18nHtml]; });
  document.querySelectorAll("[data-i18n-aria]").forEach((element) => { element.setAttribute("aria-label", dictionary[element.dataset.i18nAria]); });
  document.title = dictionary.title;
  document.querySelector('meta[name="description"]').content = dictionary.description;
  document.querySelector('meta[property="og:title"]').content = dictionary.title;
  document.querySelector('meta[property="og:description"]').content = dictionary.description;
  document.querySelector('meta[property="og:locale"]').content = dictionary.locale;
  const toggle = document.querySelector("[data-language-toggle]");
  toggle.textContent = language === "ja" ? "EN" : "JA";
  toggle.setAttribute("aria-label", language === "ja" ? "Switch to English" : "日本語に切り替える");
}

document.querySelector("[data-language-toggle]").addEventListener("click", () => {
  const nextLanguage = currentLanguage === "ja" ? "en" : "ja";
  localStorage.setItem("compare-files-language", nextLanguage);
  applyLanguage(nextLanguage);
});

document.querySelectorAll("[data-command]").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll("[data-command]").forEach((item) => {
      const active = item === tab;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-selected", String(active));
    });
    document.querySelector("[data-command-text]").textContent = commands[tab.dataset.command];
  });
});

document.querySelector("[data-copy]").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  const label = button.querySelector("[data-copy-label]");
  try {
    await navigator.clipboard.writeText(document.querySelector("[data-command-text]").textContent);
    label.textContent = translations[currentLanguage].copied;
  } catch {
    label.textContent = translations[currentLanguage].copyFailed;
  }
  window.setTimeout(() => { label.textContent = translations[currentLanguage].copy; }, 1800);
});

applyLanguage(resolveLanguage());
