# 比較条件 (criteria) と比較キー (compareKey)

## compareKey: 行の対応付け

テキスト比較は左右のファイルを**キー項目でソートしてからマージ結合**する。

- `compareKey: "true"` の項目の組がレコードを一意に識別するように選ぶ
  (主キー相当。ユニークな値を持つカラム)
- キーが一致した行同士だけが項目比較され、一致しない行は LeftOnly / RightOnly になる
- **キー項目がないと行の対応付けができない**: 全行が同一キー扱いになり、
  出力順のずれがそのまま差分になる
- キーに「実行のたびに変わる値」(タイムスタンプ・採番 ID) を選ぶと全行 LeftOnly/RightOnly に
  なる。安定した業務キーを選ぶ

## criteria 一覧

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

## Date / Datetime が受け付ける形式

`yyyy/MM/dd`・`yyyy-MM-dd`・`yyyyMMdd`・`yy/MM/dd` (+ 時刻 `HH:mm` / `HH:mm:ss` / `HH:mm:ss.SSS`)、
ISO-8601 (`2017-01-01T00:00:00.000+09:00`)、Apache アクセスログ形式 (`[01/Jan/2017:00:00:00 +0900]`)。
区切りなしの `yyyyMMddHHmmss` (14 桁) は**非対応** (`yyyyMMdd HH:mm:ss` のように日付と時刻の間に
区切りが必要)。

## 選び方の指針

| 項目の性質 | 推奨 criteria |
|:--|:--|
| 業務キー・不変の値 | Equal (+ キーなら compareKey: true) |
| 更新時刻・処理時刻 (再実行で必ず進む) | Datetime_GreaterEqualThan_Left または Ignore |
| 採番・連番 (値に意味がない) | Ignore |
| 増加していることだけ確認したい数値 | Number_GreaterEqualThan_Left |
| 毎回変わることを確認したい値 | NotEqual |

起動設定の `ignoreItemList` に項目 ID を並べると、レイアウトを変更せずに一括で
Ignore にできる (デフォルト設定には `last_update_time` / `LAST_UPDATE_TIME` が入っている)。
