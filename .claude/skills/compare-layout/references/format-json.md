# JSON / JsonList レイアウト

## fileFormat の選択

| fileFormat | 用途 | ソート |
|:--|:--|:--|
| Json | ファイル全体で 1 つの JSON オブジェクト | なし (1 レコード扱い) |
| JsonList | 1 行 = 1 JSON オブジェクト (改行区切り JSON / NDJSON) | あり (キー項目順) |

## 項目定義: ドット記法

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

## 配列の扱い

パスの途中に配列がある場合、**全要素**の値を集めたリストとして比較される。

- `valueList.content.month` は「`valueList` 配列の各要素の `content.month`」を集めて
  `[201701, 201702, 201703]` のようなリスト文字列として比較する
- 配列の特定インデックスだけを指定する記法はない
- 要素の順序が異なると差分になる

## 制約

- レコードタイプは 1 つのみ (`type: "Data"`, `codeValue: "-"`)。複数定義すると実行時エラー
- 指定したパスの親が存在しない行があるとエラーになる (`keyObj.key1` を指定したのに
  `keyObj` がない行など)。スキーマが行によって異なるファイルでは共通部分だけを指定する
- 値の表記は Java 的な文字列化で比較される: 数値 `1000` と文字列 `"1000"` は一致、
  小数は `1.0` 形式、ネストオブジェクトは `{key=value}` 形式
- Json (単一オブジェクト) はソートされないため compareKey の意味は薄いが、
  形式上 1 項目以上に `compareKey: "true"` を付けておく

## pathValueMode: jsonPath ベースの比較

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
- 動くサンプル: 同梱の `sample/left|right/TEXT_JSONPATH/jsonpath_*.json` と
  レイアウト `JsonPathサンプル` (`jsonpath_.*\\.json`) で挙動を確認できる
