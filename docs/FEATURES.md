# Features: what works, what is partial, what Android forbids

This file is deliberately blunt. If a feature is listed as **working**, it is implemented in the
source and compiles into the shipped APK; see [TESTING.md](TESTING.md) for what was actually
verified on hardware versus what still needs a device pass.

## Working end-to-end

### Launcher role
- Home intent filter (`MAIN` + `HOME` + `DEFAULT`) and a launch icon entry point.
- `RoleManager.ROLE_HOME` request on Android 10+, with a fallback that opens Android’s Home settings.
- Setup wizard: set Home, optional permissions, wallpaper/appearance.
- “Set as Home” warning chip in the menu bar until the role is held.
- Home button returns to the desktop once the role is granted.

### Desktop
- Wallpaper: seven original artworks plus any photo through the Android Photo Picker (copied to private storage).
- Desktop icons: labels, drag-to-reorder, long-press menu (Open, Add to Dock, Remove), folders.
- Widgets: Clock, Calendar, Weather, Battery, Notes, Activity. Drag freely, long-press for S/M/L and removal, add from Settings.
- Adaptive layout: 1–3 icon columns; tablet-friendly margins.

### Dock
- Glass slab with running indicators; pointer hover magnification and press magnification.
- Drag an icon sideways to reorder; long-press without moving for the context menu.
- Add/remove items, add any installed Android app, add folders, restore defaults.
- Auto-hide with a reveal handle; three sizes.

### Menu bar and windows
- Translucent 30 dp bar with brand menu, per-app File/Edit/View/Window/Help menus and status items.
- Battery percentage from a sticky broadcast; date/time ticking each second; optional seconds.
- Windows: drag, resize, minimise, zoom, close, focus, z-order, double-click title bar, right-click menu.
- Window state survives rotation (dp frames clamped to the new surface).

### Finder
- SAF folder grant with persisted permission; private Documents fallback.
- Breadcrumb navigation, sidebar locations, grid and list views.
- Sort by Name / Date / Size / Kind, ascending or descending.
- Search within the current folder; create folder, rename, delete.
- Preview pane (kind, size, modified, MIME) and open with any installed app.

### Spotlight
- Open from the menu bar, the search key, or Ctrl/⌘+Space.
- Searches Android apps, built-in apps, notes and files in the granted folder.
- Arrow-key selection, Enter to open, Esc to close.
- Web search row (DuckDuckGo) and “Ask the Assistant” row.

### Launchpad
- Every launchable Android app with real icons, built-in apps, folders and search.
- Long-press: keep in Dock, add to Desktop, new folder with this app, app info.

### System Settings
- Sixteen pages with sidebar search and phone chip navigation.
- Appearance: Auto/Light/Dark, Liquid Glass slider, blur toggle, seven accents, reduce motion, window animations.
- Wallpaper, Desktop & Widgets, Dock, Launchpad, Folders, Default Home, Assistant, Weather, Calendar, Finder, Accessibility, Privacy, Shortcuts, Advanced, About.
- Android-owned settings deep-link into the real system screens.

### Control Center and panels
- Connectivity tiles that open the official Android panels; Focus tile; brightness; media volume.
- Light/dark switching and a rotation shortcut.
- Date/calendar/battery/storage panel with quick links.

### Built-in apps
- **Notes** — persistent, pinned notes, search, delete, auto-save.
- **Calculator** — real recursive-descent parser with parentheses, decimals, percent, division-by-zero handling.
- **Calendar** — month grid with event dots, day agenda, new-event intent, optional CalendarContract access.
- **Clock** — world clock (7 zones), stopwatch with laps, 1/5/10/25-minute timer with tone.
- **Weather** — live Open-Meteo data, city search, °C/°F, six-day forecast, no API key or location permission.
- **Activity Monitor** — memory, storage, battery, uptime, cores, refresh rate, screen metrics.
- **About** — version, device, disclaimer, license.

### Assistant
- On-device commands: open app, search, save note, change wallpaper, time/date, battery, settings, control centre, launchpad, help.
- Optional cloud: any HTTPS OpenAI-compatible endpoint + model + key, stored privately.
- Voice input through the Android speech recogniser; optional spoken replies.
- Persistent history, clear from the panel or Settings.
- Explicitly labelled as not Siri / not Apple Intelligence.

### Appearance and accessibility
- Global glass opacity and blur affect every surface consistently.
- 7 wallpapers × light/dark, 7 accent colours, 3 icon sizes, 3 dock sizes.
- Spoken labels, ≥44 dp targets, scalable text, reduce-motion, high-contrast text over glass.

### Input
- Touch, mouse (hover magnification, right-click menus), hardware keyboard shortcuts.
- Drag-and-drop reordering in Dock, desktop and widgets.

## Partial

| Feature | What exists | What is missing |
| --- | --- | --- |
| Mission Control / Spaces | Ctrl+Tab window cycling | Animated overview of all windows; Android owns recents |
| Stage Manager motion | Window open/close animation | Lateral staging, window groups |
| Quick Look | Finder preview pane | Space-bar full-screen preview overlay |
| Finder column view | Grid + list | Column view |
| Dock side placement | Bottom Dock | Left/right Dock (gesture-nav conflicts) |
| Spotlight repositioning | Fixed centred window | Dragging the panel |
| Menu bar overflow | Menus truncate on narrow screens | Notch overflow arrow (no notch on Android) |
| Dynamic wallpaper | Static original art | Time-of-day animation |
| Multiple desktops | Single desktop | Virtual desktops |

## Cannot be done by a third-party Android launcher

- Replace the **notification shade**, **lock screen**, **recent apps**, **boot animation**, **system navigation bar** or **quick settings**.
- Toggle **Wi-Fi / Bluetooth / Airplane / Do Not Disturb / rotation lock** silently. (Android protects these; the UI opens the official panels.)
- Change **device-wide brightness** without the “Modify system settings” special access.
- Read **notification content** without notification-listener access, which this launcher deliberately does not request.
- Browse **all of internal storage** without `MANAGE_EXTERNAL_STORAGE` (a Play-restricted permission); the SAF folder picker is used instead.
- Show **other apps’ home-screen widgets** — that would require `AppWidgetHost` plus bind permissions; not implemented.
- Provide **Siri**, **Apple Intelligence**, **Handoff**, **AirDrop**, **Continuity** or **iCloud** features.
- Get **per-process CPU/RAM** breakdowns or control other apps’ windows.
- Replace the macOS **window server**; launcher windows are Compose surfaces that cannot host arbitrary Android activities inside them.
