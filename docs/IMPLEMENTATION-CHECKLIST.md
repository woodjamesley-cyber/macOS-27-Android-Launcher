# macOS 27 Golden Gate → Android implementation checklist

Compiled **before** any code was written, from Apple’s macOS 27 pages, the macOS 27 release notes,
WWDC 2026 coverage, the Ars Technica / Six Colors / MacStories / Macworld reviews, and the
[visual reference gallery](../research/gallery.md).

Golden Gate (released 14 September 2026) is the release that:

- added a **Liquid Glass opacity slider** (ultraclear → fully tinted) in Appearance,
- **tightened and unified window corner radii** and removed the over-rounded Tahoe corners,
- replaced floating inset sidebars with **edge-to-edge sidebars and restored colored sidebar icons**,
- made **toolbars uniform** with clearer shading,
- introduced **glass traffic-light window buttons** with a hover/press bounce,
- merged **Spotlight and Siri AI** into a single “Search or Ask” surface,
- added a **menu bar overflow double-arrow** for items hidden by the notch,
- refreshed **app icons** with more Liquid Glass layering, and
- shipped new **dynamic Golden Gate wallpapers** plus a Clarus the Dogcow easter egg.

## Checklist and status in this project

| # | macOS 27 element | Expected behaviour | Status on Android |
| --- | --- | --- | --- |
| 1 | Desktop wallpaper | Full-bleed image, light/dark variants, photo support | **Done** — 7 original procedural wallpapers + Photo Picker; Apple’s actual wallpapers are not bundled |
| 2 | Desktop icons | Grid, labels, drag, context menu | **Done** — drag-to-reorder, long-press menu, add/remove, folders |
| 3 | Desktop widgets | Clock, calendar, weather, battery, notes, activity; resize, move | **Done** — small/medium/large, free positioning, long-press menu |
| 4 | Dock | Glass slab, magnification, indicators, launch, reorder | **Done** — pointer magnification, press magnification, drag reorder, menus, auto-hide |
| 5 | Dock position choice | Left / bottom / right | **Partial** — bottom edge only; Android gesture-nav makes side docks unreliable |
| 6 | Menu bar | App menus, status items, clock, battery | **Done** — real dropdowns, per-app actions, battery %, Wi-Fi, control centre |
| 7 | Menu bar overflow | Double-arrow for hidden items | **Not applicable** — no notch; menu bar truncates menus on narrow phones |
| 8 | Window chrome | 12 dp radius, shadow, traffic lights | **Done** — original glass traffic lights with hover glyphs |
| 9 | Window drag / resize / zoom | All three | **Done** — title-bar drag, corner resize, green zoom, double-click zoom |
| 10 | Minimise to Dock | Window collapses with indicator | **Done** — minimises with a Dock click to restore |
| 11 | Window shadows distinguish active app | Stronger active shadow | **Done** — 36 dp active vs 22 dp inactive |
| 12 | Liquid Glass | Refraction, blur, tint, highlights | **Done** — blurred backdrop copy + tint + rim + shadow |
| 13 | Glass opacity slider | One global control | **Done** — Appearance → Liquid Glass, 0–100% |
| 14 | Light / dark / auto | Entire UI switches | **Done** — plus accent colours |
| 15 | Colored sidebar icons | Restored colour | **Done** — Settings/Finder sidebar items use tinted icons |
| 16 | Edge-to-edge sidebars | Sidebar flush to window edge | **Done** — Settings and Finder sidebars are flush, not floating |
| 17 | Uniform toolbars | 40 dp title bar + toolbar row | **Done** |
| 18 | Finder | Sidebar, toolbar, views, search, sort, preview | **Done** — SAF-based; multiple view modes, breadcrumbs, rename/delete/new folder |
| 19 | Finder gallery/column view | Extra view modes | **Partial** — grid + list implemented; column view omitted |
| 20 | Spotlight / “Search or Ask” | Instant results, keyboard nav | **Done** — apps, built-ins, notes, files, web, ask assistant |
| 21 | Spotlight floating/repositionable | Drag to move | **Partial** — fixed centred position (animates) |
| 22 | Siri AI | Conversational assistant | **Not possible** — Apple service unavailable on Android; replaced by independent Assistant with optional cloud AI |
| 23 | Launchpad | All apps, search, folders | **Done** — adaptive grid, search, long-press folders |
| 24 | Control Center | Tiles, sliders, appearance | **Done** with Android limits — protected toggles open system panels |
| 25 | Notification centre | Date, calendar, system status | **Done** — does not read or fake Android notifications |
| 26 | System Settings | Sidebar + searchable pages | **Done** — 16 pages including privacy and accessibility |
| 27 | Dynamic wallpapers / screensavers | Time-based artwork | **Partial** — static original artwork; no animated screensaver |
| 28 | Widgets from other apps | macOS widgets | **Not possible** — Android AppWidgetHost integration not implemented; native launcher widgets instead |
| 29 | Mission Control / Spaces | Gesture window overview | **Partial** — Ctrl+Tab cycles windows; Android owns recents |
| 30 | Stage Manager-style motion | Animated window staging | **Partial** — window open/close animation only |
| 31 | Traffic-light bounce | Playful press animation | **Partial** — press scale on Dock tiles; no traffic-light bounce |
| 32 | Aero Snap / tiling | Drag to edge to tile | **Not implemented** — zoom/maximise and free resize exist |
| 33 | Quick Look preview | Space to preview | **Partial** — Finder preview pane; no full-space Quick Look overlay |
| 34 | Continuity / Handoff / AirDrop | Apple ecosystem | **Not possible** — Android platform |
| 35 | Siri voice + dictation | Voice input | **Partial** — Android speech recogniser when installed |
| 36 | Keyboard shortcuts | ⌘Space, ⌘Tab, ⌘, etc. | **Done** — mapped to Ctrl on Android keyboards |
| 37 | Right-click menus | Context menus | **Done** — window title bar and Dock/tiles |
| 38 | Hover magnification | Pointer-driven Dock | **Done** |
| 39 | Reduce Motion | Accessibility animation control | **Done** — launcher toggle + Android animator scale |
| 40 | Clarus easter egg | Dogcow on over-scroll | **Omitted** — Apple’s mascot artwork is not ours to reproduce |
| 41 | Default Home replacement | Press Home → this launcher | **Done** — HOME intent filter + RoleManager request |
| 42 | First-run setup | Choose Home role, permissions, appearance | **Done** — three-step wizard |
| 43 | Privacy controls | Optional permissions, no hidden access | **Done** — see README permission table |
| 44 | Original assets only | No Apple artwork | **Done** — all icons/wallpapers are Compose drawings |
