#!/usr/bin/env python3
"""比較対象ファイルの分析 (決定論的)。

比較レイアウト決定のための「事実」を JSON で出力する。フォーマットの最終判断は行わない。

Usage: python3 analyze_files.py <path> [<path>...]
  path はファイルまたはディレクトリ (ディレクトリは再帰走査)。

出力 (stdout): {"files": [<file analysis>...]}
"""

import json
import os
import re
import struct
import sys

MAX_READ = 256 * 1024   # 先頭 256KB だけ読む
MAX_LINES = 200         # 分析対象の行数上限

TIMESTAMP_RE = re.compile(
    r"^\[?\d{2,4}[-/]?\d{2}[-/]?\d{2}([ T]\d{2}:\d{2}(:\d{2})?(\.\d+)?)?"
)

IMAGE_SIGNATURES = [
    (b"\x89PNG\r\n\x1a\n", "png"),
    (b"\xff\xd8\xff", "jpeg"),
    (b"GIF87a", "gif"),
    (b"GIF89a", "gif"),
    (b"BM", "bmp"),
]


def detect_image(head: bytes):
    for sig, fmt in IMAGE_SIGNATURES:
        if head.startswith(sig):
            return fmt
    return None


def png_size(head: bytes):
    if len(head) >= 24 and head[12:16] == b"IHDR":
        width, height = struct.unpack(">II", head[16:24])
        return width, height
    return None


def detect_encoding(data: bytes):
    """試行順に最初にデコードできた文字コード名と、デコード済み文字列を返す。"""
    if data.startswith(b"\xef\xbb\xbf"):
        return "utf8 (BOM 付き)", data[3:].decode("utf-8", errors="replace")
    for name, codec in [("utf8", "utf-8"), ("ms932", "cp932"), ("euc-jp", "euc_jp")]:
        try:
            return name, data.decode(codec)
        except UnicodeDecodeError:
            continue
    return "不明 (バイナリの可能性)", data.decode("utf-8", errors="replace")


def line_endings(data: bytes):
    crlf = data.count(b"\r\n")
    lf = data.count(b"\n") - crlf
    cr = data.count(b"\r") - crlf
    counts = {"CRLF": crlf, "LF": lf, "CR": cr}
    dominant = max(counts, key=counts.get) if any(counts.values()) else None
    return {"counts": counts, "dominant": dominant}


def split_simple(line: str, sep: str):
    """クォート対応の簡易分割 (分析用)。"""
    cells, cur, in_quotes = [], [], False
    i = 0
    while i < len(line):
        ch = line[i]
        if in_quotes:
            if ch == '"':
                if i + 1 < len(line) and line[i + 1] == '"':
                    cur.append('"')
                    i += 1
                else:
                    in_quotes = False
            else:
                cur.append(ch)
        elif ch == '"' and not cur:
            in_quotes = True
        elif ch == sep:
            cells.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
        i += 1
    cells.append("".join(cur))
    return cells


def looks_numeric(value: str):
    return bool(re.fullmatch(r"-?[\d,]+(\.\d+)?", value.strip())) and any(c.isdigit() for c in value)


def analyze_delimited(lines, sep, sep_name):
    """区切り文字ごとのカラム構造分析。"""
    rows = [split_simple(line, sep) for line in lines if line != ""]
    if not rows:
        return None
    col_counts = sorted({len(r) for r in rows})
    if col_counts == [1]:
        return None  # 区切り文字が出現しない

    result = {
        "separator": sep_name,
        "column_counts": col_counts,
        "consistent_columns": len(col_counts) == 1,
    }

    if len(rows) >= 2:
        header, data_rows = rows[0], rows[1:]
        header_non_numeric = all(not looks_numeric(c) for c in header if c != "")
        data_has_numeric = any(looks_numeric(c) for r in data_rows[:20] for c in r)
        result["header_likely"] = header_non_numeric and data_has_numeric
        result["header_row"] = header
    else:
        result["header_likely"] = False

    data_rows = rows[1:] if result.get("header_likely") else rows
    if data_rows and result["consistent_columns"]:
        ncol = col_counts[0]
        unique_cols, timestamp_cols = [], []
        for idx in range(ncol):
            values = [r[idx] for r in data_rows if idx < len(r)]
            if values and len(set(values)) == len(values):
                unique_cols.append(idx + 1)  # 1 始まり
            if values and all(TIMESTAMP_RE.match(v.strip()) for v in values if v.strip()):
                timestamp_cols.append(idx + 1)
        result["unique_value_columns_1based"] = unique_cols
        result["timestamp_like_columns_1based"] = timestamp_cols
    result["sample_rows"] = rows[: 3 + (1 if result.get("header_likely") else 0)]
    return result


