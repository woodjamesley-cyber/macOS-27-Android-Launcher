#!/usr/bin/env python3
"""Collect macOS 27 Golden Gate visual references for study.

Downloads images into research/references/ (gitignored) and writes a manifest with the
original page, image URL and category. Nothing is redistributed with the app; the committed
gallery links to the originals instead.
"""

import hashlib
import json
import os
import re
import urllib.request
from html import unescape
from urllib.parse import urljoin, urlparse

OUT = "research/references"
RAW = os.path.join(OUT, "raw")
PAGES = [
    "https://www.apple.com/os/macos/",
    "https://support.apple.com/en-us/127455",
    "https://support.apple.com/en-us/127257",
    "https://www.macrumors.com/roundup/macos-27/",
    "https://www.macrumors.com/2026/09/17/new-features-changes-macos-27/",
    "https://9to5mac.com/2026/09/14/macos-27-golden-gate-now-available-here-is-everything-new/",
    "https://arstechnica.com/gadgets/2026/09/macos-27-golden-gate-the-ars-technica-review/",
    "https://sixcolors.com/post/2026/09/macos-27-golden-gate-review-bridging-the-tahoe-gap/",
    "https://www.theverge.com/tech/943695/apple-wwdc-2026-macos-27-macbook-mac-announcement-features",
    "https://www.wired.com/story/macos-27-golden-gate-top-features",
    "https://www.macworld.com/article/3139330",
    "https://512pixels.net/projects/aqua-screenshot-library/macos-27-golden-gate/",
    "https://arstechnica.com/gadgets/2026/06/five-things-i-like-and-one-thing-i-still-want-in-the-macos-27-golden-gate-beta/",
    "https://www.theverge.com/tech/946567/apple-macos-27-golden-gate-dev-beta-hands-on",
    "https://www.theverge.com/tech/964701/apple-macos-27-golden-gate-public-beta-impressions-liquid-glass-siri-ai",
    "https://www.macworld.com/article/3161244/macos-golden-gate-in-pictures-5-design-upgrades-coming-to-your-mac.html",
    "https://www.macstories.net/stories/macos-27-the-macstories-review/3/",
    "https://www.macstories.net/stories/macos-27-the-macstories-review/",
    "https://9to5mac.com/2026/07/14/macos-27-golden-gate-liquid-glass-slider/",
    "https://www.idownloadblog.com/2026/09/14/macos-27-golden-gate-features/",
    # Official Apple Mac User Guide pages (27 = the macOS 27 edition).
    "https://support.apple.com/guide/mac-help/open-apps-from-the-dock-mh35859/27/mac/27",
    "https://support.apple.com/guide/mac-help/quickly-change-settings-mchl50f94f8f/27/mac/27",
    "https://support.apple.com/guide/mac-help/whats-in-the-menu-bar-mchlp1446/27/mac/27",
    "https://support.apple.com/guide/mac-help/work-on-the-desktop-mh40612/mac",
    "https://support.apple.com/guide/mac-help/organize-your-files-in-the-finder-mchlp2605/mac",
    "https://support.apple.com/guide/mac-help/find-what-you-need-with-spotlight-mchlp1008/mac",
    "https://support.apple.com/guide/mac-help/desktop-menu-bar-and-dock-mchlws12345m2/mac",
    "https://support.apple.com/guide/mac-help/change-desktop-dock-settings-mchlp1119/27/mac/27",
    "https://support.apple.com/guide/mac-help/change-the-appearance-mchlp1225/mac",
]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
    "Accept-Language": "en-US,en;q=0.9",
}

BAD_TOKENS = (
    "logo", "favicon", "sprite", "avatar", "author", "headshot", "icon-", "sprite",
    "advert", "banner-ad", "spinner", "pixel", "1x1", "share-", "social", "newsletter",
)

CATEGORY_RULES = [
    ("Dock", ("dock", "magnification", "open-apps-from-the-dock")),
    ("Control Center", ("control-center", "controlcenter", "control_center", "control center")),
    ("Finder", ("finder", "file-manager", "sidebar", "column-view", "stacks", "preview-pane")),
    ("Spotlight", ("spotlight", "search-or-ask", "ask-siri-or-search")),
    ("Siri AI", ("siri", "apple-intelligence", "apple intelligence")),
    ("Menu Bar", ("menu-bar", "menubar", "menu bar", "menu-extra", "notch", "overflow", "whats-in-the-menu-bar")),
    ("System Settings", ("system-settings", "system settings", "settings", "preferences", "appearance", "liquid-glass-slider")),
    ("Widgets", ("widget",)),
    ("Wallpapers", ("wallpaper", "desktop-picture", "golden-gate-wallpaper", "screensaver")),
    ("Liquid Glass", ("liquid-glass", "liquidglass", "liquid glass", "translucen", "refraction")),
    ("Animations", ("animation", "transition", "motion", "mission-control", "stage-manager", "genie")),
    ("Icons", ("icon", "traffic-light", "trafficlight", "traffic lights")),
    ("Applications", ("safari", "notes", "mail", "photos", "messages", "calculator", "calendar", "maps", "music", "podcasts", "freeform", "terminal")),
    ("Desktop", ("desktop", "window", "hero", "macbook", "ss-", "screenshot")),
]

