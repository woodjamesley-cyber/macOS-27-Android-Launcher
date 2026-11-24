# Golden Gate Desktop

**An unofficial, native Android recreation of the macOS 27 “Golden Gate” desktop — built to replace Samsung One UI Home as your everyday launcher.**

Golden Gate Desktop is a real Android **Home** app (not a theme, mockup, icon pack or WebView). It is written in Kotlin with Jetpack Compose, draws its own icons and wallpapers, renders its own Liquid Glass surfaces with a live blurred backdrop, and hosts draggable launcher-owned windows for Finder, Notes, Calculator, Calendar, Clock, Weather and Activity Monitor.

> **Unofficial.** This project is not affiliated with, endorsed by, or connected to Apple Inc. “macOS”, “Finder”, “Safari”, “Spotlight”, “Siri” and related marks are Apple trademarks, referenced only to describe visual inspiration. No Apple code, icons, wallpapers, fonts or screenshots are included. See [LICENSE](LICENSE).

---

## What is in the box

| Area | What actually works |
| --- | --- |
| **Home launcher role** | `MAIN` + `HOME` intent filters, `RoleManager.ROLE_HOME` request flow, Samsung One UI guidance, “Set as Home” chip in the menu bar, first-run setup wizard |
| **Desktop** | Full-bleed wallpaper, desktop icons with labels and drag-to-reorder, long-press context menus, folders, free-positioned widgets (drag, resize S/M/L, remove) |
| **Dock** | Glass slab, hover magnification for mice, press magnification for touch, running indicators, tap to launch, long-press menu, drag-to-reorder, size control, auto-hide with a reveal handle, Launchpad entry |
| **Menu bar** | 30 dp translucent bar, brand menu, per-app menus (File / Edit / View / Window / Help) with real actions, battery %, Wi-Fi, Spotlight, Assistant, Control Center, clock; “Set as Home” warning chip |
| **Windows** | Drag by title bar, resize from the corner, traffic-light controls, minimise to Dock, zoom/maximise, double-click title bar to zoom, right-click menu, focus/z-order, opening fade+scale |
| **Finder** | Storage Access Framework browsing, breadcrumbs, sidebar, grid/list views, sort by name/date/size/kind, search, new folder, rename, delete, preview pane with metadata, open with any installed app |
| **Spotlight** | Centred “Search or Ask” panel, fuzzy app search, built-in apps, notes, file search in the granted folder, web search row, assistant row, arrow keys + Enter, Esc to dismiss |
| **Launchpad** | Every launchable Android app, built-in apps, folders, search, adaptive grid, long-press to pin to Dock/Desktop or start a folder, app info |
| **System Settings** | macOS-style sidebar + searchable pages: Appearance, Wallpaper, Desktop & Widgets, Dock, Launchpad, Folders, Default Home App, Assistant, Weather, Calendar, Finder, Accessibility, Privacy, Shortcuts, Advanced, About |
| **Liquid Glass** | Backdrop blur + refraction tint + edge highlight + wide shadow; one global opacity slider (ultraclear → fully tinted), blur toggle, light/dark/auto, 7 accent colours |
| **Control Center** | Glass panel with Wi-Fi/Bluetooth/Airplane/Focus tiles, brightness (app-level, plus system-wide when permitted), media volume, light/dark, rotation shortcut, honest Android limits note |
| **Notification panel** | Large clock/date, today’s calendar events, battery, storage/memory, quick links; explains that the real Android shade cannot be replaced |
| **Assistant** | On-device command engine (open apps, search, note-taking, wallpaper, time/date/battery) plus **optional** OpenAI-compatible endpoint; voice input; conversation history; speak replies; clearly labelled “not Siri” |
| **Built-in apps** | Files, Notes (persistent, pinnable, searchable), Calculator (real parser), Calendar (month + agenda + CalendarContract), Clock (world clock, stopwatch, timer), Weather (live Open-Meteo, no key), Activity Monitor (real memory/storage/battery), About/device info |
| **Customisation** | 7 original wallpapers + any photo (Photo Picker), glass opacity/blur, accent, icon sizes, dock size/magnification/auto-hide, widget set, desktop layout, reduced motion, window animations |
| **Input** | Touch, mouse hover/right-click, hardware keyboard shortcuts (`⌘/Ctrl+Space`, `⌘/Ctrl+Tab`, `⌘/Ctrl+S`, `⌘/Ctrl+,`, `Esc`, Search key) |
| **Accessibility** | Spoken labels on every control, ≥44 dp targets, scalable `sp` typography, reduced-motion setting that honours Android’s animation scale, high-contrast glass text, no infinite background animation |

A deeper breakdown, including what is deliberately *not* claimed, is in [docs/FEATURES.md](docs/FEATURES.md). The macOS 27 research checklist is in [docs/IMPLEMENTATION-CHECKLIST.md](docs/IMPLEMENTATION-CHECKLIST.md).

---

## Screenshots

The UI needs a real device or emulator to capture honestly; this repository does not ship fabricated mockups. The visual research behind it is in **[research/gallery.md](research/gallery.md)** — 40+ real macOS 27 Golden Gate screenshots with per-category recreation notes.

To capture your own: install the APK, set it as Home, then use Android’s screenshot button. Screenshots are welcome as PRs.

---

## Install

**Requirements:** Android 8.0 (API 26) or newer. Tested build targets API 35. Works on phones and tablets.

1. Download the APK:
   - **GitHub Actions → latest “Android CI” run → Artifacts → `GoldenGateDesktop-apk`** (debug-signed, installable), or
   - **Releases** page for a tagged build (signed release APK when repository signing secrets are configured).
2. On the phone, allow “Install unknown apps” for your browser or file manager.
3. Open the APK and install.

