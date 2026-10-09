# Native 3D MVP validation

Date: 2026-10-09 (Asia/Seoul)

## Architecture

The Android application uses Activity/Views for menus and HUD and GLSurfaceView/OpenGL ES 2.0 for gameplay. Game rules run in Java. Gradle packages only JSON levels in assets. It does not package the old web UI.

## Executed checks

- Android debug APK builds with JDK 17 / SDK 36.
- Android lint passes with no errors; non-blocking warnings remain for portrait orientation, resource qualifiers and editor-only View constructors.
- Signed APK audit confirms 200 JSON assets, Java NativeGame/NativeScene classes, zero HTML/JS/CSS assets and zero WebView class references across all DEX files.
- Java native engine: 622 assertions passed.
- 200 levels completed with power chance 0, 0.1 and 1 (600 successful simulated runs).
- Reference engine/data tests passed, including distinct boards and progression through stage 200.
- Native HUD uses a fixed header, flexible camera viewport and fixed bottom controls; six waiting capsules per page, twenty stages per chapter.

## Device verification

- Installed v0.3.0 (versionCode 4) with `adb install --user 0 -r` on the connected Samsung SM-S948N. Installation and cold launch succeeded; existing application data was retained.
- Inspected actual 1440 x 3120 screenshots of the native home screen and stage 1. The 3D robot, shaded blocks, board, and all four booster controls render. The fixed bottom controls fit above the system navigation bar without scrolling.
- Dispatched red and blue containers and observed simultaneous robot movement. A later screenshot shows 26 / 41 blocks recovered, carrying robots and updated slots/gauge.
- Backgrounded the app and reopened it. The pause menu appeared; selecting Continue restored the 3D scene and gameplay.
- Captures are saved locally under `artifacts/device-native-home.png`, `artifacts/device-native-moving.png` and `artifacts/device-native-resumed.png` (ignored by Git).
- No application crash was observed. Targeted process logs contained no `FATAL EXCEPTION` / `AndroidRuntime` crash output during these checks.

Larger boards, all booster interactions, audio quality and extended performance have not yet been verified manually on the phone. The 200-level completion checks above are engine simulations, not 200 manual device playthroughs.
