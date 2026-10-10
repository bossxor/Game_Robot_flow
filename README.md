# RoboRelay

색 맞는 로봇을 보내 보드 블록을 회수하고 트럭에 싣는 **Android 네이티브 3D 퍼즐**입니다.

최신 설치: [RoboRelay v0.1.0 APK](https://github.com/bossxor/Game_Robot_flow/releases/tag/v0.1.0) · 로컬 `releases/RoboRelay-v0.1.0-native-3d-debug.apk`

Android 8.0(API 26)+ / OpenGL ES 2.0. 오프라인 500레벨.

## 플레이

- 대기열 앞쪽 컨테이너를 골라 작업 칸(기본 5)에 올린다
- 같은 색 로봇이 블록을 집어 공용 트럭에 싣는다
- 슬롯이 막히면 패배, 보드를 비우면 클리어
- 부스터: 슬롯 추가 · 컬러 제거 · 셔플 · 꺼내기
- 배속 1 / 3 / 5(기억), 남은 컨테이너가 적을 때 자동 마무리 10배
- HARD(5배수) · SUPER HARD(10배수), 일일 보상, 코인 상점
- 설정 제목 7회 터치 → 아이템 무제한 On/Off

## 기술

- 네이티브 Activity + OpenGL ES 3D 메시·조명·깊이
- 로봇: 본체=컨테이너 색, 손·얼굴 흰색, 어두운 외곽선
- 컨테이너 선택 시 로켓 상승, 안착 시 짧은 먼지
- APK에 WebView/HTML/JS 없음. 레벨 JSON만 포함
- 진행은 `SharedPreferences` 로컬 저장

## 설치

```powershell
adb install --user 0 -r releases/RoboRelay-v0.1.0-native-3d-debug.apk
adb shell am start -n com.bossxor.robotflow/.MainActivity
```

개발 서명 debug APK입니다. 스토어용은 별도 release 서명이 필요합니다.

## 빌드

JDK 17, SDK 36, Gradle 8.13.

```powershell
cd android
.\gradlew.bat assembleDebug
```

산출: `android/app/build/outputs/apk/debug/app-debug.apk`

## 검증

```sh
node --test
node scripts/verify-native.mjs
```

## 구조

```text
android/app/src/main/java/com/bossxor/robotflow/
  MainActivity.java   메뉴·수명주기·오디오
  NativeScene.java    OpenGL 3D
  NativeHud.java      슬롯·대기열·아이템 UI
  NativeGame.java     규칙·로봇 시뮬레이션
  PlayerSave.java     진행 저장
public/construction-levels/   레벨 500개
scripts/                      생성·검증
tests/native/                 네이티브 엔진 테스트
```

웹 미리보기(선택): `node scripts/serve.mjs` → `http://localhost:5173/`

저장소: [bossxor/Game_Robot_flow](https://github.com/bossxor/Game_Robot_flow)
