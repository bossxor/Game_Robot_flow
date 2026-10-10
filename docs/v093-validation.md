# RoboRelay v0.9.3 설치·검증

2026-10-10, 기능 소스 `78650e8`을 main에서 동기화하고 APK를 빌드했습니다. Android versionName 0.9.3 / versionCode 16입니다.

## 자동 검사

- 웹 테스트 230개 통과 및 정적 웹 빌드 성공.
- 네이티브 검사 5,067개 통과. 일반 운반원·중장비 각각의 검증에서 500개 레벨 모두 클리어.
- Android assembleDebug/lintDebug 성공. 오류 0개, 경고 16개.

## 기기 확인

Samsung SM-S948N, 1440×3120 화면에서 기존 설치 위에 업데이트했습니다. 설치 전후 SharedPreferences 전체 비교에서 차이가 없습니다. 기존 진행·코인·기록·아이템·무제한 모드·사운드 설정을 유지했습니다.

저장 진행과 보상에 영향을 주지 않는 debug inspect 모드에서 다음을 확인했습니다.

- 새 메인 로고, 큰 로봇·플레이 버튼, 현재 스테이지 배지.
- 5개 작업 공간과 묶음 연결선, 3열×2행 대기열의 열 단위 중앙 정렬·높이 축소.
- 컬러 제거 선택 시 1.6배 확대, 드래그 후 선택 모드 유지, 이동한 화면에서 실제 색 제거 및 기본 카메라 복귀. 같은 아이템을 다시 눌러 취소.
- 무제한 모드에서 여러 색을 제거해 대기열을 줄이고 마지막 컨테이너 자동 출격·10배속 전환, 블록 회수·트럭 출발 후 작업 완료 확인.
- 확인 중 렌더링 로그의 GL error는 0. 10배속의 로봇 4기·블록 36개 시점에 render CPU mean 12.31ms. 이는 CPU 구간 평균이며 프레임률이나 최대 부하 성능 보장은 아닙니다.

검증 후 정상 메인으로 복귀했습니다. 검증 전후 저장 전체 비교에도 차이가 없습니다. 짧은 화면의 실기기 배치, 최대 부하의 지속적인 10배속 성능, 420ms 이동 모션의 영상 평가는 별도 확인 대상입니다.

APK: `releases/RoboRelay-v0.9.3-native-3d-debug.apk` (개발 서명).

SHA256: `3DA8C02CB9987E42B432F560EE47ECA47E10ADBC12DB95909EE31D4AB51C3A13`.

로컬 확인 자료: `artifacts/roborelay-v093-home.png`, `artifacts/rr-pan2.png`, `artifacts/rr-clear.png`, `artifacts/rr-queue3.png`, `artifacts/rr-finish.png`, `artifacts/rr-result.xml`. 기기 화면과 저장 파일은 Git에 포함하지 않습니다.
