# bobmap-link.redbridgedev.ai.kr 공유 링크 호스팅

공유 링크 형식: `https://bobmap-link.redbridgedev.ai.kr/place/{카카오 place id}?n=이름&c=카테고리&a=주소&lat=..&lng=..`

- 앱이 설치된 기기: Android 앱 링크가 가로채 매장 상세로 바로 이동
- 앱이 없는 기기: 이 폴더의 `place/index.html`이 뜨고 "앱에서 열기"(스토어 폴백) / "카카오맵에서 보기" / "Google Play에서 설치" 제공

## 배포할 파일

| 경로 | 파일 |
|---|---|
| `https://bobmap-link.redbridgedev.ai.kr/.well-known/assetlinks.json` | `.well-known/assetlinks.json` |
| `https://bobmap-link.redbridgedev.ai.kr/place/*` | `place/index.html` (모든 `/place/…` 경로를 이 파일로 rewrite) |

정적 호스팅(Cloudflare Pages, GitHub Pages, Netlify 등) 어디든 됩니다. `/place/*` → `place/index.html` rewrite 규칙만 설정하세요.
- Cloudflare Pages: `_redirects` 파일에 `/place/* /place/index.html 200`
- Netlify: 동일한 `_redirects`
- GitHub Pages: rewrite가 없으므로 `404.html`을 `place/index.html` 내용으로 두면 됩니다.

## assetlinks.json 지문 채우기

`sha256_cert_fingerprints`에 서명 인증서 SHA-256을 넣어야 앱 링크 검증(autoVerify)이 통과합니다.

```bash
# Play 앱 서명을 쓰는 경우: Play Console → 설정 → 앱 무결성 → 앱 서명 키 인증서의 SHA-256
# 직접 서명하는 경우:
keytool -list -v -keystore <release.keystore> -alias <alias> | grep SHA256
# 디버그 빌드 테스트용:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA256
```

콜론 포함 대문자 형식(`AA:BB:…`) 그대로 넣습니다. 여러 개를 배열로 둘 수 있습니다.

## 검증

```bash
adb shell pm verify-app-links --re-verify kr.ai.redbridgedev.bobmap
adb shell pm get-app-links kr.ai.redbridgedev.bobmap      # verified 여부 확인
adb shell am start -a android.intent.action.VIEW -d "https://bobmap-link.redbridgedev.ai.kr/place/26338954?n=테스트"
```

검증 전에도 `bobmap://place/{id}` 커스텀 스킴은 동작합니다.
