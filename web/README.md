# bobmap-link.redbridgedev.ai.kr 공유 링크 사이트

공유 링크 형식: `https://bobmap-link.redbridgedev.ai.kr/place/{카카오 place id}?n=이름&c=카테고리&a=주소&lat=..&lng=..`

- 앱이 설치된 기기: Android 앱 링크가 가로채 매장 상세로 바로 이동
- 앱이 없는 기기: `place/index.html` 랜딩이 뜨고 "앱에서 열기"(스토어 폴백) / "카카오맵에서 보기" / "Google Play에서 설치" 제공

## 호스팅: GitHub Pages (이 저장소의 `gh-pages` 브랜치)

이 폴더(`web/`)는 원본이고, 실제 서빙은 **`gh-pages` 브랜치 루트**에서 합니다. GitHub Pages 설정(저장소 Settings → Pages)은
source = `gh-pages` / `/`, custom domain = `bobmap-link.redbridgedev.ai.kr` 로 되어 있습니다.

`gh-pages` 브랜치 구성:

| 파일 | 역할 |
|---|---|
| `.well-known/assetlinks.json` | 앱 링크 검증(패키지명 + 서명 인증서 SHA-256) |
| `place/index.html` | `/place/{id}` 랜딩 |
| `404.html` | GitHub Pages는 rewrite가 없어서 `/place/{id}`는 404로 떨어짐 → 같은 랜딩을 404.html로 서빙 |
| `index.html` | 루트 → Play 스토어로 리다이렉트 |
| `CNAME` | 커스텀 도메인 |
| `.nojekyll` | Jekyll이 `.well-known` 같은 점 폴더를 버리지 않도록 |

### 배포 갱신

`web/` 파일을 고친 뒤 `gh-pages` 브랜치에 복사해 커밋·푸시하면 1~2분 내 반영됩니다.

```bash
git fetch origin gh-pages && git worktree add /tmp/gh-pages gh-pages
cp web/place/index.html /tmp/gh-pages/place/index.html
cp web/place/index.html /tmp/gh-pages/404.html
cp web/.well-known/assetlinks.json /tmp/gh-pages/.well-known/assetlinks.json   # 지문은 아래 참고
(cd /tmp/gh-pages && git add -A && git commit -m "pages 갱신" && git push origin gh-pages)
```

## DNS (도메인 관리자가 1회)

`redbridgedev.ai.kr`의 네임서버는 네이버 클라우드(ns-ncloud.com)입니다. Global DNS에 레코드 1개를 추가합니다.

| 호스트 | 타입 | 값 | TTL |
|---|---|---|---|
| `bobmap-link` | CNAME | `grape0919.github.io.` | 300 |

추가 후 수 분~수십 분 뒤 GitHub가 도메인을 확인하고 Let's Encrypt 인증서를 자동 발급합니다. 발급되면 Settings → Pages에서 **Enforce HTTPS**를 켜세요(API: `gh api -X PUT repos/grape0919/MapForLunchApp/pages -F https_enforced=true`).

## assetlinks.json 지문

`sha256_cert_fingerprints`에 서명 인증서 SHA-256이 있어야 앱 링크 자동 검증이 통과합니다. 현재 `gh-pages`에는 테스트 빌드 서버의 디버그 키 지문만 들어 있습니다. 출시 전에 Play 앱 서명 키 지문을 추가하세요.

```bash
# Play 앱 서명: Play Console → 설정 → 앱 무결성 → 앱 서명 키 인증서 SHA-256
# 직접 서명:
keytool -list -v -keystore <release.keystore> -alias <alias> | grep SHA256
# 로컬 디버그 키:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA256
```

콜론 포함 대문자 형식(`AA:BB:…`) 그대로, 배열에 여러 개를 둘 수 있습니다.

## 검증

```bash
curl -s https://bobmap-link.redbridgedev.ai.kr/.well-known/assetlinks.json
adb shell pm verify-app-links --re-verify kr.ai.redbridgedev.bobmap
adb shell pm get-app-links kr.ai.redbridgedev.bobmap      # verified 여부 확인
adb shell am start -a android.intent.action.VIEW -d "https://bobmap-link.redbridgedev.ai.kr/place/26338954?n=테스트"
```

검증 전에도 `bobmap://place/{id}` 커스텀 스킴은 동작합니다.
