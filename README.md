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

## Project direction

The intended product flow is:

1. Pick a region and calculation method.
2. Load or calculate the day’s prayer times.
3. Schedule a quiet desktop notification shortly before each prayer.
4. Let the user mark a prayer as completed from the tray menu or the main window.

Keep calculation, scheduling, persistence, and UI in separate packages as the app grows. The tray should remain useful even when the main window is closed.

## Work left for the next implementation pass

- Add region search and timezone handling.
- Persist the selected region, notification lead time, and completed prayers.
- Schedule quiet notifications shortly before each prayer, surviving sleep, wake, timezone changes, and daylight-saving changes.
- Add Linux autostart and Windows startup options behind a user setting.
- Decide how to handle notification permissions and desktop-environment differences.
- Extend tests for month boundaries and high-latitude edge cases.

## Icons

The generated artwork is in `src/main/resources/icons`:

- `pray-time-app-light.png` and `pray-time-app-dark.png` are launcher/window variants.
- `pray-time-tray-light.png` and `pray-time-tray-dark.png` are simplified tray variants.
- `pray-time.png` is the Linux packaging icon.
- `pray-time.ico` is the Windows packaging icon.

The runtime selects the light or dark artwork from the desktop theme. Native package icons are static, so the light launcher icon is used for the package metadata.
