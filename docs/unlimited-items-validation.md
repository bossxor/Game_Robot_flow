# Hidden unlimited items mode — v0.4.1

Validated on 2026-10-09 (Asia/Seoul).

- Settings heading is an unmarked gesture area. Seven taps within five seconds toggle the saved mode; a gap longer than 1.2 seconds restarts the sequence.
- On/Off notifications appear inside the game settings sheet. Ordinary settings rows contain no mode switch or hint.
- All four use paths check mode-aware availability and avoid stock deduction while On, including targeted pull-out. Actual stock is retained, including zero stock; Off restores ordinary availability/counts immediately.
- Unlimited color removal bypasses the once-per-stage restriction. Ordinary color removal and physical slot/effect limits keep their existing rules.
- Game and shop show ∞ while On. Shop purchases are disabled while On. Restarting the app preserves mode; clearing progress resets it Off.
- Java verification passed 649 assertions, including gesture reset/repeat and repeated unlimited color clears. All 200 existing levels completed at three power chances (600 simulated runs).
- Android debug build and lint passed. Native APK audit passed. v0.4.1 / versionCode 6 installed on connected Samsung SM-S948N.
- Device checks: seven settings taps displayed On and persisted true; force-stop/relaunch retained true. Used shuffle twice; all stock stayed [3,2,3,3]. Seven more taps displayed Off and persisted false; all stock remained [3,2,3,3].
- Captures: ignored `artifacts/device-secret-on.png` and `artifacts/device-secret-off.png`.

The construction redesign remains a separate approved plan; this build adds the hidden mode to the current native game.