EXCLUDE_TOKENS = ("ios-27", "iphone-17", "iphone-18", "ipados", "watchos", "tvos", "visionos", "apple-watch", "airpods")


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", "ignore")


def category_for(url: str, alt: str, page: str = "") -> str:
    # Apple's Mac User Guide uses opaque asset URLs, so classify by the guide page slug.
    if "support.apple.com/guide" in page:
        slug = page.lower()
        for marker, name in (
            ("dock", "Dock"),
            ("control", "Control Center"),
            ("menu-bar", "Menu Bar"),
            ("finder", "Finder"),
            ("spotlight", "Spotlight"),
            ("appearance", "System Settings"),
            ("desktop", "Desktop"),
        ):
            if marker in slug:
                return name
    haystack = (url + " " + alt).lower()
    for name, tokens in CATEGORY_RULES:
        if any(token in haystack for token in tokens):
            return name
    return "Desktop"


def main() -> None:
    os.makedirs(RAW, exist_ok=True)
    found = []
    seen = set()

    for page in PAGES:
        try:
            html = fetch(page)
        except Exception as error:  # noqa: BLE001
            print(f"page failed: {page}: {error}")
            continue
        candidates = []
        for match in re.finditer(
            r'<meta[^>]+(?:property|name)=["\']og:image["\'][^>]+content=["\']([^"\']+)',
            html,
            re.I,
        ):
            candidates.append((match.group(1), ""))
        for match in re.finditer(r"<img\b[^>]*>", html, re.I):
            tag = match.group(0)
            src = re.search(r'(?:data-src|data-lazy-src|data-original|src)=["\']([^"\']+)', tag, re.I)
            alt = re.search(r'alt=["\']([^"\']*)', tag, re.I)
            if not src:
                continue
            candidates.append((unescape(src.group(1)), unescape(alt.group(1)) if alt else ""))
            srcset = re.search(r'srcset=["\']([^"\']+)', tag, re.I)
            if srcset:
                first = srcset.group(1).split(",")[0].strip().split(" ")[0]
                if first:
                    candidates.append((unescape(first), unescape(alt.group(1)) if alt else ""))

        for raw_url, alt in candidates:
            url = urljoin(page, raw_url.strip())
            parsed = urlparse(url)
            path = parsed.path.lower()
            allowed_asset = "ipcdn-web.apple.com" in parsed.netloc or (
                "apple.com" in parsed.netloc and "/assets/" in path
            )
            if not path.endswith((".jpg", ".jpeg", ".png", ".webp")) and not allowed_asset:
                continue
            if any(token in path for token in BAD_TOKENS):
                continue
            if any(token in path for token in EXCLUDE_TOKENS):
                continue
            if "mirroring" not in path and "iphone" in path:
                continue
            key = parsed.path
            if key in seen:
                continue
            seen.add(key)
            found.append({"url": url, "page": page, "alt": alt, "category": category_for(url, alt, page)})

    downloaded = []
    # Prefer full-resolution assets over CDN thumbnails of the same shot.
    found.sort(key=lambda item: ("lossy" in item["url"], "/t/" in item["url"], len(item["url"])))
    for index, item in enumerate(found[:200]):
        try:
            request = urllib.request.Request(item["url"], headers=HEADERS)
            with urllib.request.urlopen(request, timeout=35) as response:
                data = response.read()
            if len(data) < 12_000:
                continue
            content_type = (response.headers.get("Content-Type") or "").split(";")[0].strip().lower()
            extension = {
                "image/png": ".png",
                "image/jpeg": ".jpg",
                "image/webp": ".webp",
                "image/gif": ".gif",
            }.get(content_type) or os.path.splitext(urlparse(item["url"]).path)[1].lower() or ".jpg"
            if extension not in (".png", ".jpg", ".jpeg", ".webp"):
                continue
            digest = hashlib.sha1(item["url"].encode()).hexdigest()[:12]
            filename = f"{index:03d}-{digest}{extension}"
            with open(os.path.join(RAW, filename), "wb") as handle:
                handle.write(data)
            item["file"] = f"raw/{filename}"
            item["bytes"] = len(data)
            downloaded.append(item)
        except Exception as error:  # noqa: BLE001
            print(f"image failed: {item['url']}: {error}")

    with open(os.path.join(OUT, "manifest.json"), "w") as handle:
        json.dump(downloaded, handle, indent=2)

    counts = {}
    for item in downloaded:
        counts[item["category"]] = counts.get(item["category"], 0) + 1
    print(f"downloaded {len(downloaded)} images from {len(PAGES)} pages")
    print(json.dumps(counts, indent=2))
    for item in downloaded:
        print(f'{item["category"]:16s} {item["url"]}')


if __name__ == "__main__":
    main()
