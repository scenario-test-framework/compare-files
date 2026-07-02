package config

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"sort"
	"sync"
	"unicode/utf16"

	"github.com/dlclark/regexp2"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// LayoutManager はファイルレイアウト定義のレジストリです。
// Java 版 FileLayoutManager 相当。正規表現文字列をキーに TreeMap(辞書順)で保持し、
// getLayout は辞書順の先勝ちマッチで解決します。
type LayoutManager struct {
	layoutMap  map[string]*FileLayout
	sortedKeys []string                   // layoutMap キーの Java TreeMap 順キャッシュ
	regexCache map[string]*regexp2.Regexp // コンパイル済み全体一致正規表現 (エラー時は未登録)
	mu         sync.Mutex                 // 並列比較時の GetLayout 呼び出しを保護
}

// NewLayoutManager は空のレイアウトマネージャを返します。
func NewLayoutManager() *LayoutManager {
	return &LayoutManager{
		layoutMap:  map[string]*FileLayout{},
		regexCache: map[string]*regexp2.Regexp{},
	}
}

// AddLayoutData はレイアウト定義 JSON のバイト列を登録します(同一正規表現は後勝ち)。
func (m *LayoutManager) AddLayoutData(data []byte, sourceName string) error {
	list, err := ParseLayoutList(data)
	if err != nil {
		return fmt.Errorf("レイアウト定義をパースできません: %s: %w", sourceName, err)
	}
	for _, layout := range list.LayoutList {
		m.layoutMap[layout.FileRegexPattern] = layout
	}
	m.sortedKeys = nil // キー順キャッシュを無効化
	return nil
}

// AddLayoutFile はレイアウト定義ファイルを登録します。
func (m *LayoutManager) AddLayoutFile(path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return fmt.Errorf("レイアウト定義ファイルを読み込めません: %s: %w", path, err)
	}
	return m.AddLayoutData(data, path)
}

// AddLayoutDir は指定ディレクトリ直下の全ファイルをレイアウト定義として登録します。
func (m *LayoutManager) AddLayoutDir(dirPath string) error {
	if dirPath == "" {
		return fmt.Errorf("dirPath が指定されていません")
	}
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		return fmt.Errorf("レイアウト定義ディレクトリを読み込めません: %s: %w", dirPath, err)
	}
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		if err := m.AddLayoutFile(filepath.Join(dirPath, entry.Name())); err != nil {
			return err
		}
	}
	return nil
}

// GetLayout は物理ファイル名にマッチするレイアウトのコピーを返します。
// マッチしない場合は nil を返します。systemConfig が指定されている場合、
// 除外項目・除外エリアを適用します。
func (m *LayoutManager) GetLayout(physicalFileName string, systemConfig *CompareFilesConfig) *FileLayout {
	m.mu.Lock()
	defer m.mu.Unlock()
	var matched *FileLayout
	for _, regex := range m.keys() {
		layout := m.layoutMap[regex]
		re, ok := m.regexCache[regex]
		if !ok {
			compiled, err := regexp2.Compile(`\A(?:`+regex+`)\z`, regexp2.None)
			if err != nil {
				slog.Error("正規表現の評価でエラーが発生しました。",
					"レイアウト", layout.LogicalFileName, "正規表現", regex, "error", err)
				continue
			}
			m.regexCache[regex] = compiled
			re = compiled
		}
		isMatch, err := re.MatchString(physicalFileName)
		if err != nil {
			slog.Error("正規表現の評価でエラーが発生しました。",
				"レイアウト", layout.LogicalFileName, "正規表現", regex, "error", err)
			continue
		}
		if isMatch {
			matched = layout.Copy()
			break
		}
	}
	if matched == nil {
		return nil
	}
	if systemConfig != nil {
		updateIgnore(systemConfig, matched)
	}
	return matched
}

// keys は Java の TreeMap<String, …> と同じ順序(UTF-16 コード単位の辞書順)で
// 正規表現キーを返します。結果はキャッシュされ、登録時に無効化されます。
func (m *LayoutManager) keys() []string {
	if m.sortedKeys != nil {
		return m.sortedKeys
	}
	keys := make([]string, 0, len(m.layoutMap))
	for k := range m.layoutMap {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool {
		return compareJavaString(keys[i], keys[j]) < 0
	})
	m.sortedKeys = keys
	return keys
}

// compareJavaString は Java String.compareTo と同じ UTF-16 コード単位順で比較します。
func compareJavaString(a, b string) int {
	ua := utf16.Encode([]rune(a))
	ub := utf16.Encode([]rune(b))
	for i := 0; i < len(ua) && i < len(ub); i++ {
		if ua[i] != ub[i] {
			if ua[i] < ub[i] {
				return -1
			}
			return 1
		}
	}
	return len(ua) - len(ub)
}

// matchesFull は Java の Pattern.matches (全体一致) セマンティクスで照合します。
func matchesFull(pattern, s string) (bool, error) {
	re, err := regexp2.Compile(`\A(?:`+pattern+`)\z`, regexp2.None)
	if err != nil {
		return false, err
	}
	return re.MatchString(s)
}

// MatchesFull は Java の Pattern.matches 相当の全体一致照合です(外部パッケージ用)。
func MatchesFull(pattern, s string) (bool, error) {
	return matchesFull(pattern, s)
}

// updateIgnore はシステム設定の除外項目・除外エリアをレイアウトに反映します。
func updateIgnore(systemConfig *CompareFilesConfig, layout *FileLayout) {
	if layout.FileFormat == status.FormatImage {
		var merged []Rectangle
		merged = append(merged, layout.IgnoreAreaList...)
		merged = append(merged, systemConfig.IgnoreAreaList...)
		layout.IgnoreAreaList = merged
		return
	}
	if systemConfig.IgnoreItemList == nil {
		return
	}
	for _, record := range layout.RecordList {
		for _, item := range record.ItemList {
			for _, ignoreName := range systemConfig.IgnoreItemList {
				if ignoreName == item.ID {
					item.Criteria = status.CriteriaIgnore
					break
				}
			}
		}
	}
}