Package name: `com.goldengate.desktop` (debug builds: `com.goldengate.desktop.debug`).

## Make it your Home app (replacing Samsung One UI Home)

1. Launch Golden Gate Desktop.
2. In the setup sheet, tap **Set as default Home**.
3. Android shows a chooser — pick **Golden Gate Desktop** and **Always**. (On Android 10+ this uses `RoleManager.ROLE_HOME`.)
4. Press the Home button / Home gesture. You should return to the macOS-style desktop.

Samsung One UI alternative path:

> **Settings → Apps → Choose default apps → Home app → Golden Gate Desktop**

If the launcher was already set and you want to switch back: same screen, choose One UI Home.

> The Play Store and One UI may show a confirmation dialog the first time a new launcher takes the Home role. That is Android’s security model, not a bug.

---

## Build from source

```bash
git clone <your-repo-url>
cd GoldenGateDesktop
./gradlew :app:assembleDebug          # installable debug APK
./gradlew :app:installDebug           # install to a connected device
```

Requires **JDK 17** and Android SDK **Platform 35** + **Build-Tools 35.0.0**. Android Studio Ladybug or newer can open the project directly.

Release signing (optional, never commit these):

```bash
keytool -genkeypair -keystore signing/release.jks -alias goldengate \
  -keyalg RSA -keysize 2048 -validity 10000
cat > keystore.properties <<'EOF'
storeFile=signing/release.jks
storePassword=…
keyAlias=goldengate
keyPassword=…
EOF
./gradlew :app:assembleRelease
```

`keystore.properties`, `signing/`, `*.jks` and `local.properties` are gitignored.

CI equivalent: add repository secrets `SIGNING_STORE_FILE` (base64 of the keystore), `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Without them the workflow ships a debug-signed APK and says so.

## Project layout

```
app/src/main/java/com/goldengate/desktop/
├── MainActivity.kt          Home activity, role request, permissions, key shortcuts
├── GoldenGateApp.kt
├── core/                    Prefs, models, launcher state, app discovery, Shell bridge
├── data/                    Weather (Open-Meteo), calendar, notes, system stats, assistant engine
├── ui/theme/                Design tokens, accents, typography, glass style
├── ui/components/           Glass surfaces, icon set, controls, shortcut views
├── ui/wallpaper/            Original procedural wallpapers + photo wallpaper
├── ui/desktop/              Desktop root, menu bar, Dock, icons, widgets
├── ui/windows/              Window layer, chrome, resize, app content dispatch
├── ui/apps/                 Finder, Notes, Calculator, Calendar, Clock, Weather, Activity, Settings
├── ui/overlays/             Spotlight, Launchpad, Control Center, notifications, assistant, setup, folders
└── util/                    Intents, formatting
research/                    Gallery (committed) + copyrighted cache (gitignored)
tools/                       Reference collector + gallery generator
docs/                        Architecture, features, limitations, testing, checklist
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the state model and rendering strategy.

---

## Honest limitations

Android (and One UI) protects several things a third-party launcher **cannot** replace. This app does not pretend otherwise, does not request root, and does not use hidden APIs:

- **Notification shade, lock screen, recents, boot animation, system navigation** — still Android/One UI. The launcher provides its own date/calendar panel but does not read or fake notifications.
- **Wi-Fi / Bluetooth / Airplane / Focus toggles** — protected; tiles open the official system panels.
- **System brightness** — the slider controls the launcher window immediately; device-wide changes need the “Modify system settings” special access.
- **Full internal storage browsing** — Android scoped storage; Finder uses the system folder picker (SAF). No `MANAGE_EXTERNAL_STORAGE`.
- **Per-process CPU/RAM breakdown** — restricted; Activity Monitor shows the values a launcher may read.
- **Real macOS app widgets** — none; widgets are native Compose cards plus shortcuts to installed Android apps. `AppWidgetHost` is not implemented.
- **Siri / Apple Intelligence** — not available on Android. The Assistant is an independent feature with an optional third-party AI connection.
- **Motion / Stage Manager-style spaces** — Android has no launcher-controlled window manager; launcher windows are Compose surfaces, not system windows.

The full list, per feature, is in [docs/FEATURES.md](docs/FEATURES.md) and [docs/LIMITATIONS.md](docs/LIMITATIONS.md).

## Privacy and permissions

| Permission | Why | When asked |
| --- | --- | --- |
| `READ_CALENDAR` | Show today’s events in widgets / panels / Calendar | Only when you enable calendar access |
| `RECORD_AUDIO` | Dictate an Assistant prompt | Only when you tap the microphone |
| `WRITE_SETTINGS` | Optional device-wide brightness | Only when you tap “System” brightness |
| `INTERNET` | Weather (city name only) and an Assistant endpoint **you** configure | Implicit |
| Folder access (SAF) | Finder browsing | Only when you tap “Choose folder” |

Notes, layout and settings are stored in the app’s private `SharedPreferences`. The Assistant API key is stored there too, never in the repository, and is only sent to the endpoint you enter. There is no analytics, no telemetry and no hidden background service.

## Research and copyright

- **[research/gallery.md](research/gallery.md)** — 40+ catalogued macOS 27 references (Desktop, Dock, Finder, Menu Bar, Control Center, Liquid Glass, Widgets, Wallpapers, Applications, Siri AI, Spotlight, System Settings, Animations, Icons) with sources and recreation notes.
- **`research/references/`** — a local, **gitignored** cache of the original screenshots for offline study, regenerated with:

```bash
python3 tools/collect_references.py
python3 tools/build_gallery.py
```

Screenshots remain the property of Apple and the publications that captured them; they are not redistributed with the app or committed to this repository. The app’s icons, wallpapers and glass are original Compose artwork.

## License

MIT for the source code. See [LICENSE](LICENSE).
