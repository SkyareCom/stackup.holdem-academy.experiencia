const fs=require('fs');
const assert=require('node:assert/strict');
const index=fs.readFileSync('index.html','utf8');
const sw=fs.readFileSync('sw.js','utf8');

const marker='id="stackup-academy-layout-audit-20260930"';
assert(index.includes(marker),'final layout audit style is missing');
assert(index.lastIndexOf(marker)>index.lastIndexOf('PADRAO DE ESPACAMENTO'),'audit layer must come after legacy spacing rules');
assert(index.includes('--academy-ui-x:16px')&&index.includes('--academy-ui-y:12px')&&index.includes('--academy-ui-i:8px')&&index.includes('--academy-ui-pad:14px'),'spacing tokens must be 16/12/8/14');
assert(index.includes('grid-template-columns:repeat(5,minmax(0,1fr))!important'),'footer must remain one row with five columns');
assert(index.includes('id="stackup-academy-card-alignment-20261001"'),'2.1.6 alignment hotfix must be present');
assert(index.lastIndexOf('id="stackup-academy-card-alignment-20261001"')>index.lastIndexOf('id="stackup-academy-layout-audit-20260930"'),'alignment hotfix must override the prior layout audit');
assert(index.includes('id="stackup-academy-typography-lock-20261002"'),'final typography lock must be present');
assert(index.includes("font-family:'Saira Semi Condensed','Saira Condensed',sans-serif!important"),'Saira Semi Condensed must be the final Academy UI font');
assert(index.includes('font-size:12px!important'),'Academy UI text must be locked to 12px');
assert(index.includes('font-size:9px!important'),'footer label font must be 9px');
assert(!index.includes('Overlock Academy'),'Overlock must stay removed');
assert(!index.includes('<span class="ck">'),'language and interaction selectors must not render check circles');
assert(index.includes('data-l="${c}" aria-pressed="${c===lang?"true":"false"}"'),'language selection must use the full button');
assert(index.includes('data-pref="${k}" aria-pressed="${pr.mode===k?"true":"false"}"'),'interaction selection must use the full button');
assert(index.includes('#app #home .tabbar .tab')&&index.includes('align-items:center!important')&&index.includes('text-align:center!important'),'footer icons and labels must be centered');
assert(index.includes('#app #home .tile.tcard .hic')&&index.includes('flex:0 0 48px!important'),'menu card icons must use a fixed anchor');
assert(index.includes('grid-template-rows:30px 54px!important'),'menu card title/description geometry must use fixed aligned tracks');
assert(index.includes('-webkit-line-clamp:4!important'),'menu card descriptions must allow four aligned lines where needed without clipping');
assert(index.includes('function bindCardOrphans(txt)'),'menu card descriptions must bind short connector words to the next word');
assert(index.includes('bindCardOrphans(txt).split(/ +/)'),'orphan protection must preserve non-breaking spaces during card line balancing');
assert(index.includes('function practiceListH()')&&index.includes('class="tower scroll practice-four"'),'Practice root must render the four-card 2x2 layout');
assert(index.includes('#app .habit .qstats')&&index.includes('grid-template-columns:repeat(2,minmax(0,1fr))!important'),'habit summary cards must split the row evenly');
assert(index.includes('#app .habit .goalrow')&&index.includes('grid-template-columns:minmax(86px,.9fr) repeat(3,minmax(0,1fr))!important'),'weekly goal row must use the full width');
assert(index.includes('#app .sfilt .segb')&&index.includes('justify-content:center!important')&&index.includes('text-align:center!important'),'simulator filters must be centered');
assert(index.includes('const TABS=["home","fund","mod","prat","perfil"];'),'footer fifth route must be Profile');
assert(index.includes('perfil:"t_perfil"'),'footer fifth label must be Perfil/Profile');
assert(index.includes('#app .view .evneed')&&index.includes('text-align:left!important'),'plan description must follow left-aligned editorial contract');
assert(index.includes('overflow-wrap:anywhere!important'),'long copy must have an overflow escape hatch');
assert(index.includes('const APP_VERSION="2.1.7";'),'web app version must match release 2.1.7');
assert(index.includes('#app .tab span')&&index.includes('overflow:hidden!important'),'footer labels must not spill outside their cells');
assert(index.includes('#app .pfopts')&&index.includes('gap:var(--academy-ui-y)!important'),'plan cards must keep vertical separation');
assert(/const CACHE = "academy-v2\.1\.7-[^"]+";/.test(sw),'service-worker cache must be versioned for the current Academy release');
assert(sw.includes('fetch(request)'),'service worker must fetch remote content before using cache');
assert(sw.includes('caches.match(request, { ignoreSearch: true })'),'service worker cache must remain offline fallback only');
assert(sw.indexOf('fetch(request)') < sw.indexOf('caches.match(request, { ignoreSearch: true })'),'remote-first order must be preserved');
assert(sw.includes('"auth-production.js"')&&sw.includes('"billing-production.js"'),'runtime auth/billing scripts must be cached');

const removedLegacy=[
  'academy-loader.js','academy-visual-system.js','typography-standard.js',
  'engine.js','modalities-module.js','practice-module.js',
  'fonts/love-ya-like-a-sister.ttf','privacy-policy.html',
  'icon-192.png','icon-512.png'
];
for(const file of removedLegacy) assert(!fs.existsSync(file),`legacy frontend artifact must stay removed: ${file}`);

console.log('Layout/current frontend contract OK');
