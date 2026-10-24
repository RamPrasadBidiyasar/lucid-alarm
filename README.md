# WakeUp

A personal alarm app for Android that keeps ringing until you prove you are out of bed.

- A **routine** rings from a start time at a fixed interval until a hard stop, on the days you choose.
  Example: 6:00 AM, every 5 minutes, until 9:00 AM, Monday to Friday.
- The siren **rises from 30% to 100%** over 60 seconds and keeps ringing until you stop it.
- **Solving a mission** (math, typing or shake) stops only the current ring. The siren is quiet for
  60 seconds while you solve, and each correct step restarts those 60 seconds.
- **Scanning your QR code** (or typing a long phrase) confirms you are awake. It cancels every
  remaining ring for the day and arms tomorrow's routine.
- Settings shows which Android permissions are missing, and never claims the alarm is guaranteed
  when they are.

Plain Android framework code in Java, built with Maven. No AndroidX, no Gradle.

## Build the APK

```bash
./build-apk.sh                 # builds dist/WakeUp.apk
./build-apk.sh --install       # also installs it on a connected phone (USB debugging on)
./build-apk.sh --skip-tests    # skip the unit tests
```

Needs a JDK (11 or newer) and Maven. If the Android SDK is missing the script offers to install it
with `scripts/setup-sdk.sh` (command-line tools, platform 30, build-tools 30.0.3, about 300 MB).

On an Apple Silicon Mac the SDK's `aapt` is an Intel binary, so Rosetta must be installed once:
`softwareupdate --install-rosetta --agree-to-license`. The script checks for this and says so.
It is a release build (not debuggable), signed with a key the script creates the first time in
`~/.config/wakeup`. Back that folder up: an updated app can only replace an installed one if it is
signed with the same key.

To install by hand: copy `dist/WakeUp.apk` to the phone and open it, allowing installs from
that source.

## After installing

1. Open **Settings** (top right) and fix everything marked *Fix*.
2. Create your QR code in **Settings > Wake-up QR code**, print it, and stick it somewhere you must
   walk to.
3. Lock the phone and run **Test a real alarm in 1 minute**. That is the only real proof it works.

## Layout

```
src/main/java/com/wakeup
  model/        Plan, the routine data class
  data/         PlanStore (saved routines), MorningState (what happened today, settings)
  alarm/        Scheduler, AlarmReceiver, BootReceiver, RingService, SirenPlayer, Awake, Reliability
  util/         Fmt, time formatting
  ui/           UI design tokens and factories, BaseActivity, Rows, Segmented, Paste
  ui/widget/    Custom views: IconView, SwitchView, PulseView, CheckView, VolumeBars, ScanFrameView
  ui/sheet/     Bottom sheets: Sheet, TimeSheet, FixSheet
  ui/screen/    One Activity per screen
src/main/res    Theme, transitions, launcher icon
scripts/        setup-sdk.sh
build-apk.sh    One command to an APK
```

## How a ring is delivered

Only the next ring is scheduled, with `AlarmManager.setAlarmClock()`. When it fires,
`AlarmReceiver` schedules the following ring first, then starts `RingService`, which plays the
siren on the alarm audio stream and shows `RingActivity` over the lock screen. A crash can never
break the chain, and `BootReceiver` re-arms after a reboot or a clock change.

## Known limits

- Android cannot be forced past the phone's hardware maximum volume, and the app cannot stop you
  from powering off or force-stopping it. Use the reliability screen and test the alarm.
- Built against platform 30 so the old Maven Android plugin works. Newer permission checks
  (exact alarms, full-screen intent) are called by reflection.
- The font is the phone's system sans-serif.
