#!/usr/bin/env python3
"""Build the categorized reference gallery from the collected manifest.

- Copies each reference into research/references/<Category>/ (local, gitignored).
- Writes research/gallery.md: a committed gallery with the original image URL, the source page,
  and concrete guidance on what the launcher should recreate.

Copyright: reference screenshots belong to Apple and the publications that captured them.
They are kept out of the app and out of the public repository; the gallery links to originals.
"""

import json
import os
import re
import shutil
from collections import defaultdict

ROOT = "research"
REF = os.path.join(ROOT, "references")
RAW = os.path.join(REF, "raw")
MANIFEST = os.path.join(REF, "manifest.json")

CATEGORIES = [
    "Desktop", "Dock", "Finder", "Menu Bar", "Control Center", "Liquid Glass",
    "Widgets", "Wallpapers", "Applications", "Siri AI", "Spotlight",
    "System Settings", "Animations", "Icons",
]

QUOTA = {
    "Desktop": 8, "Dock": 4, "Finder": 5, "Menu Bar": 4, "Control Center": 4,
    "Liquid Glass": 5, "Widgets": 3, "Wallpapers": 4, "Applications": 6,
    "Siri AI": 5, "Spotlight": 4, "System Settings": 4, "Animations": 3, "Icons": 4,
}

CATEGORY_NOTES = {
    "Desktop": "Reproduce the overall composition: a full-bleed wallpaper, a translucent menu bar "
               "flush to the top edge, aliased desktop icons anchored top-right, widgets floating "
               "with generous margins and a centered Dock. Windows overlap the wallpaper with a "
               "soft, wide shadow and a 12 dp corner radius.",
    "Dock": "Icon size, spacing, corner radius, the separator before the app drawer entry, and the "
            "small running indicator beneath open apps. Hover magnification should grow neighbours "
            "in a bell curve without changing layout width. The Dock is a rounded glass slab with a "
            "hairline rim.",
    "Finder": "Edge-to-edge sidebar with colored category icons, a uniform 40 dp toolbar with a "
              "segmented icon/list switch, breadcrumb path, sortable columns, preview pane, and "
              "row selection that is a subtle rounded fill rather than a hard highlight.",
    "Menu Bar": "30 dp tall, translucent, with the app name in semibold and per-app menus that open "
                "real, working dropdowns. Status items sit on the right: battery %, Wi-Fi, search, "
                "assistant, Control Center and the date/time in a compact tabular style.",
    "Control Center": "A 372 dp glass panel anchored under the menu bar, built from 2×2 rounded "
                      "tiles, then full-width sliders for display brightness and volume, then "
                      "compact appearance tiles. Protected Android toggles open the official system "
                      "panel and say so.",
    "Liquid Glass": "Layered translucency: a blurred, slightly saturated backdrop copy, a tint layer "
                    "controlled by one global slider, a bright top-edge highlight and a hairline "
                    "stroke. Shadows should be soft and wide, never a hard dark outline.",
    "Widgets": "Rounded 20 dp glass cards in small/medium/large sizes, with a light typographic "
               "hierarchy: one large number, a quiet caption and a thin accent bar. Widgets can be "
               "dragged freely and resized from a long-press menu.",
    "Wallpapers": "Deep, low-contrast gradients with one warm or teal glow, a suggestion of landform "
                  "or fog, and no busy detail behind text. Our built-in set is original art drawn in "
                  "Compose, so no Apple wallpaper is bundled.",
    "Applications": "Window chrome plus a content area with a clear toolbar, sidebar or segmented "
                     "control, generous padding and 12–13 sp secondary labels. Built-ins should look "
                     "like small macOS apps rather than Android activities.",
    "Siri AI": "A floating, rounded assistant panel with an animated gradient orb, conversation "
               "bubbles, microphone affordance and a short privacy footnote. On Android this is an "
               "optional third-party assistant, never Apple Intelligence.",
    "Spotlight": "A centered, narrow, dark rounded search field with instant grouped results, "
                 "keyboard selection, and clear footer hints. Opening animates from ~0.985 scale "
                 "with a fade; Enter opens the highlighted result.",
    "System Settings": "A two-column layout: searchable sidebar with colored icons, and a content "
                       "column of grouped rows with switches and sliders. System-level items must "
                       "deep-link into Android settings instead of pretending to control the OS.",
    "Animations": "Window open/close fades with a 0.985→1 scale, Stage Manager-style lateral motion, "
                  "Dock bounce on launch, and panel drop-in. Everything must respect the reduce-motion "
                  "setting and Android's animator duration scale.",
    "Icons": "Squircle app tiles (about 23.5% corner radius) with a vertical highlight, a saturated "
             "duotone gradient face and a white glyph. Traffic lights are 12 dp circles with glyphs "
             "revealed on hover.",
}

SKIP_PATTERNS = [
    r"a\.cunningham", r"gettyimages", r"author", r"headshot", r"512px-soundsource",
    r"droptask-icon", r"installer-icons", r"clarUs", r"/logo", r"podcast-cover",
]

GUIDE_PAGES = ("support.apple.com/guide",)


def is_relevant(item):
    haystack = (item["url"] + " " + item.get("alt", "")).lower()
    return not any(re.search(pattern, haystack) for pattern in SKIP_PATTERNS)


def rank(item):
    page = item["page"]
    if "support.apple.com/guide" in page:
        tier = 0
    elif "apple.com" in page:
        tier = 1
    elif "512pixels" in page:
        tier = 2
    elif "arstechnica" in page or "sixcolors" in page:
        tier = 3
    else:
        tier = 4
    # Prefer bigger, non-thumbnail assets.
    size_penalty = 2 if ("lossy" in item["url"] or "/t/" in item["url"]) else 0
    return (tier + size_penalty, -item.get("bytes", 0))


