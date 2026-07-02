package cli

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"strings"

	"github.com/scenario-test-framework/compare-files/internal/assets"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// EnvClasspath は設定探索パスを追加する環境変数名です(Java 版と互換)。
const EnvClasspath = "COMPAREFILES_CLASSPATH"

// configSearchDirs は設定リソースの探索ディレクトリを優先順で返します。
// Java 版の classpath 順序 (COMPAREFILES_CLASSPATH → "." → ./config) を再現します。
func configSearchDirs() []string {
	var dirs []string
	if env := os.Getenv(EnvClasspath); env != "" {
		for _, dir := range strings.Split(env, string(os.PathListSeparator)) {
			if dir != "" {
				dirs = append(dirs, dir)
			}
		}
	}
	dirs = append(dirs, ".", "config")
	return dirs
}

// LoadDefaultConfig はデフォルト設定を解決して返します。
// 探索ディレクトリの compare_files.json を前勝ちで解決し、
// 見つからない場合は同梱デフォルトを利用します。
func LoadDefaultConfig() (*config.CompareFilesConfig, string, error) {
	for _, dir := range configSearchDirs() {
		path := filepath.Join(dir, "compare_files.json")
		if info, err := os.Stat(path); err == nil && !info.IsDir() {
			cfg, err := config.ParseConfigFile(path)
			if err != nil {
				return nil, path, err
			}
			return cfg, path, nil
		}
	}
	cfg, err := config.ParseConfig(assets.DefaultConfig())
	if err != nil {
		return nil, "embedded:compare_files.json", err
	}
	return cfg, "embedded:compare_files.json", nil
}

// LoadConfig はデフォルト設定 → -config ファイル → CLI オプションの 3 段階マージを行い、
// 検証済みの設定を返します。Java 版 BaseCompareFiles.execute の設定読込部と同一手順です。
func LoadConfig(opt *Option) (*config.CompareFilesConfig, error) {
	defaultConfig, defaultPath, err := LoadDefaultConfig()
	if err != nil {
		return nil, fmt.Errorf("%s: %w", msg.Get("error.parse", defaultPath), err)
	}

	var cfg *config.CompareFilesConfig
	if opt.GetConfigFilePath() == "" {
		slog.Info("デフォルト起動設定: " + defaultPath)
		cfg = defaultConfig
	} else {
		slog.Info("カスタム起動設定: " + opt.GetConfigFilePath())
		cfg, err = config.ParseConfigFile(opt.GetConfigFilePath())
		if err != nil {
			return nil, fmt.Errorf("%s: %w", msg.Get("error.parse", opt.GetConfigFilePath()), err)
		}
		cfg.SetDefault(defaultConfig)
	}

	opt.OverwriteConfig(cfg)

	if err := cfg.Validate(); err != nil {
		return nil, fmt.Errorf("%s\n%w", msg.Get("error.validate"), err)
	}
	return cfg, nil
}

// LoadLayoutManager はレイアウト定義を登録済みのマネージャを返します。
// 登録順(後勝ち): 同梱デフォルト → 探索ディレクトリの compare_layout →
// overwriteLayoutDir(カンマ区切り)。
func LoadLayoutManager(cfg *config.CompareFilesConfig) (*config.LayoutManager, error) {
	manager := config.NewLayoutManager()

	// 同梱デフォルトレイアウト
	layouts := assets.DefaultLayouts()
	// ファイル名順で安定に登録
	for _, name := range sortedNames(layouts) {
		if err := manager.AddLayoutData(layouts[name], "embedded:"+name); err != nil {
			return nil, err
		}
	}

	// 探索ディレクトリ(COMPAREFILES_CLASSPATH → "." → ./config)の compare_layout
	for _, dir := range configSearchDirs() {
		layoutDir := filepath.Join(dir, config.LayoutDirName)
		if info, err := os.Stat(layoutDir); err == nil && info.IsDir() {
			if err := manager.AddLayoutDir(layoutDir); err != nil {
				return nil, err
			}
		}
	}

	// overwriteLayoutDir (カンマ区切り、後勝ち)
	if cfg.OverwriteLayoutDir != "" {
		for _, dir := range strings.Split(cfg.OverwriteLayoutDir, ",") {
			dir = strings.TrimSpace(dir)
			if dir == "" {
				continue
			}
			if err := manager.AddLayoutDir(dir); err != nil {
				return nil, err
			}
		}
	}
	return manager, nil
}

func sortedNames(m map[string][]byte) []string {
	names := make([]string, 0, len(m))
	for name := range m {
		names = append(names, name)
	}
	// レイアウト登録は同一正規表現キーの後勝ちのみが意味を持つため、単純な辞書順で十分
	for i := 1; i < len(names); i++ {
		for j := i; j > 0 && names[j] < names[j-1]; j-- {
			names[j], names[j-1] = names[j-1], names[j]
		}
	}
	return names
}

// ExitCode は処理結果のログを出力し、対応する終了コードを返します。
// Java 版 BaseCompareFiles.exitScript 相当。
func ExitCode(processStatus status.ProcessStatus) int {
	switch processStatus {
	case status.ProcessSuccess:
		slog.Info(msg.Get("exit.success"))
	case status.ProcessWarning:
		slog.Warn(msg.Get("exit.warn"))
	default:
		slog.Error(msg.Get("exit.fail"))
	}
	return processStatus.ExitCode()
}
