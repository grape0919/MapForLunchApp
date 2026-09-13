# 앱 로고 (1a "밥" 확정)

## 스펙
- 배경 Paprika #D9532B, 좌하단 장식 원 PaprikaLight #F0894F 55% (중심 32.2%/98.6%, 반지름 46.9%)
- 글자 "밥" Jua 400, Cream #FBF7F0, 아이콘 한 변의 58.6%, 시각 중심 보정을 위해 세로 52.7% 위치
- 홈 타일(앱 내 44dp): Ink #2A2420, radius 14, Jua 18
- 워드마크: 타일 40dp + "김대리밥지도" Jua 30, Ink

## 파일
- ic_launcher_background.svg / ic_launcher_foreground.svg — Android adaptive icon 레이어 (108dp 캔버스, 글자는 66dp 안전영역 안). res/drawable에 VectorDrawable로 변환(Android Studio Image Asset → 텍스트는 outline 처리 필요: Jua로 "밥"을 path로 변환해 넣기)
- app_icon_1024.svg / app_icon_round_1024.svg — 스토어·마케팅용 원본
- app_icon_1024.png / app_icon_round_1024.png — Jua 렌더 완료본. Play 스토어 512는 다운스케일
- home_tile_44.svg — 앱 내 로고 타일
- wordmark.svg — 스플래시·설정 화면용

## 주의
글자 로고이므로 배포 전 Jua 폰트로 "밥" 글리프를 outline(path) 처리해 폰트 의존 제거. Jua는 OFL 라이선스로 로고 사용 가능.
