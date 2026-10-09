# POD — personal Always-On Display

A sideloaded Android app that shows a clock + date over the lock screen
when the screen turns off, for personal use on a Sony Xperia 5 IV
(Android 14). Design rationale, trade-offs and what's still unverified
were discussed and written up as `aod-design.md` in the project's shared
files (not part of this repo) — read that before changing the
activation/battery logic.

## What it does

- Clock + date, desk-clock styling, true-black background (OLED).
- Shows on screen-off, suppressed while in a pocket (proximity) or
  face-down (accelerometer), resumes when clear.
- Runs continuously — no time-of-day window. A manual on/off switch in
  the app (not a schedule) is how you turn it off, e.g. overnight.
- Minute-level redraw via `AlarmManager`, not a wake loop.
- Small periodic pixel drift for OLED burn-in protection.
- Brightness mirrors the device's current system brightness.

## What it is not

Not a privileged system AOD component — that requires root, which is off
the table (see design doc). This is a normal app with
`showWhenLocked`/`turnScreenOn`, so it costs more battery than Sony's
built-in ambient display for the same on-screen time.

## Building

This container has no Android SDK and can't reach `dl.google.com` to
fetch one (network policy denial, not a configuration bug here), so the
project was written and reviewed but never built/run in this session.

To build:
1. Open the `pod/` project root in Android Studio (or run `./gradlew
   assembleDebug` with `ANDROID_HOME` pointing at an installed SDK,
   platform 34, build-tools matching AGP 8.5.2).
2. Install on-device, grant the notification permission if prompted
   (Android 13+), and flip the switch in the app.

## Known open items (see design doc "Open items")

- Whether this panel exposes a sub-60Hz refresh mode to apps is
  unverified — check `Display.getSupportedRefreshRates()` once this is
  running on the device.
- `USE_EXACT_ALARM` is a restricted permission category (apps whose core
  function is alarms/clocks); if the system rejects it at install time,
  fall back to `SCHEDULE_EXACT_ALARM` and request it via
  `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- Visual style beyond "desk-clock, clock + date" (typeface, exact sizing)
  hasn't been specified further — current layout is a reasonable default,
  not a final design.
