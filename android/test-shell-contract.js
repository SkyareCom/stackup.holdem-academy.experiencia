const fs=require('fs');
const assert=require('node:assert/strict');

const gradle=fs.readFileSync('android/app/build.gradle.kts','utf8');
const manifest=fs.readFileSync('android/app/src/main/AndroidManifest.xml','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/skyare/stackupacademy/MainActivity.java','utf8');
const index=fs.readFileSync('index.html','utf8');
const sw=fs.readFileSync('sw.js','utf8');
const workflow=fs.readFileSync('.github/workflows/android-build.yml','utf8');

assert(gradle.includes('versionCode = 220'),'release versionCode must be 220');
assert(gradle.includes('versionName = "2.1.7"'),'release versionName must be 2.1.7');
assert(gradle.includes('applicationIdSuffix = ".test220"'),'debug package must be test220');
assert(main.includes('APP_URL = "https://skyarecom.github.io/stackup.holdem-academy.pub/"'),'native shell must preserve the Academy origin');
assert(main.includes('APP_PATH = "/stackup.holdem-academy.pub/"'),'native shell path restriction must match the Academy path');
assert(main.includes('LOCAL_ASSET_HOST = "appassets.androidplatform.net"'),'local fallback must use the isolated appassets host');
assert(main.includes('LOCAL_ENTRY_URL'),'native shell must retain a bundled offline fallback');
assert(main.includes('APP_ENTRY_URL'),'native shell must use an explicit remote entry document');
assert(main.includes('index.html?android_build=220'),'remote entry must identify build 220');
assert(main.includes('WebViewAssetLoader'),'native shell must retain bundled fallback assets');
assert(main.includes('.addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))'),'asset loader must only serve the isolated local fallback path');
assert(!main.includes('.setDomain(APP_HOST)'),'remote Academy origin must never be intercepted by bundled assets');
assert(main.includes('REMOTE_CONTENT_ACTIVE=true'),'native shell must log remote Academy delivery');
assert(main.includes('LOCAL_FALLBACK_MAIN_FRAME=true'),'native shell must log local fallback delivery');
assert(main.includes('onReceivedHttpError'),'native shell must handle main-frame HTTP failures');
assert(gradle.includes('androidx.webkit:webkit:1.14.0'),'WebViewAssetLoader dependency must be pinned');
assert(gradle.includes('syncAcademyWebAssets'),'Android build must package the Academy web release');
assert(gradle.includes('assets.srcDir(academyWebAssetsDir.get().asFile)'),'generated Academy web assets must be part of the APK/AAB');

assert(manifest.includes('android:name="com.skyare.stackupacademy.MainActivity"'),'production launcher must be native MainActivity');
assert(!manifest.includes('com.google.androidbrowserhelper.trusted'),'production manifest must not use old TWA launcher');
assert(!gradle.includes('com.google.androidbrowserhelper'),'old Android Browser Helper dependency must remain removed');
assert(gradle.includes('com.android.billingclient:billing:9.1.0'),'Play Billing 9.1.0 must be included');
assert(main.includes('billing-production.js?v=220'),'native shell must load the billing bridge');
const billing=fs.readFileSync('android/app/src/main/java/com/skyare/stackupacademy/BillingManager.java','utf8');
assert(billing.includes('PRODUCT_ID = "academy_access"'),'subscription product ID must be academy_access');
assert(billing.includes('BASE_MONTHLY = "monthly"'),'monthly base plan must be wired');
assert(billing.includes('BASE_SIX_MONTH = "six-month"'),'six-month base plan must be wired');
assert(billing.includes('BASE_ANNUAL = "annual"'),'annual base plan must be wired');
assert(billing.includes('acknowledgePurchase'),'subscription purchases must be acknowledged');
assert(billing.includes('queryPurchasesAsync'),'active subscriptions must be restored');

assert(main.includes('CACHE_SCHEMA = 220'),'native cache schema must match build 220');
assert(main.includes('index.html?android_build=220&cache_reset=1'),'recovery URL must use the remote entry for build 220');
assert(main.includes('loadLocalFallback'),'remote failure must fall back to bundled Academy assets');
assert(main.includes('SHELL_CREATE version=220'),'startup diagnostics must identify build 220');
assert(main.includes('migrated=1'),'cache cleanup reload must return to the bundled build 220 entry');
assert(main.includes('auth-production.js?v=220'),'native auth loader must be cache-busted for build 220');

assert(main.includes('webView = new WebView(this);'),'native WebView launcher must remain present');
assert(main.includes('onRenderProcessGone'),'renderer loss must be handled');
assert(main.includes('rendererRecoveryAttempted'),'renderer recovery must be bounded');
assert(main.includes('WEB_RENDERER_GONE'),'renderer failure must be observable');
assert(main.includes('showPermanentError()'),'startup failure must degrade to in-app error');

assert(index.includes('<meta name="stackup-release" content="2.1.7">'),'hosted web release must be 2.1.7');
assert(index.includes('const APP_VERSION="2.1.7";'),'visible app version must be 2.1.7');
assert(/const CACHE = "academy-v2\.1\.7-[^"]+";/.test(sw),'service worker cache must be versioned for Academy 2.1.7');
assert(sw.includes('fetch(request)'),'service worker must preserve remote-first delivery');
assert(sw.includes('caches.match(request, { ignoreSearch: true })'),'service worker cache must remain an offline fallback');

assert(workflow.includes('Smoke test APK on Android 14 emulator'),'CI must include Android 14 smoke test');
assert(workflow.includes('api-level: 34'),'CI must exercise API 34');
assert(workflow.includes('api-level: 36'),'CI must retain API 36 coverage');

console.log('Android shell contract OK: 2.1.7/220, remote-first shell with bundled fallback, API 34 + API 36.');
