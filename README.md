# RoboRelay — Native Android 3D

색상별 로봇을 출격시켜 블록을 회수하는 **Android 네이티브 3D 퍼즐 게임**입니다.
최신 설치 파일: [RoboRelay v0.10.0 APK](https://github.com/bossxor/Game_Robot_flow/releases/tag/v0.10.0), 로컬 경로 `releases/RoboRelay-v0.10.0-native-3d-debug.apk`.

v0.10.0은 레벨 표지판을 든 로봇, 영어 난이도 배지, 최대 3행 자동 승격 대기열과 원색 팔레트를 적용했습니다. 열쇠·잠금 컨테이너, 색상 회수 조건 봉인, 미스터리 컨테이너를 추가했습니다. [변경·검증 기록](docs/roborelay-v010.md)

v0.9.3은 새 메인과 열 단위 중앙 정렬, 컨테이너 로켓 이동, 두꺼운 묶음 연결선, 컬러 제거의 1.6배 확대·드래그 선택을 포함합니다. 마지막 자동 마무리는 10배속입니다. [기기 검증 기록](docs/v093-validation.md)

v0.9.0은 둥근 로봇·볼록한 메인 버튼, 선명한 컨테이너와 열 단위 중앙 정렬을 적용합니다. 한 열을 모두 쓰면 남은 열이 자연스럽게 중앙으로 이동하며 마지막 자동 마무리는 10배속입니다. [v0.9 UI 변경](docs/roborelay-v09.md)

v0.7.2은 5개 작업 공간 표시, 컨테이너 앞에서 순차 출격, 묶음 대기열, 화면을 터치하는 아이템 조작, 최대 3대의 대기 트럭과 확대된 로봇을 적용했습니다. NORMAL/HARD/SUPER HARD, 마지막 작업 자동 5배속, 연승 보상, 시간대 배경과 움직이는 메인을 포함합니다. 메인은 큰 입체 로고·로봇 두 대·파스텔 받침대·두툼한 플레이 버튼으로 개편했습니다. 대기열은 빈 행·열의 자리를 제거하고 각 행의 남은 컨테이너를 중앙 정렬합니다. [현재 UI·게임 규칙](docs/game-ui-v07.md), [검증 기록](docs/v072-validation.md)

참고 APK는 UI 배치와 조작 흐름 분석에 사용했습니다. 게임 그래픽과 코드는 로봇 테마로 자체 구현하며 참고 APK의 자산을 배포하지 않습니다.

Android 8.0(API 26) 이상과 OpenGL ES 2.0을 지원합니다. 최신 APK에는 500개 레벨이 포함되어 인터넷 연결 없이 실행됩니다.

## 네이티브 전환

- Android `Activity`와 네이티브 버튼·대화상자로 메뉴 구성
- `GLSurfaceView` + OpenGL ES로 3D 메시, 직교 카메라, 깊이 테스트, 조명 렌더링
- 두 다리 운반원 COURIER / 궤도·리프트 중장비 BUILDER: 구조가 다른 두 모델과 각각 7색 외장
- 걷기·회전·집기·운반·탑승, 공용 트럭 적재·출발, 최대 3대 대기·앞자리 이동
- 수백~2,304개 자재를 구역별 GPU 버퍼로 렌더링, 제거 구역만 갱신
- 화면 높이에 맞춰 카메라 영역을 늘이거나 줄이고 하단 조작부를 고정 배치
- 대기열은 평면 4열·최대 3행만 표시. 각 열의 맨 앞만 선택하며 숨겨진 다음 컨테이너가 자동으로 올라옴
- 크림·차콜·오렌지 UI, 그림 아이템 4개와 일시정지 2×2 아이콘
- 앱에 전체 스테이지 수를 표시하지 않으며 미해금 스테이지는 `?`로 표시

APK에 HTML, JavaScript, CSS 또는 WebView 실행기를 포함하지 않습니다.
현재 APK와 웹앱은 `public/construction-levels/`를 사용합니다. 불러올 때 공통 대기열 설계를 적용해 NORMAL부터 색상 순서와 5개 슬롯을 관리하도록 합니다. 자세한 내용은 [v0.8 난이도 변경](docs/roborelay-v08.md)을 참고하세요. `src/app.js`, `src/renderer.js`, `src/style.css`, `public/levels/`는 이전 프로토타입 참조 자료입니다. 웹 진입점은 `src/construction-app.js`이며 브라우저 Canvas로 게임을 표현합니다. APK의 OpenGL 3D 렌더러와는 별도입니다.

웹 미리보기는 `node scripts/serve.mjs`로 실행한 뒤 `http://localhost:5173/`에서 확인할 수 있습니다. `node scripts/build.mjs`는 정적 웹 빌드를 `dist/`에 생성합니다.

## 게임 기능

- 외곽·제거된 빈칸 flood fill 접근성, BFS 최단 경로, 타깃 예약
- 7색, 활성 컨테이너 기본 5개, 같은 열의 맨 앞만 선택·선택 후 동일 열 상승
- 기본 운반원 1개 / 게이지가 찼을 때 중장비 최대 8개 회수. 세 번째 로봇 단계 없음
- 컨테이너 5개와 독립적인 공용 트럭: 20개 적재 후 출발, 마지막 트럭은 부분 적재로 출발
- 슬롯 추가·셔플은 즉시 사용, 컬러 제거는 보드 블록 터치, 꺼내기는 대기 컨테이너 터치
- 묶음 선택·위험 선택 안내, 출격 모션 없이 컨테이너 앞에 로봇 등장, 남은 대기 컨테이너가 5개 이하이며 빈칸에 모두 들어가면 자동 출격·10배속
- 5의 배수 HARD / 10의 배수 SUPER HARD, 묶음 작업과 연승 보너스(최대 50코인), 시간대 배경
- 아이템 코인 구매, 설정 제목 7회 터치로 무제한 모드 On/Off
- 승리/교착 패배, 재시작, 일시정지, 1/2/3/4배속, 백그라운드 일시정지
- 500개 레벨, 10개 그림 테마와 변형. 초반 20×20(400개), 이후 48×48(2,304개) 보드
- 메인·스테이지·로봇·아이템·설정, 로컬 진행·기록·코인·부스터 저장
- 음악·효과음·진동 및 음량 설정
- Android 뒤로가기, 세로 화면, 시스템 바/노치 안전 영역 대응
- 귀여운 로봇 런처 아이콘 및 Android 13+ 테마 아이콘

네이티브 저장은 `SharedPreferences`를 사용합니다. 이전 프로토타입의 기기 내 localStorage JSON을 읽을 수 있으면 최초 실행에 가져옵니다. 판 도중의 블록/슬롯 상태는 저장하지 않습니다.

## APK 설치

APK를 Android 기기로 복사하여 설치하거나 USB 디버깅 기기에서:

```powershell
adb install --user 0 -r releases/RoboRelay-v0.10.0-native-3d-debug.apk
adb shell am start -n com.bossxor.robotflow/.MainActivity
```

기존 앱과 같은 applicationId와 개발 서명으로 업데이트합니다. 개발 테스트용 APK이며 스토어 배포에는 별도의 release 서명 APK/AAB가 필요합니다.

## Android 빌드

JDK 17, SDK Platform 36, Build Tools 35.0.0, Gradle 8.13, AGP 8.11.1을 사용합니다.
Android Studio에서 `android/` 폴더를 열거나:

```powershell
cd android
.\gradlew.bat assembleDebug
```

macOS/Linux:

```sh
cd android
chmod +x gradlew
./gradlew assembleDebug
```

결과: `android/app/build/outputs/apk/debug/app-debug.apk`
빌드 시 `public/construction-levels/`만 assets에 동기화합니다. APK는 로컬 산출물이며 Git에는 포함하지 않습니다.

## 검증

Node.js 20 이상과 JDK 17에서:

```sh
node --test
node scripts/generate-construction.mjs
node scripts/verify-native.mjs
```

Java 테스트는 APK의 `NativeGame.java`를 직접 컴파일합니다. 맨 앞 선택·동일 열 상승, 두 단계 로봇, 타깃 예약, 트럭과 컨테이너의 독립성, 전체·부분 적재 출발, 일시정지, 7색, 부스터와 이스터에그를 검사합니다. 500개 레벨을 기본 로봇만/게이지 보너스 사용 조건에서 각각 완주하고 모든 자재의 적재 완료를 확인합니다. `node --test`는 기존 참조 엔진 회귀 테스트입니다.

실행한 검증과 실제 기기 확인 범위는 [v0.5 검증 기록](docs/construction-v05-validation.md)에 정리합니다.

## 구조

```text
android/app/src/main/java/com/bossxor/robotflow/
  MainActivity.java      네이티브 메뉴, 수명주기, 오디오, 게임 제어
  NativeScene.java       OpenGL 3D 메시·카메라·조명·로봇 애니메이션
  NativeHud.java         화면 안에 고정되는 슬롯·컨테이너·부스터 터치 UI
  NativeGame.java        Android 게임 규칙과 로봇 시뮬레이션
  QueuePlanner.java      NORMAL/HARD/SUPER HARD 대기열 조정
  LevelLoader.java       JSON 레벨 읽기
  PlayerSave.java        네이티브 진행 저장과 이전 데이터 가져오기
public/construction-levels/ APK에 포함되는 그림 레벨 500개
public/levels/           기존 참조 프로토타입 레벨
src/engine.js            레벨 검증용 참조 규칙
src/levels.js            재현 가능한 레벨 생성
scripts/                 레벨 생성 및 Java 엔진 검증
tests/native/            실제 네이티브 엔진 테스트
.github/workflows/       Android APK 자동 빌드
```

## MVP 결정 및 후속 범위

한 색이 모두 회수되면 남은 같은 색 컨테이너를 자동 정리하여 잉여 컨테이너 교착을 방지합니다. 처음 클리어 시 기본 보상, 재클리어 시 25% 보상을 지급하고 가장 빠른 기록을 저장합니다.

모델은 네이티브 절차적 메시, 아이콘은 Canvas 도형, 음향은 직접 합성한 소프트 차임입니다. 특수 블록, 앱 내 레벨 편집기, 클라우드 저장, 광고·결제는 후속 범위입니다.

현재 디자인 기준은 [공사현장 개편 계획](docs/construction-redesign-plan.md)입니다. 모델·카메라는 `NativeScene.java`, 메뉴는 `MainActivity.java`와 `GameSheet.java`, 대기열은 `NativeHud.java`, 아이콘은 `GameIcons.java`에서 수정합니다.

설정 제목을 5초 안에 7번 터치하면 아이템 무제한 모드를 켜거나 끕니다. 터치 간격이 1.2초를 넘으면 새 순서로 시작합니다. On이면 수량 배지는 `∞`, 수량을 차감하지 않고 0개라도 사용 가능하며 재실행 후에도 유지됩니다. Off이면 원래 수량·사용 제한으로 돌아갑니다. 작업 공간 최대 6개 규칙은 유지합니다.

대상 저장소: [bossxor/Game_Robot_flow](https://github.com/bossxor/Game_Robot_flow)
`main`에 업로드하면 GitHub Actions에서 규칙 검증과 Android 빌드를 실행합니다. APK는 실행의 `RoboRelay-debug-apk` artifact에 저장됩니다.
