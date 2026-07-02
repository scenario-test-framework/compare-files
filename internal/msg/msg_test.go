package msg

import (
	"os"
	"path/filepath"
	"testing"
)

func TestGetLogMessages(t *testing.T) {
	if got := Get("log.file.compare", "a.csv", "b.csv"); got != "  ・ファイル比較 左:a.csv、右:b.csv" {
		t.Errorf("log.file.compare = %q", got)
	}
	if got := Get("log.dir.scan"); got != "・ファイル走査" {
		t.Errorf("log.dir.scan = %q", got)
	}
}

func TestParsePropertiesFileAndOverride(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "compare_files_messages.properties")
	content := "# コメント行\n" +
		"! これもコメント\n" +
		"\n" +
		"log.dir.scan=・Scanning files\n" +
		"log.file.compare=  ・Compare left:{0} right:{1}\n" +
		"log.text.sort=\\ \\ \\ \\ ・Sort\n" +
		"custom.multiline=1行目\\n2行目\\tタブ\n"
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}

	m, err := ParsePropertiesFile(path)
	if err != nil {
		t.Fatalf("ParsePropertiesFile: %v", err)
	}
	if m["log.dir.scan"] != "・Scanning files" {
		t.Errorf("log.dir.scan = %q", m["log.dir.scan"])
	}
	if m["custom.multiline"] != "1行目\n2行目\tタブ" {
		t.Errorf("custom.multiline = %q", m["custom.multiline"])
	}

	SetOverrides(m)
	t.Cleanup(func() { overrides = map[string]string{} })

	if got := Get("log.dir.scan"); got != "・Scanning files" {
		t.Errorf("上書き後の log.dir.scan = %q", got)
	}
	// 値の先頭の空白は Java properties と同様に除去される
	if got := Get("log.file.compare", "L.csv", "R.csv"); got != "・Compare left:L.csv right:R.csv" {
		t.Errorf("上書き後の log.file.compare = %q", got)
	}
	// 先頭の空白は「\ 」でエスケープすれば保持できる
	if got := Get("log.text.sort"); got != "    ・Sort" {
		t.Errorf("上書き後の log.text.sort = %q", got)
	}
	// 上書きされていないキーは既定値のまま
	if got := Get("log.dir.compare"); got != "・ディレクトリ比較" {
		t.Errorf("log.dir.compare = %q", got)
	}
}

func TestParsePropertiesFileInvalidLine(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "bad.properties")
	if err := os.WriteFile(path, []byte("キーだけで値がない\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	if _, err := ParsePropertiesFile(path); err == nil {
		t.Fatal("= のない行はエラーになるべき")
	}
}
