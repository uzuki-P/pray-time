# Pray Time

Pray Time is a small Kotlin/JVM desktop starter for Windows and Linux. It uses Compose Multiplatform Desktop for the window and system tray, with Material 3 components for the first UI pass.

The initial screen computes today's prayer times for the default region, Medan Johor (Medan, Indonesia), using the Adhan library with the Singapore method. It shows a live countdown to the next prayer, skips sunrise as a target, and lets you mark past prayers as completed from the main window. It also has a tray menu and a theme-aware window and tray icon.

## Run it

Requirements:

- JDK 17 or newer
- No global Gradle install is needed. The Gradle wrapper is included.

```bash
just dev
```

On Linux the tray is a hand-rolled StatusNotifierItem over DBus (dbus-java) with `ItemIsMenu=false`, so a left click toggles the compact popup directly — no menu detour, no xembed proxy. The popup shows the region, next prayer with live countdown, and today's checklist; left click again dismisses it. The right-click menu (rendered by the desktop via DBusMenu) has "Open schedule" and Quit. Closing the main window only hides it — the app keeps running in the tray until Quit. Windows and macOS still use the ComposeNativeTray library. Closing the window leaves the tray process running.

## Location search and data files

State lives in `$XDG_CONFIG_HOME/praytime/state.properties` (falling back to `~/.config/praytime/`): selected region, today's completed prayers, and up to 10 recently selected locations. The Medan-area built-in list stays in code and is always available.

A worldwide city list (~169k entries from GeoNames, stored as SQLite) ships inside the app. On the first search it is extracted once to `~/.config/praytime/cities.db`, and lookups then run inside SQLite — the list is only read while you are actually searching, never at startup, and is never held in memory.

## Project direction

The intended product flow is:

1. Pick a region and calculation method.
2. Load or calculate the day’s prayer times.
3. Schedule a quiet desktop notification shortly before each prayer.
4. Let the user mark a prayer as completed from the tray menu or the main window.

Keep calculation, scheduling, persistence, and UI in separate packages as the app grows. The tray should remain useful even when the main window is closed.

## Work left for the next implementation pass

- Persist the notification lead time.
- Schedule quiet notifications shortly before each prayer, surviving sleep, wake, timezone changes, and daylight-saving changes.
- Verify Windows autostart (implemented via the HKCU Run key; Linux XDG autostart is done and tested).
- Decide how to handle notification permissions and desktop-environment differences.
- Extend tests for month boundaries and high-latitude edge cases.

## Icons

The generated artwork is in `src/main/resources/icons`:

- `pray-time-app-light.png` and `pray-time-app-dark.png` are launcher/window variants.
- `pray-time-tray-light.png` and `pray-time-tray-dark.png` are simplified tray variants.
- `pray-time.png` is the Linux packaging icon.
- `pray-time.ico` is the Windows packaging icon.

The runtime selects the light or dark artwork from the desktop theme. Native package icons are static, so the light launcher icon is used for the package metadata.
