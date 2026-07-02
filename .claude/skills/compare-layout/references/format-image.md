# 画像 (Image) レイアウト

## 基本

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

## ignoreAreaList (比較除外エリア)

タイムスタンプ表示など、実行のたびに変わる領域をマスクして比較から除外する。

- 座標は左上原点のピクセル指定 (`x`, `y`, `width`, `height`)
- レイアウトの ignoreAreaList に加えて、起動設定 (compare_files.json) の
  `ignoreAreaList` が**常にマージ**される。デフォルト設定には
  `{"x":0,"y":0,"width":1024,"height":128}` が入っているため、画面上部を比較したい場合は
  設定側を空にすること
- 除外エリア数は結果サマリの Ignore Row 列に出力される

## 比較結果

- 差分ピクセルは近接するもの同士 (距離 10px) がグループ化され、矩形の差分エリアになる。
  差分エリア数がサマリの NG Row 列に出力される
- 画像サイズが異なる場合、重ならない領域全体が差分になる
- 結果画像 (CompareDetail_*.png) は左右並置で出力される:
  - NG: 差分エリアを赤枠と矢印でマーク、右側の背景が赤
  - OK: 除外エリアを半透明マスク表示、右側の背景が緑 (writeDiffOnly 時は出力されない)
- ラベル (期待値: / 実績値:) やボーダー色は起動設定の `okImageStyle` / `ngImageStyle` で変更できる

## 注意点

- JPEG は非可逆圧縮のため、同じ見た目でも保存し直すとピクセル値が変わり差分になる。
  生成プロセスが同一のファイル同士の比較に向く
- 差分エリアが大量に出る場合はサイズ違いか全面的な色差。ignoreArea でしのがず原因を確認する
