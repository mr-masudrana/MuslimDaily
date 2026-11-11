#!/usr/bin/env python3
"""Checks the structure of duas.json (does not check religious correctness)."""
import json, sys, re

def main(path):
    errors, warnings = [], []
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception as e:
        print(f"ERROR: cannot read JSON: {e}")
        return 1
    ids = set()
    cats = data.get("categories")
    if not isinstance(cats, list) or not cats:
        errors.append("'categories' must be a non-empty list")
        cats = []
    total = 0
    for c in cats:
        cid = c.get("id", "?")
        if not c.get("id"): errors.append("category without id")
        t = c.get("title") or {}
        if not t.get("en") or not t.get("bn"):
            errors.append(f"category {cid}: title needs en and bn")
        for it in c.get("items") or []:
            total += 1
            iid = it.get("id")
            if not iid: errors.append(f"{cid}: item without id"); continue
            if not re.fullmatch(r"[a-z0-9\-]+", iid):
                errors.append(f"{iid}: id must be lowercase letters, digits and '-'")
            if iid in ids: errors.append(f"{iid}: duplicate id")
            ids.add(iid)
            for f in ("title", "translation"):
                v = it.get(f) or {}
                if not v.get("en") or not v.get("bn"):
                    errors.append(f"{iid}: {f} needs en and bn")
            if not it.get("arabic"): errors.append(f"{iid}: arabic is empty")
            blob = json.dumps(it, ensure_ascii=False)
            if "REPLACE" in blob: errors.append(f"{iid}: still contains REPLACE placeholders")
            if not it.get("reference"): warnings.append(f"{iid}: no reference given")
            r = it.get("repeat")
            if r is not None and (not isinstance(r, int) or r < 1):
                errors.append(f"{iid}: repeat must be an integer >= 1")
    for w in warnings: print("WARN :", w)
    for e in errors: print("ERROR:", e)
    print(f"{len(cats)} categories, {total} items, {len(errors)} errors, {len(warnings)} warnings")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/dua/duas.json"))
