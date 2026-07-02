package csvio

import (
	"reflect"
	"strings"
	"testing"
)

func readAllRecords(t *testing.T, input string, config Config) ([][]string, []int) {
	t.Helper()
	r := NewReader(strings.NewReader(input), config)
	var records [][]string
	var lineNums []int
	for {
		values, err := r.ReadValues()
		if err != nil {
			t.Fatal(err)
		}
		if values == nil {
			return records, lineNums
		}
		records = append(records, values)
		lineNums = append(lineNums, r.LineNumber())
	}
}

func TestReadValuesCsv(t *testing.T) {
	input := "\"a\",\"b,c\",\"d\"\"e\"\nplain,2,3\n"
	records, lineNums := readAllRecords(t, input, CsvConfig())
	want := [][]string{{"a", "b,c", `d"e`}, {"plain", "2", "3"}}
	if !reflect.DeepEqual(records, want) {
		t.Errorf("records: %#v", records)
	}
	if !reflect.DeepEqual(lineNums, []int{1, 2}) {
		t.Errorf("lineNums: %v", lineNums)
	}
}

func TestReadValuesMultiline(t *testing.T) {
	input := "\"line1\nline2\",b\nnext,row\n"
	records, lineNums := readAllRecords(t, input, CsvConfig())
	if len(records) != 2 {
		t.Fatalf("records: %#v", records)
	}
	if records[0][0] != "line1\nline2" {
		t.Errorf("セル内改行: %q", records[0][0])
	}
	// 1 レコード目はセル内改行を含み物理 2 行を消費
	if !reflect.DeepEqual(lineNums, []int{2, 3}) {
		t.Errorf("lineNums: %v", lineNums)
	}
}

func TestReadValuesTsv(t *testing.T) {
	input := "a\t\"b\\\"c\"\t3\n"
	records, _ := readAllRecords(t, input, TsvConfig())
	want := [][]string{{"a", `b"c`, "3"}}
	if !reflect.DeepEqual(records, want) {
		t.Errorf("TSV バックスラッシュエスケープ: %#v", records)
	}
}

func TestReadValuesEmptyLineAndEOF(t *testing.T) {
	// 空行は [""] を返す (呼び出し側で EOF 扱いされる)
	records, _ := readAllRecords(t, "a,b\n\nc,d\n", CsvConfig())
	if len(records) != 3 || len(records[1]) != 1 || records[1][0] != "" {
		t.Errorf("空行: %#v", records)
	}
	// 空入力は即 nil
	records, _ = readAllRecords(t, "", CsvConfig())
	if records != nil {
		t.Errorf("空入力: %#v", records)
	}
	// 最終行に改行なし
	records, _ = readAllRecords(t, "a,b", CsvConfig())
	if len(records) != 1 || records[0][1] != "b" {
		t.Errorf("改行なし終端: %#v", records)
	}
}

func TestReadValuesCRLF(t *testing.T) {
	records, lineNums := readAllRecords(t, "a,b\r\nc,d\r\n", CsvConfig())
	want := [][]string{{"a", "b"}, {"c", "d"}}
	if !reflect.DeepEqual(records, want) {
		t.Errorf("CRLF: %#v", records)
	}
	if !reflect.DeepEqual(lineNums, []int{1, 2}) {
		t.Errorf("lineNums: %v", lineNums)
	}
}
