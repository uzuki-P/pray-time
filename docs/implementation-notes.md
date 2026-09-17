# Implementation notes

## Suggested package layout

```text
dev.praytime
├── domain       prayer names, dates, regions, calculation settings
├── calculation  prayer-time calculation or provider adapter
├── scheduling   notification scheduling and lifecycle recovery
├── storage      preferences and completed-prayer persistence
├── platform     desktop notifications, autostart, and tray integration
└── ui           Compose screens, theme, and UI state
```

## Important desktop behavior

- Keep the application process alive after the window closes.
- Rebuild the schedule when the system date or timezone changes.
- Recalculate missed notifications after suspend and resume.
- Treat the tray as a primary compact view, not only as a quit button.
- Test the notification path on KDE Plasma, GNOME, and Windows because their notification and tray behavior differs.

## Material 3 direction

The starter uses Material 3 primitives, a restrained indigo/teal/gold palette, roomy cards, and a single clear action for the next prayer. Add a custom `PrayTimeTheme` before adding many screens so colors, typography, and shape tokens stay in one place.