def analyze_json(lines, text):
    """JSON / JsonList の判定と構造抽出。"""
    def key_paths(obj, prefix="", depth=0):
        paths = []
        if isinstance(obj, dict) and depth < 4:
            for k, v in obj.items():
                p = f"{prefix}.{k}" if prefix else k
                if isinstance(v, dict):
                    paths.extend(key_paths(v, p, depth + 1))
                elif isinstance(v, list) and v and isinstance(v[0], dict):
                    paths.extend(key_paths(v[0], p, depth + 1))
                else:
                    paths.append(p)
        return paths

    non_empty = [l for l in lines if l.strip()]
    # JsonList: 各行が独立した JSON オブジェクト
    if non_empty and all(l.lstrip().startswith("{") for l in non_empty[:20]):
        parsed = []
        for l in non_empty[:20]:
            try:
                parsed.append(json.loads(l))
            except json.JSONDecodeError:
                parsed = None
                break
        if parsed:
            return {"kind": "JsonList", "line_count_sampled": len(parsed), "key_paths": key_paths(parsed[0])}
    # Json: 全体で 1 オブジェクト
    try:
        obj = json.loads(text)
        if isinstance(obj, dict):
            return {"kind": "Json", "key_paths": key_paths(obj)}
    except json.JSONDecodeError:
        pass
    return None


def analyze_file(path):
    info = {"path": path}
    try:
        info["size_bytes"] = os.path.getsize(path)
        with open(path, "rb") as f:
            data = f.read(MAX_READ)
    except OSError as e:
        info["error"] = str(e)
        return info
    info["truncated"] = info["size_bytes"] > len(data)

    if info["size_bytes"] == 0:
        info["kind"] = "empty"
        return info

    image_format = detect_image(data)
    if image_format:
        info["kind"] = "image"
        info["image_format"] = image_format
        if image_format == "png":
            size = png_size(data)
            if size:
                info["image_size"] = {"width": size[0], "height": size[1]}
        return info

    if b"\x00" in data[:8192]:
        info["kind"] = "binary"
        return info

    info["kind"] = "text"
    encoding, text = detect_encoding(data)
    info["encoding_candidate"] = encoding
    info["line_endings"] = line_endings(data)

    lines = text.splitlines()[:MAX_LINES]
    info["sampled_lines"] = len(lines)

    json_result = analyze_json(lines, text)
    if json_result:
        info["json"] = json_result

    # XML / YAML らしさ (事実のみ。最終判断はモデル側)
    stripped = text.lstrip()
    if stripped.startswith("<?xml") or stripped.startswith("<"):
        info["xml_like"] = True
    ext = os.path.splitext(path)[1].lower()
    if ext in (".yaml", ".yml"):
        info["yaml_extension"] = True
    elif not json_result and not stripped.startswith("<"):
        key_value_lines = sum(
            1 for l in lines if re.match(r"^\s*[\w.\-]+:(\s|$)", l) or l.strip().startswith("- ")
        )
        non_empty_count = len([l for l in lines if l.strip() and not l.strip().startswith("#")])
        if non_empty_count and key_value_lines / non_empty_count > 0.7:
            info["yaml_like"] = True

    delimited = {}
    for sep, name in [(",", "comma"), ("\t", "tab")]:
        result = analyze_delimited(lines, sep, name)
        if result:
            delimited[name] = result
    if delimited:
        info["delimited"] = delimited

    # 固定長らしさ: 行のバイト長の分布 (デコード前のバイト基準)
    raw_lines = [l for l in data.split(b"\n") if l.strip(b"\r")]
    lengths = sorted({len(l.rstrip(b"\r")) for l in raw_lines[:MAX_LINES]})
    info["line_byte_lengths"] = lengths[:10]
    info["uniform_line_length"] = len(lengths) == 1

    return info


def collect_paths(args):
    paths = []
    for arg in args:
        if os.path.isdir(arg):
            for root, _, files in os.walk(arg):
                for name in sorted(files):
                    paths.append(os.path.join(root, name))
        else:
            paths.append(arg)
    return paths


def main():
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    files = [analyze_file(p) for p in collect_paths(sys.argv[1:])]
    print(json.dumps({"files": files}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
