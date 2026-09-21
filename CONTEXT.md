# Pray Time

A desktop tray app that shows today's prayer times for a chosen Region, counts down to the next prayer, and alerts the user around prayer times. The tray stays useful even when the main window is closed.

## Language

### Places and settings

**Region**:
A searchable place (city, district, or kelurahan) with coordinates and a timezone, used to calculate prayer times.
_Avoid_: City, Location

**Calculation method**:
The rule set used to compute prayer times for a Region (currently the Singapore method). A future setting.
_Avoid_: Parameters, convention

### Prayer times

**Day schedule**:
The six times calculated for one Region on one date: five prayers plus Sunrise.
_Avoid_: Schedule, prayer list

**Prayer**:
One of the five obligatory prayers in the day schedule: Fajr, Dhuhr, Asr, Maghrib, Isha.
_Avoid_: Prayer time (when referring to the entry), entry

**Sunrise**:
A time shown in the day schedule for reference. It is not a prayer and is never a countdown target or reminder subject.
_Avoid_: Sunrise prayer

**Next prayer**:
The first Prayer in the day schedule that has not yet arrived, searching tomorrow's schedule after the last one today.
_Avoid_: Upcoming prayer

**Due prayer**:
A Prayer whose time has passed and that is not yet completed.
_Avoid_: Overdue prayer, missed prayer, late prayer

**Completed prayer**:
A Prayer the user marked as prayed for that day. Completion is per day; the same prayer tomorrow starts uncompleted.
_Avoid_: Done, checked, prayed prayer

**Mark as prayed**:
The action that completes a due or past prayer.
_Avoid_: Check off, tick

### Alerts

**Due reminder**:
The alert that appears when a prayer becomes due and stays uncompleted. Built.
_Avoid_: Notification, popup

**Pre-prayer notice**:
The planned alert scheduled a few minutes before a prayer time.
_Avoid_: Reminder (for this concept), desktop notification

**Snooze**:
Dismissing a due reminder temporarily; it reappears after a fixed interval while the prayer is still uncompleted.
_Avoid_: Later, remind me again

**Dismiss**:
Hiding the current due reminder until the next prayer becomes due.
_Avoid_: Close
