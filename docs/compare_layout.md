<!--
このファイルは自動生成です。直接編集しないでください。
正本: .claude/skills/compare-layout/references/
再生成: python3 .claude/skills/compare-layout/scripts/generate_docs.py
-->

# 比較レイアウトリファレンス

compare-files の比較レイアウト定義 (JSON) の仕様です。
レイアウトの作成は Claude Code の `compare-layout` スキル
(`.claude/skills/compare-layout/`) で支援できます。

- [比較レイアウトの基礎](#比較レイアウトの基礎)
- [CSV / TSV レイアウト](#csv--tsv-レイアウト)
- [固定長テキスト (Fixed) レイアウト](#固定長テキスト-fixed-レイアウト)
- [JSON / JsonList レイアウト](#json--jsonlist-レイアウト)
- [YAML レイアウト](#yaml-レイアウト)
- [XML レイアウト](#xml-レイアウト)
- [画像 (Image) レイアウト](#画像-image-レイアウト)
- [比較条件 (criteria) と比較キー (compareKey)](#比較条件-criteria-と比較キー-comparekey)

---

## 比較レイアウトの基礎

### レイアウト定義ファイル

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

### レイアウト共通フィールド

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

### fileRegexPattern のマッチング仕様

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

### レイアウトの読み込み順序 (後勝ちマージ)

1. バイナリ同梱のデフォルト (サンプル定義)
2. `COMPAREFILES_CLASSPATH` の各ディレクトリ直下の `compare_layout/`
3. カレントディレクトリの `compare_layout/`
4. `./config/compare_layout/`
5. `-layout, --overwriteLayoutDir` で指定したディレクトリ (カンマ区切りで複数可、記載順)

### レイアウトが適用されない場合の挙動

fileRegexPattern がどのレイアウトにもマッチしないファイルは:

- 拡張子が画像 (png/jpg/jpeg/gif/bmp) → デフォルトの画像比較
- それ以外 → Text 扱い (行全体を 1 項目として文字列比較、ソートなし)

### レイアウトの検証

```bash
compare_files --lintLayout <layout.json>
```

必須項目・enum 値・正規表現・Fixed のバイト長整合などを検証する。
エラーなし: exit 0 / エラーあり: exit 6。警告 (WARN) は実行可能だが意図の確認を推奨。

### レイアウト作成の補助 (Google Sheets)

スプレッドシートからレイアウトを生成する [generator](https://docs.google.com/spreadsheets/d/1pZ7Ta75L5zM8JshsTG9axp0QXs3WG6Xxy1N81JhF11M/copy) も利用できる。
`__tmpl__` シートをコピーして記載し、メニュー「【compare-files】→ generate layout」で
`__generated__` シートに JSON が生成される。

---

## CSV / TSV レイアウト

### fileFormat の選択

| fileFormat | 用途 |
|:--|:--|
| CSV_withHeader | ヘッダー行あり CSV (カンマ区切り、クォート `"`、エスケープ `""`) |
| CSV_noHeader | ヘッダー行なし CSV |
| TSV_withHeader | ヘッダー行あり TSV (タブ区切り、クォート `"`、エスケープ `\`) |
| TSV_noHeader | ヘッダー行なし TSV |

withHeader 形式は、起動設定の `csvHeaderRow` (既定 1) / `csvDataStartRow` (既定 2) に従って
ヘッダー行を読み飛ばす。データが 3 行目から始まるファイルは `-cd 3` 等で調整する。

### 項目定義

カラムは**レイアウトの itemList の並び順**でマッピングされる (ヘッダーの項目名との突き合わせではない)。
つまり itemList はファイルの**全カラムを左から順に**列挙する必要がある。

```json
{
  "logicalFileName":  "ヘッダーありCSVサンプル",
  "fileRegexPattern": "csv_with-header.*\\.csv",
  "fileFormat":       "CSV_withHeader",
  "charset":          "utf8",
  "lineSp":           "LF",
  "recordList": [
    {
      "type": "Data", "codeValue": "-", "itemList": [
        { "id": "KEY1",             "name": "キー1",    "criteria": "Equal", "compareKey": "true" },
        { "id": "KEY2",             "name": "キー2",    "criteria": "Equal", "compareKey": "true" },
        { "id": "AMOUNT",           "name": "金額",     "criteria": "Equal", "compareKey": "false" },
        { "id": "NUMBER",           "name": "連番",     "criteria": "Ignore", "compareKey": "false" },
        { "id": "LAST_UPDATE_TIME", "name": "更新時刻", "criteria": "Datetime_GreaterEqualThan_Left", "compareKey": "false" }
      ]
    }
  ]
}
```

- `id`: 比較結果 (CompareDetail) のカラム名になる。物理名 / 論理名どちらでもよい
- `name`: 説明用 (比較には使われない)
- `byteLength`: CSV/TSV では比較に使われない (省略可)
- `compareKey`: 行の対応付けに使うキー項目は `"true"` (文字列でも真偽値でも可)

### レコードタイプが 1 つの場合

`type: "Data"`、`codeValue: "-"` (起動設定 `codeValueForOnlyOneRecordType` の既定値) とするのが慣例。

### マルチレコードタイプ (ヘッダー/データ/トレーラ行が混在する CSV)

1 カラム目の先頭文字列でレコードタイプを判別する。各レコードに `codeValue`
(判別文字列。例: Header=`"0"`, Data=`"1"`) を指定し、レコードごとに itemList を定義する。

- 判別は「1 カラム目の値の先頭が codeValue に一致するか」で行う
- CSV_withHeader / TSV_withHeader では機能しない (常に Data 扱いになる) ため、
  マルチレコードタイプは noHeader 形式で使う
- 比較結果のカラム名は `レコードタイプ.項目ID` (例: `Data.KEY1`) になる

### 注意点

- 値にカンマや改行を含むセルはクォートされていれば正しく扱われる
- 空行があるとそこで読み込みが終了する (ファイル末尾以外に空行を置かない)
- 比較前に内部で行ソートが行われる (キー項目 → その他項目の順)。
  入力が既にキー順でソート済みなら `-s, --sorted` でソートをスキップでき、高速化できる

---

## 固定長テキスト (Fixed) レイアウト

### 基本

各行をバイト位置で項目に分割して比較する。**recordList と各項目の byteLength が必須**。

```json
{
  "logicalFileName":  "固定長テキストサンプル",
  "fileRegexPattern": "fixed_multi-record-type.*\\.txt",
  "fileFormat":       "Fixed",
  "charset":          "utf8",
  "lineSp":           "LF",
  "recordList": [
    {
      "type": "Header", "codeValue": "0", "itemList": [
        { "id": "RECORD_TYPE", "byteLength": 1,  "criteria": "Equal", "compareKey": "true"  },
        { "id": "DATA_COUNT",  "byteLength": 10, "criteria": "Equal", "compareKey": "false" },
        { "id": "BLANK",       "byteLength": 39, "criteria": "Equal", "compareKey": "false" }
      ]
    },
    {
      "type": "Data", "codeValue": "1", "itemList": [
        { "id": "RECORD_TYPE", "byteLength": 1,  "criteria": "Equal", "compareKey": "true" },
        { "id": "KEY1",        "byteLength": 3,  "criteria": "Equal", "compareKey": "true" },
        { "id": "AMOUNT",      "byteLength": 10, "criteria": "Equal", "compareKey": "false" },
        { "id": "BLANK",       "byteLength": 36, "criteria": "Equal", "compareKey": "false" }
      ]
    }
  ]
}
```

### byteLength の仕様 (重要)

- 単位は**バイト** (文字数ではない)。charset が ms932 なら全角 1 文字 = 2 バイト、
  UTF-8 なら日本語 1 文字 = 通常 3 バイト
- 項目の byteLength を順に合計した長さが 1 レコードのバイト長になる
- マルチバイト文字が項目境界をまたぐと文字化けする。境界は文字の切れ目に合わせて設計する
- **全レコードタイプのバイト長合計は同一**でなければならない (先頭レコードの長さで全行を
  読み込むため)。予備 (BLANK) 項目で長さを揃えるのが慣例

### レコードタイプの判別

- 行の先頭から `codeValue` の文字数分を切り出して一致判定する
  (例: 先頭 1 バイトが `0` → Header)。codeValue は ASCII 前提
- レコードタイプが 1 つだけで判別文字列が存在しないファイルは、
  `codeValue` に起動設定 `codeValueForOnlyOneRecordType` (既定 `-`) と同じ値を指定する

### lineSp (改行コード)

| 値 | 挙動 |
|:--|:--|
| LF / CR | 各レコードの後の 1 バイトを読み捨てる |
| CRLF | 2 バイトを読み捨てる |
| None | 読み捨てなし。**改行なしで固定長レコードが連結されたファイル**に使う |

`None` の場合は内部ソートがスキップされる (行の出力順がずれていると、ずれたまま比較される)。

### record 単位の byteLength

itemList の合計と別に `recordList[].byteLength` を明示することもできる (通常は省略でよい)。

---

## JSON / JsonList レイアウト

### fileFormat の選択

| fileFormat | 用途 | ソート |
|:--|:--|:--|
| Json | ファイル全体で 1 つの JSON オブジェクト | なし (1 レコード扱い) |
| JsonList | 1 行 = 1 JSON オブジェクト (改行区切り JSON / NDJSON) | あり (キー項目順) |

### 項目定義: ドット記法

ネストした値は項目 ID をドット区切りで指定する。**比較したい項目だけを列挙**すればよい
(CSV と違い全項目の列挙は不要。列挙しなかった項目は比較されない)。

```json
{
  "logicalFileName":  "JsonListサンプル",
  "fileRegexPattern": "jsonlist_.*\\.json",
  "fileFormat":       "JsonList",
  "charset":          "utf8",
  "lineSp":           "LF",
  "recordList": [
    {
      "type": "Data", "codeValue": "-", "itemList": [
        { "id": "keyObj.key1",             "criteria": "Equal", "compareKey": "true" },
        { "id": "keyObj.key2",             "criteria": "Equal", "compareKey": "true" },
        { "id": "valueObj.amount",         "criteria": "Equal", "compareKey": "false" },
        { "id": "valueObj.lastUpdateTime", "criteria": "Datetime_GreaterEqualThan_Left", "compareKey": "false" }
      ]
    }
  ]
}
```

対象データの例: `{"keyObj": {"key1": 100, "key2": "AAA"}, "valueObj": {"amount": 1000, ...}}`

### 配列の扱い

パスの途中に配列がある場合、**全要素**の値を集めたリストとして比較される。

- `valueList.content.month` は「`valueList` 配列の各要素の `content.month`」を集めて
  `[201701, 201702, 201703]` のようなリスト文字列として比較する
- 配列の特定インデックスだけを指定する記法はない
- 要素の順序が異なると差分になる

### 制約

- レコードタイプは 1 つのみ (`type: "Data"`, `codeValue: "-"`)。複数定義すると実行時エラー
- 指定したパスの親が存在しない行があるとエラーになる (`keyObj.key1` を指定したのに
  `keyObj` がない行など)。スキーマが行によって異なるファイルでは共通部分だけを指定する
- 値の表記は Java 的な文字列化で比較される: 数値 `1000` と文字列 `"1000"` は一致、
  小数は `1.0` 形式、ネストオブジェクトは `{key=value}` 形式
- Json (単一オブジェクト) はソートされないため compareKey の意味は薄いが、
  形式上 1 項目以上に `compareKey: "true"` を付けておく

### pathValueMode: jsonPath ベースの比較

`"pathValueMode": "true"` を指定すると、itemList の定義なしで、ドキュメントを
**jsonPath → リーフ値** のペアに平坦化して比較する。プロパティの挿入位置を考慮した
比較をしたい場合に利用する (挿入されたプロパティは LeftOnly / RightOnly として現れ、
他のプロパティの比較はズレない)。

```json
{
  "logicalFileName":  "JsonPathサンプル",
  "fileRegexPattern": "api_response_.*\\.json",
  "fileFormat":       "Json",
  "charset":          "utf8",
  "pathValueMode":    "true"
}
```

- 結果 CSV は `path` (比較キー)・`value` の 2 カラムになる
- パス表記: ルートは `$`、オブジェクトキーは `.key`、配列は `[0]` (0 始まり)。
  記号を含むキーはブラケット表記 `['key-name']`
- `Json` はルートから `$.a.b[0]`、`JsonList` は行ごとに `$[行番号].a.b` の形式
- 空のオブジェクト・配列は `{}` / `[]` を値として 1 ペア出力される
- `recordList` は使用されない (定義しても無視され、lint で警告になる)
- 配列要素の**挿入**では後続要素のインデックスがずれるため、挿入位置以降は
  LeftOnly / RightOnly のペアとして出力される

---

## YAML レイアウト

`fileFormat: "Yaml"` はファイル全体を 1 つの YAML マッピングとして読み込み、
JSON (`fileFormat: "Json"`) と同じイメージで項目単位に比較する。

### 項目定義: ドット記法

JSON と同様に、ネストした値は項目 ID をドット区切りで指定する。
**比較したい項目だけを列挙**すればよい。

```json
{
  "logicalFileName":  "Yamlサンプル",
  "fileRegexPattern": "config_.*\\.ya?ml",
  "fileFormat":       "Yaml",
  "charset":          "utf8",
  "recordList": [
    {
      "type": "Data", "codeValue": "-", "itemList": [
        { "id": "metadata.name",  "criteria": "Equal", "compareKey": "true" },
        { "id": "spec.replicas",  "criteria": "Equal", "compareKey": "false" },
        { "id": "spec.containers", "criteria": "Equal", "compareKey": "false" }
      ]
    }
  ]
}
```

対象データの例:

```yaml
metadata:
  name: my-app
spec:
  replicas: 3
  containers:
    - nginx
    - sidecar
```

### 値の解釈

- キーの順序は比較に影響しない (項目 ID で参照するため)
- アンカー / エイリアス (`&a` / `*a`) は展開してから比較される
- スカラーは YAML のタグで解釈される: 数値は JSON と同じ数値表記
  (16 進などは 10 進に正規化)、真偽値は `true` / `false`、null は `null`
- 配列はパスの全要素を集めたリスト (`[a, b]` 形式) として比較される (JSON と同じ)

### 制約 (JSON と共通)

- ファイル全体で 1 レコード扱い。レコードタイプは 1 つのみで、複数定義すると実行時エラー
- 指定したパスの親が存在しないとエラーになる
- 複数ドキュメント (`---` 区切り) は先頭ドキュメントのみ比較される

### pathValueMode

`"pathValueMode": "true"` を指定すると、JSON と同様に itemList の定義なしで
jsonPath → 値のペアに平坦化して比較できる (詳細は JSON / JsonList のリファレンス)。

```json
{
  "logicalFileName":  "YamlPathサンプル",
  "fileRegexPattern": "values_.*\\.ya?ml",
  "fileFormat":       "Yaml",
  "charset":          "utf8",
  "pathValueMode":    "true"
}
```

---

## XML レイアウト

`fileFormat: "XML"` はスキーマ (itemList) の定義なしで、ドキュメントを
**xpath → 値** のペアに平坦化して比較する。xpath が比較キー (項目 ID 相当) になる。

```json
{
  "logicalFileName":  "XMLサンプル",
  "fileRegexPattern": "order_.*\\.xml",
  "fileFormat":       "XML",
  "charset":          "utf8"
}
```

### パスと値の対応

対象データの例:

```xml
<root>
  <item id="1">
    <name>alice</name>
  </item>
  <item id="2">
    <name>bob</name>
  </item>
  <mixed>text<sub>s</sub></mixed>
</root>
```

| path | value |
|:-----|:------|
| `/root[1]/item[1]/@id` | `1` |
| `/root[1]/item[1]/name[1]` | `alice` |
| `/root[1]/item[2]/@id` | `2` |
| `/root[1]/item[2]/name[1]` | `bob` |
| `/root[1]/mixed[1]/text()` | `text` |
| `/root[1]/mixed[1]/sub[1]` | `s` |

- 要素のインデックス `[n]` は**同名の兄弟要素内での 1 始まりの出現順**。
  左右でインデックスが常に同じ規則で振られるため、要素の挿入があった場合は
  挿入位置以降が LeftOnly / RightOnly のペアとして出力される
- 属性は `要素パス/@属性名`
- 子要素を持たない要素はそのパスに値 (トリム済みテキスト、空なら空文字) を出力する
- 子要素とテキストが混在する要素は、テキストを `要素パス/text()` に出力する
- 名前空間付きの要素・属性名は `名前空間URI:ローカル名` で表記される
  (プリフィックスは左右のファイルで異なりうるため URI で正規化)
- コメント・処理命令 (PI)・CDATA 区切りは比較対象外 (CDATA の中身はテキスト扱い)
- XML 宣言の encoding は無視され、レイアウトの `charset` が優先される

### 結果 CSV

`path` (比較キー)・`value` の 2 カラム構成で、path 順にソートして比較される。
`recordList` は使用されない (定義しても無視され、lint で警告になる)。

---

## 画像 (Image) レイアウト

### 基本

ピクセル単位の厳密一致で比較する (許容誤差なし)。対応形式: png / jpg / jpeg / gif / bmp。

```json
{
  "logicalFileName":  "PNGサンプル",
  "fileRegexPattern": "png.*\\.png",
  "fileFormat":       "Image",
  "ignoreAreaList": [
    { "x": 256, "y": 452, "width": 120, "height": 60 }
  ]
}
```

- `recordList` / `charset` / `lineSp` は不要
- レイアウトを定義しなくても、画像拡張子のファイルは自動で画像比較になる。
  レイアウトが必要になるのは **ignoreAreaList を指定したいとき**が主

### ignoreAreaList (比較除外エリア)

タイムスタンプ表示など、実行のたびに変わる領域をマスクして比較から除外する。

- 座標は左上原点のピクセル指定 (`x`, `y`, `width`, `height`)
- レイアウトの ignoreAreaList に加えて、起動設定 (compare_files.json) の
  `ignoreAreaList` が**常にマージ**される。デフォルト設定には
  `{"x":0,"y":0,"width":1024,"height":128}` が入っているため、画面上部を比較したい場合は
  設定側を空にすること
- 除外エリア数は結果サマリの Ignore Row 列に出力される

### 比較結果

- 差分ピクセルは近接するもの同士 (距離 10px) がグループ化され、矩形の差分エリアになる。
  差分エリア数がサマリの NG Row 列に出力される
- 画像サイズが異なる場合、重ならない領域全体が差分になる
- 結果画像 (CompareDetail_*.png) は左右並置で出力される:
  - NG: 差分エリアを赤枠と矢印でマーク、右側の背景が赤
  - OK: 除外エリアを半透明マスク表示、右側の背景が緑 (writeDiffOnly 時は出力されない)
- ラベル (期待値: / 実績値:) やボーダー色は起動設定の `okImageStyle` / `ngImageStyle` で変更できる

### 注意点

- JPEG は非可逆圧縮のため、同じ見た目でも保存し直すとピクセル値が変わり差分になる。
  生成プロセスが同一のファイル同士の比較に向く
- 差分エリアが大量に出る場合はサイズ違いか全面的な色差。ignoreArea でしのがず原因を確認する

---

## 比較条件 (criteria) と比較キー (compareKey)

### compareKey: 行の対応付け

テキスト比較は左右のファイルを**キー項目でソートしてからマージ結合**する。

- `compareKey: "true"` の項目の組がレコードを一意に識別するように選ぶ
  (主キー相当。ユニークな値を持つカラム)
- キーが一致した行同士だけが項目比較され、一致しない行は LeftOnly / RightOnly になる
- **キー項目がないと行の対応付けができない**: 全行が同一キー扱いになり、
  出力順のずれがそのまま差分になる
- キーに「実行のたびに変わる値」(タイムスタンプ・採番 ID) を選ぶと全行 LeftOnly/RightOnly に
  なる。安定した業務キーを選ぶ

### criteria 一覧

「左 = 期待値、右 = 実績値」として、条件を満たすと OK。

| criteria | OK になる条件 |
|:--|:--|
| Ignore | 常に除外 (OK でも NG でもなく Ignore として集計) |
| Equal | 文字列として一致 (空文字と null は同一視) |
| NotEqual | 文字列として不一致 |
| Number_GreaterThan_Left | 数値として 左 < 右 |
| Number_GreaterEqualThan_Left | 数値として 左 <= 右 |
| Number_LessThan_Left | 数値として 左 > 右 |
| Number_LessEqualThan_Left | 数値として 左 >= 右 |
| Year_* (4 種) | 先頭 4 文字を年 (yyyy) として同様に比較 |
| Month_* (4 種) | 先頭 6 文字を年月 (yyyyMM) として同様に比較 |
| Date_* (4 種) | 年月日として比較 (時刻は切り捨て) |
| Datetime_* (4 種) | 年月日時分秒 (ミリ秒含む) として比較 |

`*` はそれぞれ `GreaterThan_Left` / `GreaterEqualThan_Left` / `LessThan_Left` / `LessEqualThan_Left`。
名前は「右が左より Greater なら OK」と読む (例: `Datetime_GreaterEqualThan_Left` =
実績値の時刻が期待値**以降**なら OK)。

- criteria を省略した場合は Equal 扱い
- 数値比較は任意精度 (カンマ区切り文字列 `1,000` は数値変換できずエラーになるので Equal を使う)
- 数値・日付系で片方が空の場合は NG。**変換できない値 (非数値・非日付) はエラー**になり、
  そのファイルの比較全体が Error 扱いになる。値が入らないことがある項目には使わない

### Date / Datetime が受け付ける形式

`yyyy/MM/dd`・`yyyy-MM-dd`・`yyyyMMdd`・`yy/MM/dd` (+ 時刻 `HH:mm` / `HH:mm:ss` / `HH:mm:ss.SSS`)、
ISO-8601 (`2017-01-01T00:00:00.000+09:00`)、Apache アクセスログ形式 (`[01/Jan/2017:00:00:00 +0900]`)。
区切りなしの `yyyyMMddHHmmss` (14 桁) は**非対応** (`yyyyMMdd HH:mm:ss` のように日付と時刻の間に
区切りが必要)。

### 選び方の指針

| 項目の性質 | 推奨 criteria |
|:--|:--|
| 業務キー・不変の値 | Equal (+ キーなら compareKey: true) |
| 更新時刻・処理時刻 (再実行で必ず進む) | Datetime_GreaterEqualThan_Left または Ignore |
| 採番・連番 (値に意味がない) | Ignore |
| 増加していることだけ確認したい数値 | Number_GreaterEqualThan_Left |
| 毎回変わることを確認したい値 | NotEqual |

起動設定の `ignoreItemList` に項目 ID を並べると、レイアウトを変更せずに一括で
Ignore にできる (デフォルト設定には `last_update_time` / `LAST_UPDATE_TIME` が入っている)。
