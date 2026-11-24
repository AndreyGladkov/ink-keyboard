#!/usr/bin/env python3

import sys

# Converts rawdict_utf16_65105_freq.txt from AOSP PinyinIME
# (https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/+/refs/heads/main/jni/data/)
# into app/src/main/assets/pinyin/zh-CN.txt.
# Source line format: "<word> <frequency> <flag> <syllable> [<syllable> ...]".
# The flag is 1 for words with characters outside GB2312, which are mostly traditional
# characters, so those lines are dropped for Simplified Chinese.

HEADER = """# Simplified Chinese pinyin lexicon: syllables, word, frequency.
# Derived from rawdict_utf16_65105_freq.txt of AOSP PinyinIME by tools/make-pinyin-dict.py.
# Copyright (C) 2009 The Android Open Source Project
# SPDX-License-Identifier: Apache-2.0
"""


def read_entries(path: str) -> dict[tuple[tuple[str, ...], str], int]:
    entries: dict[tuple[tuple[str, ...], str], int] = {}
    with open(path, encoding="utf-16") as source:
        for line in source:
            fields = line.split()
            if len(fields) < 4 or fields[2] != "0":
                continue
            word = fields[0]
            syllables = tuple(fields[3:])
            frequency = max(1, round(float(fields[1])))
            key = (syllables, word)
            entries[key] = max(entries.get(key, 0), frequency)
    return entries


def main(source_path: str, target_path: str):
    entries = read_entries(source_path)
    ordered = sorted(entries.items(), key=lambda item: (item[0][0], -item[1], item[0][1]))
    with open(target_path, "w", encoding="utf-8", newline="\n") as target:
        target.write(HEADER)
        for (syllables, word), frequency in ordered:
            target.write(f"{' '.join(syllables)}\t{word}\t{frequency}\n")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: make-pinyin-dict.py <rawdict_utf16_65105_freq.txt> <zh-CN.txt>")
    main(sys.argv[1], sys.argv[2])
