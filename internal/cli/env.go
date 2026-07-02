package cli

import (
	"log/slog"
	"os"
	"strings"
)

// EnvOpt は共通起動パラメータを設定する環境変数名です。
const EnvOpt = "COMPAREFILES_OPT"

// EnvJavaOpt は Java 版互換の共通起動パラメータ環境変数名です。
// JVM 専用フラグ (-X*, -D*) は警告を出して無視します。
const EnvJavaOpt = "COMPAREFILES_JAVA_OPT"

// EnvArgs は環境変数から共通起動パラメータを取得します。
// コマンドライン引数の前に連結して利用します。
func EnvArgs() []string {
	var args []string
	if v := os.Getenv(EnvOpt); v != "" {
		args = append(args, strings.Fields(v)...)
	}
	if v := os.Getenv(EnvJavaOpt); v != "" {
		for _, arg := range strings.Fields(v) {
			if strings.HasPrefix(arg, "-X") || strings.HasPrefix(arg, "-D") {
				slog.Warn("COMPAREFILES_JAVA_OPT の JVM 専用フラグを無視します。", "flag", arg)
				continue
			}
			args = append(args, arg)
		}
	}
	return args
}
