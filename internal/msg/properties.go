package msg

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

// ParsePropertiesFile はメッセージ上書き用の properties ファイル (UTF-8) をパースします。
// 形式: 1 行 1 定義の「キー=値」。空行と # / ! 始まりのコメント行は無視します。
// 値のエスケープは \n \t \\ をサポートします (Java properties のサブセット)。
func ParsePropertiesFile(path string) (map[string]string, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", path, err)
	}
	defer f.Close()

	result := map[string]string{}
	scanner := bufio.NewScanner(f)
	lineNum := 0
	for scanner.Scan() {
		lineNum++
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") || strings.HasPrefix(line, "!") {
			continue
		}
		eq := strings.Index(line, "=")
		if eq <= 0 {
			return nil, fmt.Errorf("メッセージ定義をパースできません。対象:%s#%d: %q", path, lineNum, line)
		}
		key := strings.TrimSpace(line[:eq])
		value := unescapeProperties(strings.TrimLeft(line[eq+1:], " \t"))
		result[key] = value
	}
	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", path, err)
	}
	return result, nil
}

func unescapeProperties(s string) string {
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		c := s[i]
		if c != '\\' || i+1 >= len(s) {
			b.WriteByte(c)
			continue
		}
		i++
		switch s[i] {
		case 'n':
			b.WriteByte('\n')
		case 't':
			b.WriteByte('\t')
		case '\\':
			b.WriteByte('\\')
		default:
			b.WriteByte('\\')
			b.WriteByte(s[i])
		}
	}
	return b.String()
}
