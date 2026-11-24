# Known limitations and platform constraints

Golden Gate Desktop is a normal, unprivileged Android application. It does not use root, Shizuku,
hidden APIs, accessibility abuse, device-owner provisioning, or bootloader changes. That means some
parts of the macOS experience are impossible, and the UI says so where relevant.

## Android / One UI ownership

| Area | Owner | Launcher behaviour |
| --- | --- | --- |
| Lock screen | Android + One UI | Not touched. No custom lock screen, no wallpaper-on-lock control. |
| Notification shade | Android + One UI | Not replaced or read. The launcher’s own panel shows date, calendar, battery and storage only. |
| Recent apps | Android | Not replaced. Ctrl+Tab cycles launcher-owned windows instead. |
| Status bar | Android | Draws underneath it (transparent), adds its own menu bar below. |
| Navigation bar / gesture pill | Android | Draws around it; Dock sits above the inset. |
| Quick Settings | Android | Not replaced. Control Center opens official panels for protected toggles. |
| Boot animation | Vendor | Untouched. |
| Always-on display | Vendor | Untouched. |
| Default apps for browser/gallery | Android | Photos/Safari shortcuts launch whichever app Android resolves. |

## Permissions and special access

- **Calendar** — optional `READ_CALENDAR`; without it widgets show “Calendar access off”.
- **Microphone** — optional; only used while the Assistant mic is active.
- **Modify system settings** — optional; needed only for device-wide brightness.
- **SAF folder access** — the user grants one tree; everything else in storage stays private.
- **No** `QUERY_ALL_PACKAGES` (uses the launcher intent query instead).
- **No** notification-listener access.
- **No** `MANAGE_EXTERNAL_STORAGE`.
- **No** `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, or accessibility-service hooks.

## Launcher windows are not system windows

Draggable windows are Compose surfaces inside the launcher. They can host the built-in apps listed
in the README, but they cannot embed another Android app’s activity. “Safari” and “Photos” therefore
open the real installed apps; Android has no API for a launcher to reparent arbitrary activities.

## Rendering

- Backdrop blur relies on `RenderEffect` (Android 12+). On older devices the tint layer still reads
  as frosted glass but without a true blur.
- Live wallpaper / video wallpaper is not implemented; wallpaper is a static image or drawn artwork.
- HDR depth and real refraction (macOS uses a compositor-level material) cannot be reproduced
  exactly; the recreation uses a blurred backdrop copy, tint, gradient highlight and hairline rim.
- Apple’s SF Pro font is not redistributable, so the platform sans is used with tightened tracking.

## Data and network

- Weather requires internet; failures show a clear message and keep the last cached forecast for
  the session only.
- The Assistant’s cloud mode requires the user to supply their own endpoint, model and key. No key
  is bundled, and no request is made unless a key is set.
- Calendar data is read on demand for the visible day/month only.
- Settings are not synced across devices (Android backup may include the private preferences if the
  user has backup enabled).

## Multi-window / desktop modes

- Samsung DeX: the launcher runs, but DeX has its own desktop shell; making this app the DeX home
  surface is outside a standard launcher’s control.
- Freeform/multi-window: launcher windows remain inside the launcher’s own task.

## Known rough edges

- Dock hover magnification is computed from pointer x position and an approximate icon stride; on
  very large Dock sizes the peak may sit between two icons.
- Desktop icon drag reorder targets a nearest index rather than showing live gaps.
- Widget positions are stored as fractions, so extreme aspect-ratio changes can require a small drag
  to re-tidy them.
- Folder tile previews show a folder glyph rather than a 2×2 mini-grid of contents.
- Finder rename depends on the document provider supporting it; some providers refuse and the UI
  reports that honestly.
- The release APK in this repository’s CI is debug-signed unless you add signing secrets.
