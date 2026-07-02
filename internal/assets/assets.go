// Package assets はバイナリに同梱するデフォルトリソースを提供します。
// Java 版のクラスパスリソース (src/main/resources) 相当です。
package assets

import (
	"embed"
)

//go:embed compare_files.json compare_layout arrow.png fonts
var fs embed.FS

// DefaultConfig は同梱のデフォルト設定 compare_files.json を返します。
func DefaultConfig() []byte {
	data, err := fs.ReadFile("compare_files.json")
	if err != nil {
		panic(err) // embed 済みリソースの読み込み失敗はビルド不整合
	}
	return data
}

// DefaultLayouts は同梱のレイアウト定義(ファイル名→内容)を返します。
func DefaultLayouts() map[string][]byte {
	entries, err := fs.ReadDir("compare_layout")
	if err != nil {
		panic(err)
	}
	layouts := map[string][]byte{}
	for _, entry := range entries {
		data, err := fs.ReadFile("compare_layout/" + entry.Name())
		if err != nil {
			panic(err)
		}
		layouts[entry.Name()] = data
	}
	return layouts
}

// Font は画像ラベル描画用フォント SourceHanCodeJP-Normal.otf を返します。
func Font() []byte {
	data, err := fs.ReadFile("fonts/SourceHanCodeJP-Normal.otf")
	if err != nil {
		panic(err)
	}
	return data
}

// ArrowImage は差分マーカー用 arrow.png を返します。
func ArrowImage() []byte {
	data, err := fs.ReadFile("arrow.png")
	if err != nil {
		panic(err)
	}
	return data
}
