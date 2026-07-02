package row

import "testing"

func TestParseYAMLObjectMergeKey(t *testing.T) {
	yamlData := `
base: &base
  x: 1
  y: from-base
obj:
  <<: *base
  y: 2
`
	m, err := ParseYAMLObject(yamlData)
	if err != nil {
		t.Fatalf("ParseYAMLObject: %v", err)
	}
	obj, _ := m.GetOrNil("obj").(*OrderedMap)
	if obj == nil {
		t.Fatalf("obj = %v", m.GetOrNil("obj"))
	}
	// merge されたキーが展開され、明示キーが優先される
	if got := ToJavaString(obj.GetOrNil("x")); got != "1" {
		t.Errorf("obj.x = %q", got)
	}
	if got := ToJavaString(obj.GetOrNil("y")); got != "2" {
		t.Errorf("obj.y = %q (明示キーが優先されるべき)", got)
	}
	// << 自体はキーとして残らない
	if _, exists := obj.Get("<<"); exists {
		t.Error("<< がキーとして残っている")
	}
	// 展開後は明示定義と同じ表現になる (merge key 位置に挿入)
	if got := ToJavaString(obj); got != "{x=1, y=2}" {
		t.Errorf("obj = %q, want {x=1, y=2}", got)
	}
}

func TestParseYAMLObjectMergeKeySequence(t *testing.T) {
	yamlData := `
a: &a
  k1: from-a
  shared: from-a
b: &b
  k2: from-b
  shared: from-b
obj:
  <<: [*a, *b]
`
	m, err := ParseYAMLObject(yamlData)
	if err != nil {
		t.Fatalf("ParseYAMLObject: %v", err)
	}
	obj, _ := m.GetOrNil("obj").(*OrderedMap)
	if obj == nil {
		t.Fatalf("obj = %v", m.GetOrNil("obj"))
	}
	if got := ToJavaString(obj.GetOrNil("k1")); got != "from-a" {
		t.Errorf("obj.k1 = %q", got)
	}
	if got := ToJavaString(obj.GetOrNil("k2")); got != "from-b" {
		t.Errorf("obj.k2 = %q", got)
	}
	// シーケンスは先に書かれたマッピングが優先される (YAML merge 仕様)
	if got := ToJavaString(obj.GetOrNil("shared")); got != "from-a" {
		t.Errorf("obj.shared = %q, want from-a", got)
	}
}

func TestParseYAMLObjectQuotedMergeKeyIsLiteral(t *testing.T) {
	// 引用された "<<" は merge key ではなく通常キー
	m, err := ParseYAMLObject(`"<<": literal`)
	if err != nil {
		t.Fatalf("ParseYAMLObject: %v", err)
	}
	if got := ToJavaString(m.GetOrNil("<<")); got != "literal" {
		t.Errorf("<< = %q", got)
	}
}

func TestParseYAMLObjectAlias(t *testing.T) {
	yamlData := `
anchor: &v hello
copy: *v
`
	m, err := ParseYAMLObject(yamlData)
	if err != nil {
		t.Fatalf("ParseYAMLObject: %v", err)
	}
	if got := ToJavaString(m.GetOrNil("copy")); got != "hello" {
		t.Errorf("copy = %q", got)
	}
}
