# Robot Flow v0.7.2 — 메인 및 대기열

## 변경

- 메인: 큰 입체 로고, 확대된 두 모델, 민트색 장난감 공사장 받침대, 해·구름·크레인, 움직이는 트럭. 78dp 플레이 버튼과 이름이 붙은 62dp 입체 메뉴.
- 대기열: 실제 남은 컨테이너만 행별 중앙 정렬. 4/3/2/1개로 줄어도 크기·간격 유지. 빈 행을 제거하고 영역 높이도 220ms 동안 축소. 기존 열별 맨 앞 선택·연결 묶음·스와이프 규칙 유지.
- 게임 엔진의 출격·속도·아이템·보상 규칙은 v0.7.1 유지.

## 확인

- Android assembleDebug/lintDebug 성공: 오류 0, 기존 경고 15.
- 웹 221개 테스트 통과, 정적 웹 빌드 및 메인 브라우저 확인, 콘솔 오류 없음.
- 네이티브 규칙 테스트와 레이아웃 검사 통과: 긴 대기열 최대 4행, 3/1/0행 축소 높이, 1/2/3개 컨테이너 중앙 좌표.
- 기기에서 메인 확인. 저장·보상에 영향을 주지 않는 debug inspect 모드로 2열×2행, 1열×1행 중앙 정렬·높이 축소 확인. 정상 메인으로 복귀.
- QA 도중 무제한 설정을 On으로 바꾼 현재 기기 값을 보존. 진행도·코인·기록·아이템 재설정 없음.
- 버전 0.7.2 / versionCode 11, ADB 업데이트 설치 완료.

APK: `releases/RobotFlow-v0.7.2-native-3d-debug.apk` (개발 서명).
SHA256: `A88552737074FE4F8AADD419B91666FE5B438F5B67A874DD395F2257E1F554F1`.
로컬 확인 화면: `artifacts/native-toy-home-final.png`, `artifacts/native-queue-2x2.png`, `artifacts/native-queue-1x1.png`, `artifacts/web-toy-home.png`.

다음 디자인 변경은 [현재 UI 규칙](game-ui-v07.md)을 기준으로 합니다. 웹은 Canvas/SVG 미리보기이며 APK는 Java/OpenGL 3D입니다.
