# List Hub update notes

## Implemented in this update

- **Functional theme setting:** light/dark mode is applied through AppCompat DayNight and saved locally for app startup; the signed-in profile preference is also updated in Firestore.
- **Functional language setting:** English, Afrikaans and isiZulu are applied with AppCompat per-app locales. Translations are included for the main navigation, authentication, task and settings strings. Locale selection is saved locally and to the user profile.
- **Natural-language quick add:** the dashboard accepts text such as `Meet lecturer tomorrow 2pm`, extracts `tomorrow` and 12-hour times, then creates the task with the parsed due date/time.
- **Productivity summary:** dashboard shows completed task count and completion percentage for the selected task tab.
- **Reorderable task list:** drag task rows up/down to reorder the visible list. Reordering is currently session/display order only; persistent custom ordering is not stored in Firestore.
- **Share task list:** share the current task list as text through Android's share sheet. This is an export/share action, not live multi-user collaboration.
- **Gamification:** existing karma points and level-up logic remains in place.
- **Focus Mode / Pomodoro:** dedicated screen with 25-minute focus, 5-minute short break, 15-minute long break, editable intervals (long break constrained to 15–30 minutes), pause/resume, task label, and completed focus session records in Firestore (`focusSessions`). Actual elapsed focus seconds are recorded.

## Build and configuration

Open the `ListHubFirebase` project in Android Studio and let Gradle sync. The uploaded project did not include the Gradle wrapper scripts/JAR, and this environment has no Gradle executable or Android SDK, so I could not run an Android build here. Verify with **Build > Make Project** in Android Studio and run on an emulator/device.

## Not yet implemented

Live shared-list collaboration with member invitations, a full Kanban board with To Do / In Progress / Done states, and scheduled notification delivery for due dates are not implemented in this update. The existing date/time picker and Today/Upcoming filters remain available, and the existing notification preference remains a stored setting only. These require additional Firestore security/rules and Android notification scheduling work rather than being represented as complete here.

For security, `local.properties` (which contained a machine-specific SDK path and an API key) is not included in the updated archive. Copy `local.properties.example` to `local.properties`, set your SDK path, and provide your own Google Tasks API key if you use that integration. Keep `local.properties` private.
