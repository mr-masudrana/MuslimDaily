#!/usr/bin/env python3
"""Checks the structure of practices.json."""
import json, sys, re

def main(path):
    errors = []
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception as e:
        print(f"ERROR: cannot read JSON: {e}"); return 1
    items = data.get("practices")
    if not isinstance(items, list): print("ERROR: 'practices' must be a list"); return 1
    ids = set()
    for p in items:
        pid = p.get("id")
        if not pid: errors.append("practice without id"); continue
        if pid in ids: errors.append(f"{pid}: duplicate id")
        ids.add(pid)
        t = p.get("title") or {}
        if not t.get("en") or not t.get("bn"): errors.append(f"{pid}: title needs en and bn")
        if "REPLACE" in json.dumps(p, ensure_ascii=False): errors.append(f"{pid}: still contains REPLACE placeholders")
        tm = p.get("time")
        if tm is not None and not re.fullmatch(r"([01]\d|2[0-3]):[0-5]\d", tm): errors.append(f"{pid}: time must be HH:mm")
        for key, lo, hi in (("weekdays",1,7),("hijriDays",1,30),("hijriMonths",1,12)):
            v = p.get(key)
            if v is not None and (not isinstance(v, list) or not all(isinstance(x,int) and lo<=x<=hi for x in v)):
                errors.append(f"{pid}: {key} must be a list of integers {lo}..{hi}")
        s = p.get("surah")
        if s is not None and (not isinstance(s,int) or not 1<=s<=114): errors.append(f"{pid}: surah must be 1..114")
    for e in errors: print("ERROR:", e)
    print(f"{len(items)} practices, {len(errors)} errors")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/agenda/practices.json"))
