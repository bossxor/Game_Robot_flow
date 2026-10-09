# Native game UI / UX v0.4.0

## Changes

- One consistent game sheet system replaces Android AlertDialog titles, lists and default buttons across tutorial, pause, results, settings, robot information, shop, purchase, restart, exit, reset and boosters.
- Deep green headers, cream surfaces, lime primary actions, rounded information cards, dark scrim and 220 ms entrance animation.
- Settings use custom ON/OFF chips and styled volume controls. Shop cards explain item effects and ownership; purchases show cost and remaining coins before confirmation.
- Leaving a mission and restarting require confirmation. Settings opened during a mission return to the pause menu; continuing is explicit.
- Booster confirmation pauses the mission and cancellation preserves inventory. Color removal pauses while choosing a color. Pull-out mode explains how to select a target and cancel.
- Brief feedback uses an animated in-game banner. Menu actions use ripples and press animation; canvas controls show pressed state and cancel selection when dragged away.
- Mini/power movement changed from 14 to 3.8 blocks per second at 1x; mega changed from 10 to 2.8. Walking animation cadence reduced. The 2x control still doubles simulation speed.

## Validation on 2026-10-09

- `assembleDebug lintDebug` succeeded, with no lint errors.
- Java engine: 623 assertions passed, including a movement-distance check. All 200 levels completed at each of three power probabilities (600 simulations).
- Native APK audit passed: 200 level assets, native renderer/engine and no WebView/web assets.
- v0.4.0 / versionCode 5 installed on Samsung SM-S948N, retaining progress and inventory.
- Inspected settings, shop, pause and gameplay screenshots at 1440 x 3120. Fixed the initially clipped close glyph and verified the corrected button in the final build.
- Dispatched a container at 1x and observed block recovery. Settings music toggle changed to OFF and back to ON; Done returned to the pause menu. No application crash output appeared in the current process log during these checks.
- Screenshots and UI dumps are stored under ignored `artifacts/device-v04-*` paths.

The APK is signed for development. These checks do not constitute store certification or manual playthroughs of all 200 levels. Purchase confirmation, reset and every result branch have not all been exercised manually on the user's device.