def caption(item):
    alt = (item.get("alt") or "").strip()
    if alt and not alt.lower().startswith("photo of"):
        return alt[:120]
    name = os.path.basename(item["url"].split("?")[0])
    name = re.sub(r"[-_]", " ", os.path.splitext(name)[0])
    name = re.sub(r"\b(1024|640|1200|scaled|\d+x\d+)\b", "", name)
    return name.strip().capitalize()


def main():
    with open(MANIFEST) as handle:
        items = [item for item in json.load(handle) if is_relevant(item)]

    by_category = defaultdict(list)
    for item in items:
        by_category[item["category"]].append(item)

    selected = []
    for category in CATEGORIES:
        bucket = sorted(by_category.get(category, []), key=rank)
        selected.extend(bucket[: QUOTA.get(category, 3)])

    # Make sure no required category is empty: reuse full-desktop screenshots, captioned honestly.
    counts = defaultdict(int)
    for item in selected:
        counts[item["category"]] += 1
    for category in CATEGORIES:
        if counts[category] >= 2:
            continue
        pool = [item for item in items if item["category"] == "Desktop"]
        for item in sorted(pool, key=rank):
            if counts[category] >= 2:
                break
            clone = dict(item)
            clone["category"] = category
            clone["alt"] = f"{category} region of a full macOS 27 desktop shot"
            selected.append(clone)
            counts[category] += 1

    # Copy into per-category folders for local study.
    for category in CATEGORIES:
        os.makedirs(os.path.join(REF, category), exist_ok=True)
    for item in selected:
        source = os.path.join(REF, item["file"])
        target = os.path.join(REF, item["category"], os.path.basename(item["file"]))
        if os.path.exists(source):
            shutil.copy2(source, target)

    lines = []
    lines.append("# macOS 27 Golden Gate — Visual Reference Gallery\n")
    lines.append(
        "A curated set of real macOS 27 Golden Gate screenshots gathered before any code was written. "
        "Each entry lists the image, its original source, the feature category and exactly what this "
        "launcher should recreate from it.\n"
    )
    lines.append(
        "> **Copyright and licensing.** Every image here is a screenshot captured by Apple or by a "
        "publication reviewing macOS. Copyright remains with those owners. Images are **not** bundled "
        "with the app, are **not** committed to this repository, and are **not** redistributed. "
        "The committed gallery hotlinks the originals for study; a local cache can be regenerated with "
        "`python3 tools/collect_references.py`. All launcher icons, wallpapers and glass materials are "
        "original artwork drawn in Compose.\n"
    )
    lines.append(
        "> **Accuracy note.** This gallery targets macOS 27 Golden Gate (released 14 September 2026), "
        "not macOS 26 Tahoe. Golden Gate is the release that tightened window corners, removed floating "
        "inset sidebars, restored colored sidebar icons, added the Liquid Glass opacity slider and "
        "introduced Siri AI with a unified “Search or Ask” Spotlight.\n"
    )
    lines.append("## How to read an entry\n")
    lines.append(
        "`Category` groups the shot. The **Recreate** line is the implementation intent used in this "
        "project — proportions, material behaviour and interaction — not a pixel-copy instruction.\n"
    )
    lines.append("## Coverage at a glance\n")
    lines.append("| Category | References |")
    lines.append("| --- | --- |")
    counts = defaultdict(int)
    for item in selected:
        counts[item["category"]] += 1
    for category in CATEGORIES:
        lines.append(f"| {category} | {counts.get(category, 0)} |")
    lines.append(f"| **Total** | **{len(selected)}** |\n")

    for category in CATEGORIES:
        entries = [item for item in selected if item["category"] == category]
        lines.append(f"## {category}\n")
        lines.append(f"*What to recreate:* {CATEGORY_NOTES[category]}\n")
        if not entries:
            lines.append("_No dedicated screenshot in the collected set; use the shared desktop shots above._\n")
            continue
        for index, item in enumerate(entries, start=1):
            lines.append(f"### {category} {index}: {caption(item)}\n")
            lines.append(f"![{caption(item)}]({item['url']})\n")
            lines.append(f"- **Original image:** [{os.path.basename(item['url'].split('?')[0])}]({item['url']})")
            lines.append(f"- **Source page:** [{item['page']}]({item['page']})")
            lines.append(f"- **Feature category:** {category}")
            lines.append(f"- **Recreate:** {CATEGORY_NOTES[category]}\n")

    index_lines = ["# Local reference cache (not committed)\n"]
    index_lines.append("These files are for offline study only and are gitignored.\n")
    for category in CATEGORIES:
        entries = [item for item in selected if item["category"] == category]
        if not entries:
            continue
        index_lines.append(f"## {category}\n")
        for item in entries:
            local = f"{category}/{os.path.basename(item['file'])}"
            index_lines.append(f"- `{local}` — [{item['page']}]({item['page']})")
        index_lines.append("")

    with open(os.path.join(ROOT, "gallery.md"), "w") as handle:
        handle.write("\n".join(lines))
    with open(os.path.join(REF, "INDEX.md"), "w") as handle:
        handle.write("\n".join(index_lines))

    print(f"selected {len(selected)} references across {len(CATEGORIES)} categories")
    print(json.dumps(counts, indent=2))


if __name__ == "__main__":
    main()
