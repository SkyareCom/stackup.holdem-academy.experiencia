# STACKUP HOLD'EM ACADEMY — RELEASE RULES

- Android applicationId: com.skyare.stackupacademy
- versionCode: 226
- versionName: 1.0.9
- Runtime front: app/src/main/assets/index.html only
- No GitHub Pages, remote-first, remote fallback or remote WebView start URL.
- Release signing must use the upload key accepted by the existing Google Play app.
- Never commit keystore files or signing passwords.
- Before every release, app/src/main/assets/index.html must be byte-for-byte synchronized with the approved root index.html.
