# 比較レイアウトの基礎

## レイアウト定義ファイル

| 項目 | 説明 |
|:-----|:-----|
| 配置ディレクトリ | `config/compare_layout/` (または `-layout` オプションで指定したディレクトリ) |
| ファイル名 | 任意 (ディレクトリ直下の全ファイルが読み込まれる) |
| ファイル形式 | JSON |
| 文字コード | UTF-8 |
| 改行コード | LF |

ルート構造は `layoutList` 配列。1 ファイルに複数のレイアウトを定義できる:

```json
{
  "layoutList": [
    { "logicalFileName": "...", "fileRegexPattern": "...", "fileFormat": "...", ... }
  ]
}
```

## レイアウト共通フィールド

| 項目 | 必須 | 説明 |
|:-----|:----:|:-----|
| logicalFileName | 推奨 | 論理ファイル名。結果サマリ (CompareSummary.csv) の Layout 列に出力される |
| fileRegexPattern | ○ | 物理ファイル名の正規表現。**ファイル名 (パスではない) に全体一致**したレイアウトが適用される |
| fileFormat | ○ | `CSV_withHeader` / `CSV_noHeader` / `TSV_withHeader` / `TSV_noHeader` / `Json` / `JsonList` / `Yaml` / `XML` / `Fixed` / `Text` / `Image` |
| charset | テキスト形式で推奨 | 入力ファイルの文字コード (`utf8` / `ms932` / `sjis` / `euc-jp` 等)。未設定時は設定の defaultInputCharset、なければ UTF-8 |
| lineSp | 形式による | 改行コード `CR` / `LF` / `CRLF` / `None`。主に Fixed で使用 (詳細は固定長のリファレンス) |
| recordList | テキスト形式で必須 (Text / XML / pathValueMode を除く) | レコード定義の配列 |
| pathValueMode | 任意 | `Json` / `JsonList` / `Yaml` を jsonPath ベースの path・value 2 項目で比較する (詳細は JSON / YAML / XML のリファレンス) |
| ignoreAreaList | Image のみ | 比較除外エリア (詳細は画像のリファレンス) |

## fileRegexPattern のマッチング仕様

ここを誤るとレイアウトが適用されず、意図せず Text (行全体比較) や画像比較になる。

- **全体一致** (Java の `Pattern.matches` 相当)。部分一致ではない。
  `csv_with-header.*\.csv` は `csv_with-header_20170101.csv` にマッチするが、
  `prefix_csv_with-header.csv` にはマッチしない
- マッチ対象は**ファイル名のみ** (ディレクトリパスは含まない)。比較は左ファイル名で判定される
- 正規表現は Java 互換構文 (先読み・後方参照も使用可)
- 複数のレイアウトがマッチしうる場合、**正規表現文字列の辞書順で先にあるものが勝つ**
  (登録順ではない)。汎用パターン (`.*\.csv`) と特化パターン (`sales_.*\.csv`) を併用する場合、
  `.` は多くの文字より辞書順で小さいため汎用側が先にマッチしがち。特化パターンを使いたい場合は
  汎用パターン側の正規表現を「特化パターンにマッチしない」形にする
- 同一の fileRegexPattern を複数回定義した場合は**後勝ち**で上書きされる
  (config のレイアウトをテスト用定義で上書きする用途に使える)
- **バイナリ同梱のサンプルレイアウトが常に登録済み**である点に注意:
  `csv_with-header.*\.csv` / `fixed_multi-record-type.*\.txt` / `jsonlist_.*\.json` /
  `png.*\.png` など。対象ファイル名がこれらにマッチする場合、自作の特化パターン
  (例: `csv_with-header_ok\.csv`) は辞書順で負けて適用されないことがある。
  その場合はサンプルと**同一の fileRegexPattern を定義して後勝ちで上書き**するのが確実
- JSON 内ではバックスラッシュをエスケープする: `\d` → `"\\d"`

## レイアウトの読み込み順序 (後勝ちマージ)

1. バイナリ同梱のデフォルト (サンプル定義)
2. `COMPAREFILES_CLASSPATH` の各ディレクトリ直下の `compare_layout/`
3. カレントディレクトリの `compare_layout/`
4. `./config/compare_layout/`
5. `-layout, --overwriteLayoutDir` で指定したディレクトリ (カンマ区切りで複数可、記載順)

## レイアウトが適用されない場合の挙動

fileRegexPattern がどのレイアウトにもマッチしないファイルは:

- 拡張子が画像 (png/jpg/jpeg/gif/bmp) → デフォルトの画像比較
- それ以外 → Text 扱い (行全体を 1 項目として文字列比較、ソートなし)

## レイアウトの検証

```bash
compare_files --lintLayout <layout.json>
```

必須項目・enum 値・正規表現・Fixed のバイト長整合などを検証する。
エラーなし: exit 0 / エラーあり: exit 6。警告 (WARN) は実行可能だが意図の確認を推奨。

## レイアウト作成の補助 (Google Sheets)

スプレッドシートからレイアウトを生成する [generator](https://docs.google.com/spreadsheets/d/1pZ7Ta75L5zM8JshsTG9axp0QXs3WG6Xxy1N81JhF11M/copy) も利用できる。
`__tmpl__` シートをコピーして記載し、メニュー「【compare-files】→ generate layout」で
`__generated__` シートに JSON が生成される。
