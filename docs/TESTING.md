# Testing

## Verified in this build environment (Linux, no physical device)

| Check | Command / method | Result |
| --- | --- | --- |
| Debug build compiles | `./gradlew :app:assembleDebug` | **PASS** |
| Release build compiles and signs | `./gradlew :app:assembleRelease` with a locally generated keystore | **PASS** |
| APK exists | `app/build/outputs/apk/release/app-release.apk` | **PASS** (~11 MB) |
| Package / version | `aapt2 dump badging` | `com.goldengate.desktop`, versionCode `1`, versionName `1.0.0` |
| Label | `aapt2 dump badging` | `Golden Gate Desktop` |
| Min / target SDK | manifest + Gradle config | min 26, target 35, compile 35 |
| HOME intent filter present in the packaged manifest | `aapt dump xmltree ... AndroidManifest.xml` | **PASS** — `android.intent.category.HOME` + `DEFAULT` present |
| APK signature | `apksigner verify --print-certs` | **PASS** — v1/v2 scheme, CN=Golden Gate Desktop |
| Permissions declared | `aapt2 dump badging` | INTERNET, ACCESS_NETWORK_STATE, READ_CALENDAR, RECORD_AUDIO, VIBRATE, WRITE_SETTINGS (maxSdk 35) |
| Gradle wrapper generated | `gradle wrapper --gradle-version 8.13` | **PASS** |
| CI workflow syntax | reviewed manually | Not executed here; GitHub runs it on push |

**Not verified here:** installation on a real phone, first launch, Home-role grant, visual layout,
gestures, performance, battery use, rotation, tablet layout, and each built-in app’s runtime
behaviour. No Android emulator/device was available in this environment. The APK compiles and is
correctly packaged, but **runtime behaviour on hardware still needs your device pass**.

## Manual test plan (please run on the Samsung device)

### 1. Install and role
1. Install `GoldenGateDesktop-release.apk` (or the CI debug artifact).
2. Launch from the app drawer. The setup sheet should appear.
3. Tap **Set as default Home** → chooser → Golden Gate Desktop → Always.
4. Press Home / Home gesture → desktop returns. The “Set as Home” chip should be gone.
5. Settings → Apps → Choose default apps → Home app should show Golden Gate Desktop.

### 2. Desktop
- Long-press the wallpaper area: no crash; Dock and icons remain interactive.
- Drag a desktop icon onto another: order changes and survives a launcher restart.
- Long-press an icon → menu → Add to Dock / Remove from Desktop.
- Widgets: drag the clock widget; long-press → change S/M/L → remove; add a new widget from Settings.

### 3. Dock
- Hover with a mouse (or DeX pointer): neighbouring icons grow, layout does not shift.
- Tap icons: windows open; a dot appears under running apps.
- Long-press an icon without moving: context menu. Long-press and drag sideways: reorder.
- Settings → Dock → auto-hide: the Dock hides; the bottom handle reveals it.
- Launchpad tile at the far right opens the app grid.

### 4. Windows
- Drag by the title bar; resize from the bottom-right corner.
- Traffic lights: close, minimise (restore from Dock), zoom (green), double-click title bar.
- Open two windows; click to focus; Ctrl+Tab cycles; the front window has a stronger shadow.
- Rotate the device: windows stay on-screen and usable.

### 5. Finder
- First open: choose a folder via the system picker and confirm the persistent permission prompt.
- Browse into folders, breadcrumb back, switch grid/list, sort, search.
- New folder, rename a file, delete a file (confirm the provider allows it).
- Select a file → Open launches the right app; preview pane shows size/date/type.

### 6. Spotlight and Assistant
- Ctrl+Space (hardware keyboard) or the menu-bar search icon opens Spotlight.
- Type an app name → Enter launches it. Type a file name after granting a Finder folder.
- Ask the Assistant “open notes”, “note buy milk”, “wallpaper midnight bay”, “time”.
- Optional: add an endpoint/key in Settings → Assistant, then ask a general question and confirm the
  reply appears and history persists.

### 7. Control Center / notifications
- Brightness slider dims the launcher immediately; “System” requests the special access if not granted.
- Volume slider changes media volume (with the system UI).
- Wi-Fi / Bluetooth / Airplane / Focus open the official panels.
- Clock in the menu bar opens the date/calendar panel; with calendar permission, today’s events appear.

### 8. Appearance and accessibility
- Appearance → Light/Dark/Auto changes every surface.
- Liquid Glass slider: 0% vs 100% visibly changes tint everywhere; blur toggle still leaves legible text.
- Accent colours update highlights.
- Enable Reduce motion → window open/close no longer animates.
- Increase Android font size → text scales without clipping.

### 9. Settings integrity
- Every sidebar page opens; every switch persists across a force-stop and relaunch.
- Default Home page reflects the real role state.
- Privacy page opens Android app info; Advanced copies settings JSON without the API key.

### 10. Tablet pass
- Repeat 2–8 on a tablet or DeX window: sidebar layouts, 3-column desktop icons, larger windows.

## Reporting

If something fails, include: device model, Android/One UI version, exact steps, and a screenshot.
`Settings → Advanced → Copy settings JSON` produces a shareable state dump with no API key.
