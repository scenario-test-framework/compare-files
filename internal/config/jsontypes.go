package config

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
)

// JBool は Jackson 1.x 互換の寛容な bool です。
// JSON の true/false に加えて "true"/"false" (大文字小文字非区別) の文字列を受け付けます。
// 既定の compare_files.json が "deleteWorkDir": "true" のように文字列で保持しているため必須です。
type JBool bool

func (b *JBool) UnmarshalJSON(data []byte) error {
	data = bytes.TrimSpace(data)
	if len(data) > 0 && data[0] == '"' {
		var s string
		if err := json.Unmarshal(data, &s); err != nil {
			return err
		}
		switch strings.ToLower(strings.TrimSpace(s)) {
		case "true":
			*b = true
		case "false", "":
			*b = false
		default:
			return fmt.Errorf("bool に変換できません: %q", s)
		}
		return nil
	}
	var v bool
	if err := json.Unmarshal(data, &v); err != nil {
		return err
	}
	*b = JBool(v)
	return nil
}

// JInt は Jackson 1.x 互換の寛容な int です。数値と数値文字列を受け付けます。
type JInt int

func (i *JInt) UnmarshalJSON(data []byte) error {
	data = bytes.TrimSpace(data)
	if len(data) > 0 && data[0] == '"' {
		var s string
		if err := json.Unmarshal(data, &s); err != nil {
			return err
		}
		v, err := strconv.Atoi(strings.TrimSpace(s))
		if err != nil {
			return fmt.Errorf("int に変換できません: %q", s)
		}
		*i = JInt(v)
		return nil
	}
	var v int
	if err := json.Unmarshal(data, &v); err != nil {
		return err
	}
	*i = JInt(v)
	return nil
}
