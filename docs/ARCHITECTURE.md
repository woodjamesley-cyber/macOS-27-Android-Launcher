# Architecture

Golden Gate Desktop is a single-activity Compose application that acts as an Android **Home** app. It has no database, no WebView, no background service and no third-party UI kit.

## Layers

```
MainActivity (HOME intent, role request, permissions, hardware keys)
   └── DesktopRoot (Compose tree)
         ├── WallpaperArt            original procedural / photo wallpaper
         ├── MenuBar                brand + per-app menus, status items, clock
         ├── DesktopSurface         desktop icons, widgets
         ├── WindowLayer            launcher-owned windows (Compose)
         ├── DockBar                magnification, drag reorder, menus
         └── Overlays               Spotlight, Launchpad, Control Center,
                                    notifications, Assistant, folders, setup
```

`MainActivity` implements the `Shell` interface, which is provided through `LocalShell`. Composables never reach for the Activity directly; anything requiring an Activity (photo picker, SAF tree, speech recogniser, `RoleManager`, brightness) goes through that bridge. This keeps the UI deterministic and makes a future multi-activity or preview build straightforward.

## State

- **`Prefs`** — one observable settings object backed by `SharedPreferences`. Every property is Compose `mutableStateOf` and every setter writes through. Complex values (Dock items, desktop items, folders, widgets, notes, assistant history) are JSON in the same store.
- **`LauncherState`** — session state: installed apps, open windows, z-order, active window, current overlay, running indicators, battery level, clock, messages, and the command bus.
- **`AppCommand`** — a tiny command bus (`state.sendCommand(app, "new_folder")`). Menu bar entries, widgets and the assistant use it to ask a specific built-in app to perform an action (new note, new folder, refresh, today, new event) without holding references to that app’s UI.

Windows are immutable `AppWindow` values in a `List` inside `LauncherState`. Drag and resize recompute frames in dp:

```kotlin
data class AppWindow(
    val key: String,
    val app: BuiltinApp,
    val frame: FrameDp,
    val restoreFrame: FrameDp,
    val maximized: Boolean,
    val minimized: Boolean,
    val z: Int
)
```

Frames are clamped to the desktop surface, so a window cannot be dragged fully off-screen and survives rotation because positions are density-independent.

## Liquid Glass rendering

`GlassPanel` is the single material primitive:

1. It measures its own position with `onGloballyPositioned`.
2. It draws `WallpaperArt` again at `-position`, sized to the root window, clipped to the panel.
3. That backdrop copy is blurred with `Modifier.blur` (RenderEffect on API 31+).
4. A tint brush derived from the global glass-slider value is overlaid.
5. A top-highlight gradient and 0.8 dp hairline border finish the rim, with a wide soft shadow beneath.

Because every surface samples the same wallpaper and the same `GlassStyle`, changing the opacity slider or the wallpaper instantly updates the Dock, menu bar, every window, Control Center, Spotlight and Setup with no per-component work. On devices that cannot do RenderEffect the tint still provides a legible frosted look.

## Windowing

Launcher windows are Compose surfaces, not Android system windows. The title bar owns a `detectDragGestures` handler, the bottom-right corner owns a resize handler, and the traffic lights map to close / minimise / zoom. Press events focus a window at the `Initial` pointer pass without consuming them, so child controls keep working. Windows animate with a short fade + 0.985→1 scale, disabled by the reduce-motion setting (and the setting is one switch, not a per-component flag).

## App discovery and launching

`AppsRepository` queries `ACTION_MAIN` + `CATEGORY_LAUNCHER` — the standard launcher intent, which also satisfies Android 11 package-visibility rules without `QUERY_ALL_PACKAGES`. Icons are rasterised off the main thread and cached in the app list. Launching uses an explicit `ComponentName` with `FLAG_ACTIVITY_NEW_TASK`.

`PackageManager` add/remove broadcasts received while the launcher is resumed trigger a debounced rescan.

## Files (SAF)

`FinderApp` keeps a stack of `DocumentFile` objects. The root comes from `OpenDocumentTree` with persisted read/write permission; if no folder is chosen it falls back to an app-private `Documents` directory, so Finder is always usable. All directory I/O runs on `Dispatchers.IO`; failures degrade to an empty listing rather than a crash.

## Data integrations

- **Weather** — Open-Meteo geocoding + forecast, no API key, city name only, 15-minute in-memory cache, explicit error text.
- **Calendar** — `CalendarContract.Instances` between two instants; permission is optional and the UI shows an enable affordance instead of failing.
- **Assistant** — `AssistantEngine.local` handles real commands; `AssistantEngine.cloud` posts to a user-supplied HTTPS OpenAI-compatible endpoint with a strict system prompt stating it is not Siri. No key ships with the app.
- **System stats** — `ActivityManager.MemoryInfo`, `StatFs`, battery broadcast, `DisplayManager` refresh rate.

## Theming and design tokens

`ui/theme/Theme.kt` holds the tokens: 12 dp window radius (Golden Gate tightened corners), 18 dp panel radius, 30 dp menu bar, 12 dp traffic lights, 7 accent colours, and the `GlassStyle` data class (`dark`, `tint`, `blur`, `blurRadius`) that is provided via `LocalGlassStyle`. Typography is the platform sans with tightened display letter-spacing; no Apple font is bundled.

Icons are original Compose squircle tiles: a duotone gradient face, a 23.5% corner radius, a white glyph from Material Symbols, an inner highlight and a hairline rim. Wallpapers are drawn with `Canvas` gradients, radial glows, dune paths and aurora ribbons.

## Responsiveness

- Desktop icon columns: 1–3 based on available width.
- Window default size: 60% of the desktop on tablets, 94% on compact widths; windows clamp on resize/rotation.
- Finder, Notes and Calendar switch between two-pane and single-pane layouts at 620–720 dp.
- Settings uses a sidebar at ≥720 dp and a scrollable chip row below that.
- Dock magnification is pointer-driven on mouse and press-driven on touch.

## Performance choices

- One clock ticker for the whole desktop (1 Hz), not one per widget.
- Battery via a sticky broadcast, not polling.
- No infinite decorative animation; Dock magnification is distance-based and only evaluated while hovering.
- Blur can be turned off (also helps low-end devices).
- Window frames are not animated during drag, so dragging stays 1:1 with the finger.
- `LauncherState.refreshApps` debounces rescans.
- App icons are decoded once per scan, not per frame.

## Accessibility

Every interactive surface has a `contentDescription` or `semantics` label; targets are at least 44×44 dp; all text uses `sp`; the reduce-motion setting and Android animator scale are respected; glass tints are chosen for contrast in both Light and Dark appearance.

## Extending

1. Add a `BuiltinApp` enum entry with `AppKind.WINDOW`.
2. Write `MyApp(state, prefs, modifier)` in `ui/apps/`.
3. Add it to `AppContent` in `WindowLayer`.
4. Optionally add menu entries in `MenuBar.appMenus` and a default shortcut in `Prefs`.

No routing, DI framework or navigation library is required for that.
