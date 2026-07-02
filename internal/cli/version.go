package cli

// Version はビルド時に ldflags で埋め込まれるバージョン文字列です。
//
//	go build -ldflags "-X github.com/scenario-test-framework/compare-files/internal/cli.Version=v2.0.0"
var Version = "dev"
