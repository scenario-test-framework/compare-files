#!/usr/bin/env python3
"""利用者向けドキュメント docs/compare_layout.md を生成する。

正本はこのスキルの references/ 配下。references を変更したら本スクリプトで再生成する。

Usage: python3 generate_docs.py [--check]
  --check: 生成結果が既存の docs/compare_layout.md と一致するか確認する (CI 用)。
           一致しなければ exit 1。
"""

import os
import re
import sys

SKILL_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO_ROOT = os.path.abspath(os.path.join(SKILL_DIR, "..", "..", ".."))
OUTPUT = os.path.join(REPO_ROOT, "docs", "compare_layout.md")

# 掲載順 (references のファイル名)
SECTIONS = [
    "layout-basics.md",
    "format-csv-tsv.md",
    "format-fixed.md",
    "format-json.md",
    "format-yaml.md",
    "format-xml.md",
    "format-image.md",
    "criteria.md",
]

HEADER = """<!--
このファイルは自動生成です。直接編集しないでください。
正本: .claude/skills/compare-layout/references/
再生成: python3 .claude/skills/compare-layout/scripts/generate_docs.py
-->

# 比較レイアウトリファレンス

compare-files の比較レイアウト定義 (JSON) の仕様です。
レイアウトの作成は Claude Code の `compare-layout` スキル
(`.claude/skills/compare-layout/`) で支援できます。

"""


def demote_headings(markdown: str) -> str:
    """見出しレベルを 1 段下げる (# → ##)。コードブロック内は対象外。"""
    out, in_code = [], False
    for line in markdown.splitlines():
        if line.lstrip().startswith("```"):
            in_code = not in_code
        if not in_code and re.match(r"^#{1,5} ", line):
            line = "#" + line
        out.append(line)
    return "\n".join(out)


def section_title(markdown: str) -> str:
    for line in markdown.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return ""


def generate() -> str:
    parts, toc = [], []
    for name in SECTIONS:
        path = os.path.join(SKILL_DIR, "references", name)
        with open(path, encoding="utf-8") as f:
            content = f.read().strip()
        title = section_title(content)
        # GitHub 互換アンカー: 英数字・CJK・ハイフン・空白以外を除去し、空白をハイフンに
        anchor = re.sub(r"[^\w\- ぁ-んァ-ヶ一-龠]", "", title).replace(" ", "-").lower()
        toc.append(f"- [{title}](#{anchor})")
        parts.append(demote_headings(content))
    return HEADER + "\n".join(toc) + "\n\n---\n\n" + "\n\n---\n\n".join(parts) + "\n"


def main():
    generated = generate()
    if "--check" in sys.argv:
        try:
            with open(OUTPUT, encoding="utf-8") as f:
                current = f.read()
        except FileNotFoundError:
            current = ""
        if current != generated:
            print(f"NG: {OUTPUT} が references と一致しません。generate_docs.py で再生成してください。", file=sys.stderr)
            sys.exit(1)
        print(f"OK: {OUTPUT} は最新です。")
        return
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        f.write(generated)
    print(f"generated: {OUTPUT}")


if __name__ == "__main__":
    main()
