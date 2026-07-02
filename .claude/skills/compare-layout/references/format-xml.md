# XML レイアウト

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

## パスと値の対応

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
| `/root[1]/mixed[1]/text()[1]` | `text` |
| `/root[1]/mixed[1]/sub[1]` | `s` |

- 要素のインデックス `[n]` は**同名の兄弟要素内での 1 始まりの出現順**。
  左右でインデックスが常に同じ規則で振られるため、要素の挿入があった場合は
  挿入位置以降が LeftOnly / RightOnly のペアとして出力される
- 属性は `要素パス/@属性名`
- 子要素を持たない要素はそのパスに値 (トリム済みテキスト、空なら空文字) を出力する。
  先頭末尾の空白は pretty-print 由来のゆらぎを差分にしないため意図的に無視される
- 子要素とテキストが混在する要素は、**子要素で区切られたテキストランごとに**
  `要素パス/text()[n]` (1 始まりの出現順) に出力する。
  `<mixed>a<sub/>b</mixed>` と `<mixed>ab<sub/></mixed>` はテキスト境界が異なる差分として検出される。
  各ランも leaf 要素と同じく前後の空白をトリムして比較し、空白のみのランは無視する
  (`foo <b/>bar` と `foo<b/> bar` の空白位置の違いは差分にならない)
- 名前空間付きの要素・属性名は `名前空間URI:ローカル名` で表記される
  (プリフィックスは左右のファイルで異なりうるため URI で正規化)
- コメント・処理命令 (PI)・CDATA 区切りは比較対象外 (CDATA の中身はテキスト扱い)
- XML 宣言の encoding は無視され、レイアウトの `charset` が優先される

## 結果 CSV

`path` (比較キー)・`value` の 2 カラム構成で、path 順にソートして比較される。
`recordList` は使用されない (定義しても無視され、lint で警告になる)。
