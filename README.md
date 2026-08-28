# 김대리밥지도 v2

주변 맛집을 지도에서 검색하고, "골라줘 내 점심" 룰렛으로 오늘의 점심을 뽑아주는 앱.

2020년 출시했던 v1(Java + 구 Daum 맵 SDK)을 Kotlin으로 전면 리라이트했다.

## 기술 스택
- Kotlin + Jetpack Compose (Material 3)
- 카카오맵 Android SDK v2 + 카카오 로컬 REST API (Retrofit + kotlinx.serialization)
- Room (즐겨찾기 · 방문 기록 · 제외 가게) + DataStore (검색 반경 · 제외 카테고리)
- targetSdk 35 / minSdk 26, AGP 8.7, Gradle 8.9

## 기능
- 현재 위치(또는 지도 중심) 반경 100~1000m 맛집 키워드 검색
- 룰렛 애니메이션으로 랜덤 점심 뽑기 → "여기로 결정" 시 방문 기록 저장
- 제외 필터: 카테고리 제외("오늘은 이건 빼고") + 개별 가게 제외
- 즐겨찾기, 방문 기록 + 별점 · 나만의 한 줄 기록
- 카카오 플레이스 상세 페이지 바텀시트(WebView), AdMob 배너

## 빌드
1. `local.properties.sample`을 `local.properties`로 복사
2. [카카오 개발자 콘솔](https://developers.kakao.com)에서 발급한 키를 채운다
   - `KAKAO_NATIVE_APP_KEY` — 카카오맵 SDK 용
   - `KAKAO_REST_API_KEY` — 로컬 검색 API 용
3. `./gradlew assembleDebug`

`local.properties`는 절대 커밋하지 않는다. 키가 비어 있으면 앱은 안내 문구를 표시한다.
