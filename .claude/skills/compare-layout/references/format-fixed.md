# 固定長テキスト (Fixed) レイアウト

## 基本

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

## byteLength の仕様 (重要)

- 単位は**バイト** (文字数ではない)。charset が ms932 なら全角 1 文字 = 2 バイト、
  UTF-8 なら日本語 1 文字 = 通常 3 バイト
- 項目の byteLength を順に合計した長さが 1 レコードのバイト長になる
- マルチバイト文字が項目境界をまたぐと文字化けする。境界は文字の切れ目に合わせて設計する
- **全レコードタイプのバイト長合計は同一**でなければならない (先頭レコードの長さで全行を
  読み込むため)。予備 (BLANK) 項目で長さを揃えるのが慣例

## レコードタイプの判別

- 行の先頭から `codeValue` の文字数分を切り出して一致判定する
  (例: 先頭 1 バイトが `0` → Header)。codeValue は ASCII 前提
- レコードタイプが 1 つだけで判別文字列が存在しないファイルは、
  `codeValue` に起動設定 `codeValueForOnlyOneRecordType` (既定 `-`) と同じ値を指定する

## lineSp (改行コード)

| 値 | 挙動 |
|:--|:--|
| LF / CR | 各レコードの後の 1 バイトを読み捨てる |
| CRLF | 2 バイトを読み捨てる |
| None | 読み捨てなし。**改行なしで固定長レコードが連結されたファイル**に使う |

`None` の場合は内部ソートがスキップされる (行の出力順がずれていると、ずれたまま比較される)。

## record 単位の byteLength

itemList の合計と別に `recordList[].byteLength` を明示することもできる (通常は省略でよい)。
