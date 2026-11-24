# Research: macOS 27 Golden Gate

Everything in this folder documents the interface that Golden Gate Desktop recreates. It was
compiled **before** implementation, from macOS 27 sources rather than older macOS screenshots.

## Contents

| File | Purpose |
| --- | --- |
| [`gallery.md`](gallery.md) | The committed visual gallery: 40+ references across 14 categories, each with its original source and a concrete “what to recreate” note. Images are hotlinked, not copied. |
| `references/` | **Gitignored** local cache of the original screenshots, organised into the same 14 category folders, plus `INDEX.md` and `manifest.json`. Regenerate with the tools below. |

## Categories

Desktop · Dock · Finder · Menu Bar · Control Center · Liquid Glass · Widgets · Wallpapers ·
Applications · Siri AI · Spotlight · System Settings · Animations · Icons

## Regenerate the local cache

```bash
python3 tools/collect_references.py   # fetch article/guide pages, download images, categorise
python3 tools/build_gallery.py        # write research/gallery.md + per-category local folders
```

Requires only Python 3 (standard library). Pages that block scraping are reported and skipped; the
committed gallery never depends on the cache being present.

## What macOS 27 Golden Gate changed (research summary)

macOS 27 shipped on 14 September 2026 and is the first Apple-silicon-only macOS. Its interface
changes matter more than its feature list for this project:

- **Liquid Glass refinement.** A global opacity slider in System Settings → Appearance, from
  ultraclear to fully tinted. Better diffusion of complex backdrops, darker edges, brighter
  highlights, and stronger window shadows so the active window is obvious.
- **Uniform window corners.** One radius across windows and apps; the oversized Tahoe rounding is
  gone. Toolbars gained shading and a consistent height.
- **Sidebars reverted to edge-to-edge**, with colored sidebar icons returned and a darker column
  background for contrast.
- **New glass traffic-light buttons** with a playful press/drag bounce.
- **App icons refreshed** with more Liquid Glass layers across light, dark, tinted and clear modes.
- **Siri AI** replaced the old Siri, driven by generative models and personal context, and is
  reachable from a unified “Search or Ask” Spotlight that is narrower, darker and more rounded.
- **Menu bar overflow**: a double-arrow reveals items hidden behind the notch.
- **New dynamic Golden Gate wallpapers** (day/night bridge views) and an animated screensaver.
- Finder/Mail/Photos refinements, Game Porting Toolkit 4, parental controls, and performance work.

Sources used for this summary and the gallery include Apple’s macOS 27 product page, Apple Support
articles (upgrade notes, “What’s new in the updates”, compatibility list), Apple’s macOS 27 release
notes, the Apple Mac User Guide for macOS 27, WWDC 2026 coverage, and reviews by Ars Technica,
Six Colors, MacStories, Macworld, MacRumors, 9to5Mac, The Verge and WIRED.

## Copyright

The screenshots are Apple’s copyrighted interface captured by Apple or by reviewers. They are **not**
included in the app and **not** committed to this repository. They remain the property of their
owners and are referenced for study and commentary. The launcher’s own icons, wallpapers, glass
materials and layout are original work.
