# YAML レイアウト

`fileFormat: "Yaml"` はファイル全体を 1 つの YAML マッピングとして読み込み、
JSON (`fileFormat: "Json"`) と同じイメージで項目単位に比較する。

## 項目定義: ドット記法

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

## 値の解釈

- キーの順序は比較に影響しない (項目 ID で参照するため)
- アンカー / エイリアス (`&a` / `*a`) は展開してから比較される
- merge key (`<<: *base`) も YAML merge セマンティクスで展開される
  (マッピング内の明示キーが優先。`<<: [*a, *b]` は先に書かれた方が優先)
- スカラーは YAML のタグで解釈される: 数値は JSON と同じ数値表記
  (16 進などは 10 進に正規化)、真偽値は `true` / `false`、null は `null`
- 配列はパスの全要素を集めたリスト (`[a, b]` 形式) として比較される (JSON と同じ)

## 制約 (JSON と共通)

- ファイル全体で 1 レコード扱い。レコードタイプは 1 つのみで、複数定義すると実行時エラー
- 指定したパスの親が存在しないとエラーになる
- 複数ドキュメント (`---` 区切り) は先頭ドキュメントのみ比較される

## pathValueMode

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
