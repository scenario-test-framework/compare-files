# CSV / TSV レイアウト

## fileFormat の選択

| fileFormat | 用途 |
|:--|:--|
| CSV_withHeader | ヘッダー行あり CSV (カンマ区切り、クォート `"`、エスケープ `""`) |
| CSV_noHeader | ヘッダー行なし CSV |
| TSV_withHeader | ヘッダー行あり TSV (タブ区切り、クォート `"`、エスケープ `\`) |
| TSV_noHeader | ヘッダー行なし TSV |

withHeader 形式は、起動設定の `csvHeaderRow` (既定 1) / `csvDataStartRow` (既定 2) に従って
ヘッダー行を読み飛ばす。データが 3 行目から始まるファイルは `-cd 3` 等で調整する。

## 項目定義

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

## レコードタイプが 1 つの場合

`type: "Data"`、`codeValue: "-"` (起動設定 `codeValueForOnlyOneRecordType` の既定値) とするのが慣例。

## マルチレコードタイプ (ヘッダー/データ/トレーラ行が混在する CSV)

1 カラム目の先頭文字列でレコードタイプを判別する。各レコードに `codeValue`
(判別文字列。例: Header=`"0"`, Data=`"1"`) を指定し、レコードごとに itemList を定義する。

- 判別は「1 カラム目の値の先頭が codeValue に一致するか」で行う
- CSV_withHeader / TSV_withHeader では機能しない (常に Data 扱いになる) ため、
  マルチレコードタイプは noHeader 形式で使う
- 比較結果のカラム名は `レコードタイプ.項目ID` (例: `Data.KEY1`) になる

## 注意点

- 値にカンマや改行を含むセルはクォートされていれば正しく扱われる
- 空行があるとそこで読み込みが終了する (ファイル末尾以外に空行を置かない)
- 比較前に内部で行ソートが行われる (キー項目 → その他項目の順)。
  入力が既にキー順でソート済みなら `-s, --sorted` でソートをスキップでき、高速化できる
