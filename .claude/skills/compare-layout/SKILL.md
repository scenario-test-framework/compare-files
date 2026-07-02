---
name: compare-layout
description: compare-files の比較レイアウト定義 (JSON) を、比較対象ファイルの実物から生成する。ユーザーが「比較レイアウトを作って」「このファイルを比較したい」「レイアウト定義がわからない」「CSV/TSV/固定長/JSON/YAML/XML/画像の比較設定」「compare_files で比較する準備」などに言及したら、ファイル形式が何であっても必ずこのスキルを使うこと。レイアウトの修正・検証・レビューにも使う。入力は単一ファイル・複数ファイル・ディレクトリのいずれでもよい。
---

# 比較レイアウト生成

compare-files は比較レイアウト定義 (JSON) に従って、テキスト・画像ファイルを項目単位で比較するツール。
このスキルは、比較対象ファイルの実物を分析して、そのファイルに合ったレイアウト定義を生成する。

## ワークフロー

### 1. ファイルを分析する (決定論)

対象パス (ファイル・ディレクトリのどちらでも可、複数可) を分析スクリプトに渡す:

```bash
python3 scripts/analyze_files.py <path> [<path>...]
```

ファイルごとに JSON で返る: 推定フォーマット候補、文字コード候補、改行コード、区切り文字、
カラム数の一貫性、ヘッダー行らしさ、固定長レコード長の分布、JSON キー構造、画像种別。
スクリプトは「事実」だけを返す。フォーマットの最終判断は次のステップで行う。

### 2. フォーマットを判断し、対応するリファレンスを読む

分析結果からフォーマットを判断し、**該当するリファレンスだけ**を読む:

| 判断したフォーマット | 読むファイル |
|:--|:--|
| すべての形式に共通の基礎 (必ず最初に読む) | `references/layout-basics.md` |
| CSV / TSV (ヘッダーあり・なし) | `references/format-csv-tsv.md` |
| 固定長テキスト | `references/format-fixed.md` |
| JSON / JSONList (改行区切り JSON) | `references/format-json.md` |
| YAML | `references/format-yaml.md` |
| XML | `references/format-xml.md` |
| 画像 (png/jpg/jpeg/gif/bmp) | `references/format-image.md` |
| 比較条件 (criteria) の選び方 | `references/criteria.md` |

判断に迷うシグナル:
- 区切り文字が検出されず行のバイト長が全行同一 → 固定長の可能性が高い
- 各行が `{` で始まり独立した JSON としてパースできる → JsonList
- ファイル全体で 1 つの JSON → Json
- 拡張子 .yaml / .yml またはインデントベースのキー: 値 構造 → Yaml
- `<?xml` 宣言またはタグ構造 → XML (itemList 不要。xpath ベースの path・value 比較)
- プロパティの挿入位置を考慮した比較をしたい JSON/YAML → pathValueMode (format-json.md 参照)
- どれにも該当しない → Text (レイアウト不要。レイアウトなしで行全体比較になる)

### 3. 比較キーと比較条件を決める

レイアウトの品質は「どの項目を比較キーにするか」「タイムスタンプ等をどう扱うか」で決まる。
`references/criteria.md` を読み、分析結果のヒント (ユニークなカラム、日時らしい値) を使って決める。

ここは推測で確定させず、判断材料をユーザーに示して確認する:
- 比較キー候補 (値がユニークなカラム) — キーがないと行の対応付けができない
- 実行のたびに変わる項目 (更新時刻・連番など) — Ignore や Datetime_* 条件の候補

### 4. レイアウト JSON を書いて検証する (決定論)

レイアウトを書いたら必ず検証する:

```bash
bash scripts/validate_layout.sh <layout.json>
```

これは本体の `compare_files --lintLayout` を呼ぶ (バイナリがなければ `go run` にフォールバック)。
エラー (exit 6) が出たら修正して再検証。警告も内容を確認してユーザーに伝える。

### 5. 実ファイルで動作確認する

対象ファイルが左右そろっている場合は実際に比較して、レイアウトが適用されることを確認する:

```bash
compare_files -layout <レイアウトを置いたディレクトリ> -od /tmp/layout_check <left> <right>
```

結果サマリの Layout 列に論理名が出ていればマッチ成功 (「-」ならレイアウトが適用されていない
= fileRegexPattern がファイル名に全体一致していない)。左右が用意できない場合は同じファイル同士を
比較すれば「全行 OK」になることを確認できる。

## 出力

- レイアウトはユーザー指定の場所、なければ `config/compare_layout/<対象の説明>.json` に書く
- 生成したレイアウトの各項目について、なぜその compareKey / criteria にしたかを 1 行ずつ説明する
- 検証結果 (OK / 警告) と、動作確認の結果を報告する

## 複数ファイル・ディレクトリの場合

ディレクトリを受け取ったら配下を走査し、ファイル名のパターン (拡張子・命名規則) でグループ化する。
グループごとに代表ファイル 1 つを分析してレイアウトを作り、fileRegexPattern をグループ全体に
マッチする正規表現にする。1 つのレイアウトファイルに複数のレイアウトをまとめてよい
(layoutList は配列)。正規表現は辞書順の先勝ちマッチなので、パターン同士の包含関係に注意する
(詳細は layout-basics.md)。

## ドキュメント生成 (メンテナ向け)

この references/ が比較レイアウト仕様の正本。利用者向けドキュメント docs/compare_layout.md は
以下で再生成する (references を変更したら必ず実行):

```bash
python3 scripts/generate_docs.py
```
